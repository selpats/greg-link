package cafe.zach.greglink.bridge.events.server;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.StatisticsFile;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.AchievementEvent;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class AchievementHandler {

    private static Field statsField;
    private static volatile boolean statsFieldResolved = false;

    private static StatisticsFile getStatsFile(EntityPlayerMP player) {
        try {
            return player.func_147099_x();
        } catch (Throwable ignored) {
            try {
                if (!statsFieldResolved) {
                    Field f = EntityPlayerMP.class.getDeclaredField("field_147103_bO");
                    f.setAccessible(true);
                    statsField = f;
                    statsFieldResolved = true;
                }
                if (statsField != null) {
                    return (StatisticsFile) statsField.get(player);
                }
            } catch (Throwable ignored2) {
                statsFieldResolved = true;
            }
            return null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAchievement(AchievementEvent event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP)) return;
        if (event.entityPlayer instanceof FakePlayer) return;

        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        if (player.playerNetServerHandler == null || player.worldObj == null || player.worldObj.provider == null) {
            return;
        }

        Achievement achievement = event.achievement;
        if (achievement == null) return;

        StatisticsFile stats = getStatsFile(player);
        if (stats == null) return;

        // Fast O(1) check first: skip if already unlocked
        if (stats.hasAchievementUnlocked(achievement)) return;

        // Only fire if the player can actually unlock this achievement
        if (!stats.canUnlockAchievement(achievement)) return;

        String username = player.getCommandSenderName();
        String uuid = player.getUniqueID()
            .toString();
        String achievementName = achievement.func_150951_e()
            .getUnformattedText();
        String worldName = player.worldObj.getWorldInfo()
            .getWorldName();
        String dimensionId = String.valueOf(player.worldObj.provider.dimensionId);

        ActionRegistry.fireMinecraft(
            ActionRegistry.ON_MINECRAFT_ACHIEVEMENT,
            MinecraftContext.forAchievement(username, uuid, achievementName, worldName, dimensionId));
    }
}
