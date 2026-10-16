package cafe.zach.discord.api.action.common.minecraft;

import java.util.List;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ConfigHandler;

public class RelayAchievementToDiscord {

    public static IMinecraftAction create() {
        return context -> {
            List<ChannelMapping> mappings = ConfigHandler.getInstance()
                .getChannelsForDimension(context.dimensionId);

            for (ChannelMapping mapping : mappings) {
                if (mapping.discord.eventsUseEmbeds) {
                    DiscordBridge.sendEmbedWithAvatarAsync(
                        mapping.discordChannelId,
                        MessageFormatter.embedAchievement(context),
                        context.username);
                } else {
                    DiscordBridge.sendMessage(mapping.discordChannelId, MessageFormatter.formatAchievement(context));
                }
            }
        };
    }
}
