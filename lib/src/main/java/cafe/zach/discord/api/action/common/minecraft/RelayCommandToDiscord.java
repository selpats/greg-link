package cafe.zach.discord.api.action.common.minecraft;

import java.util.List;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ChannelMinecraftConfig;
import cafe.zach.discord.api.config.ConfigHandler;

public class RelayCommandToDiscord {

    public static IMinecraftAction create() {
        return context -> {
            List<ChannelMapping> mappings = ConfigHandler.getInstance()
                .getChannelsForDimension(ChannelMinecraftConfig.WILDCARD);

            for (ChannelMapping mapping : mappings) {
                if (!mapping.filters.relayCommands) {
                    continue;
                }

                if (mapping.discord.chatsUseEmbeds) {
                    DiscordBridge.sendEmbedWithAvatarAsync(
                        mapping.discordChannelId,
                        MessageFormatter.embedChatCommand(context),
                        context.username);
                } else {
                    DiscordBridge.sendMessage(mapping.discordChannelId, MessageFormatter.formatChatCommand(context));
                }
            }
        };
    }
}
