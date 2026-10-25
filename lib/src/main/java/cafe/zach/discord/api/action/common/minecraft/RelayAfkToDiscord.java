package cafe.zach.discord.api.action.common.minecraft;

import java.util.List;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ChannelMinecraftConfig;
import cafe.zach.discord.api.config.ConfigHandler;

public class RelayAfkToDiscord {

    public static IMinecraftAction create() {
        return context -> {
            List<ChannelMapping> mappings = ConfigHandler.getInstance()
                .getChannelsForDimension(ChannelMinecraftConfig.WILDCARD);

            for (ChannelMapping mapping : mappings) {
                if (!mapping.filters.relayAfk) {
                    continue;
                }

                if (mapping.discord.eventsUseEmbeds) {
                    DiscordBridge.sendEmbedWithAvatarAsync(
                        mapping.discordChannelId,
                        MessageFormatter.embedAfk(context),
                        context.username);
                } else {
                    DiscordBridge.sendMessage(mapping.discordChannelId, MessageFormatter.formatAfk(context));
                }
            }
        };
    }
}
