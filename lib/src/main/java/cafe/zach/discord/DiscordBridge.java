package cafe.zach.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.discord.api.exceptions.InvalidDiscordConfigurationException;

public class DiscordBridge {

    private static DiscordService service;

    // offer to dependent mod impls, not necessarily needed
    public static EmbedBuilder embedBuilder() {
        return new EmbedBuilder();
    }

    public static void createConnection() throws InvalidDiscordConfigurationException, InterruptedException {
        service = new DiscordService(ConfigHandler.getInstance());
    }

    public static void restart() throws InvalidDiscordConfigurationException, InterruptedException {
        if (service == null) {
            createConnection();
            return;
        }
        service.restart(ConfigHandler.getInstance());
    }

    public static void shutdown() {
        if (service != null) {
            service.awaitDestroy();
            service = null;
        }
    }

    public static void sendMessage(String channelId, String message) {
        if (service == null || !service.isRunning()) return;
        service.sendMessage(channelId, message);
    }

    public static void sendEmbed(String channelId, MessageEmbed embed) {
        if (service == null || !service.isRunning()) return;
        service.sendEmbed(channelId, embed);
    }

    public static void sendEmbedWithAvatar(String channelId, MessageEmbed embed, byte[] avatar) {
        if (service == null || !service.isRunning()) return;
        service.sendEmbedWithAvatar(channelId, embed, avatar);
    }

    public static void sendEmbedWithAvatarAsync(String channelId, MessageEmbed embed, String avatarIdentifier) {
        if (service == null || !service.isRunning()) return;
        service.sendEmbedWithAvatarAsync(channelId, embed, avatarIdentifier);
    }

    public static boolean isConnected() {
        return service != null && service.isRunning();
    }
}
