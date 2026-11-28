package cafe.zach.greglink.bridge.events.server;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.GregLink;
import cafe.zach.greglink.compat.BetterQuestingHelper;
import cpw.mods.fml.common.Loader;

public final class BetterQuestingQuestHandler {

    private static final String MOD_ID = "betterquesting";
    private static final long PARTY_DEDUP_MS = 5000L;
    private static final long PRUNE_INTERVAL_MS = 60_000L;

    private static final ConcurrentHashMap<UUID, Long> RECENT_QUESTS = new ConcurrentHashMap<UUID, Long>();
    private static volatile long lastPruneTime = 0L;

    private static Object listenerInstance;
    private static volatile boolean failed = false;

    private BetterQuestingQuestHandler() {}

    public static synchronized void register() {
        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.relayBqQuests) {
            unregister();
            return;
        }

        if (!Loader.isModLoaded(MOD_ID)) {
            GregLink.LOG.info("BetterQuesting is not installed; skipping quest completion notifications.");
            return;
        }

        if (failed || listenerInstance != null) return;

        try {
            Object instance = Class.forName("cafe.zach.greglink.bridge.events.server.BetterQuestingQuestListener")
                .getDeclaredConstructor()
                .newInstance();
            MinecraftForge.EVENT_BUS.register(instance);
            listenerInstance = instance;
            GregLink.LOG.info("BetterQuesting quest completion notifications via GregLink enabled.");
        } catch (Throwable t) {
            failed = true;
            GregLink.LOG.warn("Could not initialize BetterQuesting quest notifications via GregLink.", t);
        }
    }

    public static synchronized void unregister() {
        RECENT_QUESTS.clear();
        if (listenerInstance != null) {
            try {
                MinecraftForge.EVENT_BUS.unregister(listenerInstance);
            } catch (Throwable ignored) {}
            listenerInstance = null;
        }
    }

    static void onQuestsCompleted(UUID playerId, Set<UUID> questIds) {
        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.relayBqQuests) return;

        EntityPlayerMP player = findOnlinePlayer(playerId);
        if (player == null || player instanceof FakePlayer) return;

        String username = player.getCommandSenderName();
        String uuidStr = player.getUniqueID()
            .toString();
        String dimensionId = (player.worldObj != null && player.worldObj.provider != null)
            ? String.valueOf(player.worldObj.provider.dimensionId)
            : "*";

        long now = System.currentTimeMillis();
        pruneRecent(now);

        for (UUID questId : questIds) {
            if (questId == null) continue;
            if (BetterQuestingHelper.isSilentQuest(questId)) continue;

            // Deduplicate party-shared quest completions firing for multiple players simultaneously
            Long previous = RECENT_QUESTS.get(questId);
            if (previous != null && (now - previous.longValue()) < PARTY_DEDUP_MS) {
                continue;
            }
            RECENT_QUESTS.put(questId, Long.valueOf(now));

            String rawName = BetterQuestingHelper.getQuestName(questId);
            String questName = BetterQuestingHelper.cleanFormatting(rawName);
            if (questName.isEmpty()) {
                continue;
            }

            ActionRegistry.fireMinecraft(
                ActionRegistry.ON_MINECRAFT_QUEST,
                MinecraftContext.forQuest(username, uuidStr, questName, dimensionId));
        }
    }

    private static EntityPlayerMP findOnlinePlayer(UUID playerId) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;
        ServerConfigurationManager configManager = server.getConfigurationManager();
        if (configManager == null || configManager.playerEntityList == null) return null;

        List<?> players = configManager.playerEntityList;
        for (Object obj : players) {
            if (!(obj instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) obj;
            if (playerId.equals(player.getUniqueID())) {
                return player;
            }
        }
        return null;
    }

    private static void pruneRecent(long now) {
        if (now - lastPruneTime < PRUNE_INTERVAL_MS) return;
        lastPruneTime = now;
        RECENT_QUESTS.entrySet()
            .removeIf(
                e -> (now - e.getValue()
                    .longValue()) >= PARTY_DEDUP_MS);
    }
}
