package cafe.zach.discord.api.action.common.minecraft;

import java.util.List;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ChannelMinecraftConfig;
import cafe.zach.discord.api.config.ConfigHandler;

public class RelayAe2CraftToDiscord {

    public static IMinecraftAction create() {
        return context -> {
            ConfigHandler config = ConfigHandler.getInstance();
            if (config == null || !config.filterAe2CraftingNotifications) return;

            List<ChannelMapping> mappings = config.getChannelsForDimension(ChannelMinecraftConfig.WILDCARD);

            for (ChannelMapping mapping : mappings) {
                if (mapping.discord.eventsUseEmbeds) {
                    if (context.username != null && !context.username.isEmpty()) {
                        DiscordBridge.sendEmbedWithAvatarAsync(
                            mapping.discordChannelId,
                            MessageFormatter.embedAe2Craft(context),
                            context.username);
                    } else {
                        DiscordBridge.sendEmbed(mapping.discordChannelId, MessageFormatter.embedAe2Craft(context));
                    }
                } else {
                    DiscordBridge.sendMessage(mapping.discordChannelId, MessageFormatter.formatAe2Craft(context));
                }
            }
        };
    }
}
