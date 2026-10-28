package cafe.zach.greglink.bridge.actions;

import java.util.List;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.IMinecraftAction;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.links.AccountLinkManager;
import cafe.zach.greglink.links.AccountLinkManager.MentionedMessage;

public class RelayLinkedChatToDiscord {

    public static IMinecraftAction create() {
        return context -> {
            AccountLinkManager.refreshMinecraftName(context.uuid, context.username);
            MentionedMessage message = AccountLinkManager.resolveMinecraftMentions(context.content);
            List<ChannelMapping> mappings = ConfigHandler.getInstance()
                .getChannelsForDimension(context.dimensionId);

            for (ChannelMapping mapping : mappings) {
                DiscordBridge.sendMinecraftChat(
                    mapping.discordChannelId,
                    context,
                    message.content,
                    mapping.discord.chatsUseEmbeds,
                    message.discordUserIds);
            }
        };
    }
}
