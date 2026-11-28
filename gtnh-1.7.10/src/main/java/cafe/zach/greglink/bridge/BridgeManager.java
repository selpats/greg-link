package cafe.zach.greglink.bridge;

import net.minecraftforge.common.MinecraftForge;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.discord.api.exceptions.InvalidDiscordConfigurationException;
import cafe.zach.greglink.GregLink;
import cafe.zach.greglink.bridge.events.server.*;
import cafe.zach.greglink.bridge.registry.DiscordActionRegistry;
import cafe.zach.greglink.bridge.registry.MinecraftActionRegistry;
import cafe.zach.greglink.config.GregLinkConfig;
import cafe.zach.greglink.links.AccountLinkManager;
import cpw.mods.fml.common.FMLCommonHandler;

public class BridgeManager {

    public static void registerEventHandlers() {
        // Chat itself
        MinecraftForge.EVENT_BUS.register(new ServerChatHandler());

        // Misc
        MinecraftForge.EVENT_BUS.register(new AchievementHandler());
        MinecraftForge.EVENT_BUS.register(new LivingDeathHandler());

        // Join/leave events
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLoggedInHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLoggedOutHandler());

        // ServerUtilities AFK logger listener
        AfkHandler.register();
    }

    public static void registerActions() {
        ActionRegistry.clear();
        MinecraftActionRegistry.register();
        DiscordActionRegistry.register();
    }

    public static void connect() {
        AfkHandler.register();
        Ae2WebIntegrationHandler.register();
        BetterQuestingQuestHandler.register();
        try {
            DiscordBridge.createConnection();
        } catch (InvalidDiscordConfigurationException | InterruptedException e) {
            GregLink.LOG.fatal("Could not connect to Discord!", e);
        }
    }

    public static void onServerStarted() {
        GtPowerfailHandler.register();
    }

    public static void disconnect() {
        AfkHandler.unregister();
        BetterQuestingQuestHandler.unregister();
        GtPowerfailHandler.clear();
        DiscordBridge.shutdown();
    }

    public static synchronized boolean reload() {
        ConfigHandler previous = ConfigHandler.getInstance();
        String previousToken = previous != null && previous.discordToken != null ? previous.discordToken : "";

        GregLinkConfig updated = new GregLinkConfig();
        if (!updated.tryLoad()) {
            return false;
        }
        updated.save();
        ConfigHandler.setInstance(updated);

        AccountLinkManager.load();
        AfkHandler.register();
        Ae2WebIntegrationHandler.register();
        BetterQuestingQuestHandler.register();
        GtPowerfailHandler.register();

        String newToken = updated.discordToken != null ? updated.discordToken : "";
        if (!newToken.equals(previousToken) || !DiscordBridge.isConnected()) {
            Thread reconnectThread = new Thread(() -> {
                try {
                    DiscordBridge.restart();
                } catch (InvalidDiscordConfigurationException | InterruptedException e) {
                    GregLink.LOG.fatal("Could not reconnect to Discord after config reload!", e);
                }
            }, "GregLink-Reconnect");
            reconnectThread.setDaemon(true);
            reconnectThread.start();
        }
        return true;
    }
}
