package cafe.zach.greglink.bridge.events.server;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraftforge.common.util.FakePlayer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.discord.api.config.ConfigHandler;

public class AfkHandler {

    private static final String SU_LOGGER_NAME = "Server Utilities";
    private static final String NOW_AFK_SUFFIX = " is now AFK";
    private static final String NO_LONGER_AFK_SUFFIX = " is no longer AFK";
    private static final String APPENDER_NAME = "GregLink-AfkAppender";

    private static AbstractAppender appender;

    public static synchronized void register() {
        if (appender != null) return;

        Logger logger = LogManager.getLogger(SU_LOGGER_NAME);
        if (!(logger instanceof org.apache.logging.log4j.core.Logger)) return;

        org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) logger;
        appender = new AbstractAppender(APPENDER_NAME, null, null, true) {

            @Override
            public void append(LogEvent event) {
                if (event == null || event.getLevel() != Level.INFO || event.getMessage() == null) return;

                ConfigHandler config = ConfigHandler.getInstance();
                if (config == null || !config.hasRelayAfk()) return;

                String msg = event.getMessage()
                    .getFormattedMessage();
                if (msg == null) return;

                boolean isAfk;
                String rawPlayerName;
                if (msg.endsWith(NOW_AFK_SUFFIX)) {
                    isAfk = true;
                    rawPlayerName = msg.substring(0, msg.length() - NOW_AFK_SUFFIX.length());
                } else if (msg.endsWith(NO_LONGER_AFK_SUFFIX)) {
                    isAfk = false;
                    rawPlayerName = msg.substring(0, msg.length() - NO_LONGER_AFK_SUFFIX.length());
                } else {
                    return;
                }

                handleAfkLog(rawPlayerName, isAfk);
            }
        };
        appender.start();
        coreLogger.addAppender(appender);
    }

    public static synchronized void unregister() {
        if (appender == null) return;
        try {
            Logger logger = LogManager.getLogger(SU_LOGGER_NAME);
            if (logger instanceof org.apache.logging.log4j.core.Logger) {
                ((org.apache.logging.log4j.core.Logger) logger).removeAppender(appender);
            }
            appender.stop();
        } catch (Throwable ignored) {} finally {
            appender = null;
        }
    }

    private static void handleAfkLog(String rawDisplayName, boolean isAfk) {
        String cleanTarget = MessageFormatter.stripFormatting(rawDisplayName)
            .trim();
        if (cleanTarget.isEmpty()) return;

        String username = cleanTarget;
        String uuid = null;

        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            ServerConfigurationManager configManager = server.getConfigurationManager();
            if (configManager != null && configManager.playerEntityList != null) {
                List<?> players = configManager.playerEntityList;
                for (Object obj : players) {
                    if (!(obj instanceof EntityPlayerMP)) continue;
                    EntityPlayerMP player = (EntityPlayerMP) obj;
                    if (player instanceof FakePlayer) continue;

                    String senderName = player.getCommandSenderName();
                    String displayName = MessageFormatter.stripFormatting(player.getDisplayName())
                        .trim();

                    if (cleanTarget.equals(displayName) || cleanTarget.equals(senderName)) {
                        username = senderName;
                        uuid = player.getUniqueID()
                            .toString();
                        break;
                    }
                }
            }
        }

        ActionRegistry.fireMinecraft(ActionRegistry.ON_MINECRAFT_AFK, MinecraftContext.forAfk(username, uuid, isAfk));
    }
}
