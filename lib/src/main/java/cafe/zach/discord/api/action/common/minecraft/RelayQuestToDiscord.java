package cafe.zach.discord.api.action.common.minecraft;

import java.util.List;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ChannelMinecraftConfig;
import cafe.zach.discord.api.config.ConfigHandler;

public class RelayQuestToDiscord {

    public static IMinecraftAction create() {
        return context -> {
            ConfigHandler config = ConfigHandler.getInstance();
            if (config == null || !config.relayBqQuests) return;

            String dim = (context.dimensionId != null && !context.dimensionId.isEmpty()) ? context.dimensionId
                : ChannelMinecraftConfig.WILDCARD;
            List<ChannelMapping> mappings = config.getChannelsForDimension(dim);

            for (ChannelMapping mapping : mappings) {
                if (mapping.discord.eventsUseEmbeds) {
                    DiscordBridge.sendEmbedWithAvatarAsync(
                        mapping.discordChannelId,
                        MessageFormatter.embedQuest(context),
                        context.username);
                } else {
                    DiscordBridge.sendMessage(mapping.discordChannelId, MessageFormatter.formatQuest(context));
                }
            }
        };
    }
}
