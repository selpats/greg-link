package cafe.zach.discord.listeners;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.discord.api.action.format.EmbedColors;
import cafe.zach.discord.api.action.registry.ActionRegistry;

public class OnMessageReceived extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        User selfUser = event.getJDA()
            .getSelfUser();
        if (
            event.getAuthor()
                .equals(selfUser)
        ) return;

        Message message = event.getMessage();
        String content = message.getContentDisplay();

        List<StickerItem> stickers = message.getStickers();
        if (stickers != null && !stickers.isEmpty()) {
            StringBuilder stickerText = new StringBuilder();
            for (StickerItem sticker : stickers) {
                String name = sticker.getName();
                if (
                    name != null && !name.trim()
                        .isEmpty()
                ) {
                    if (stickerText.length() > 0) {
                        stickerText.append(" ");
                    }
                    stickerText.append(":")
                        .append(name.trim())
                        .append(":");
                }
            }
            if (stickerText.length() > 0) {
                if (
                    content == null || content.trim()
                        .isEmpty()
                ) {
                    content = stickerText.toString();
                } else {
                    content = content + " " + stickerText.toString();
                }
            }
        }

        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            String attachmentTag = attachments.size() > 1 ? "[attachments]" : "[attachment]";
            if (
                content == null || content.trim()
                    .isEmpty()
            ) {
                content = attachmentTag;
            } else {
                content = content + " " + attachmentTag;
            }
        }

        String replyTo = resolveReplyTarget(message.getReferencedMessage(), selfUser);

        DiscordContext context = DiscordContext.forMessage(
            event.getAuthor()
                .getName(),
            replyTo,
            content,
            event.getChannel()
                .getId(),
            event.getGuild()
                .getId(),
            event.getAuthor()
                .isBot());

        ActionRegistry.fireDiscord(ActionRegistry.ON_DISCORD_MESSAGE, context);
    }

    private static String resolveReplyTarget(Message referenced, User selfUser) {
        if (referenced == null) return null;

        User refAuthor = referenced.getAuthor();
        if (refAuthor == null) return null;

        if (!refAuthor.equals(selfUser)) {
            return refAuthor.getName();
        }

        // Plain-text relayed player chat: "**Username** » message"
        String raw = referenced.getContentRaw();
        if (raw != null && raw.startsWith("**")) {
            int separatorIdx = raw.indexOf("** » ");
            if (separatorIdx > 2) {
                String playerName = raw.substring(2, separatorIdx)
                    .trim();
                if (!playerName.isEmpty() && playerName.indexOf('\n') == -1) {
                    return playerName;
                }
            }
        }

        // Embed relayed player chat (uses EmbedColors.CHAT and sets Author to player username)
        List<MessageEmbed> embeds = referenced.getEmbeds();
        if (embeds != null && !embeds.isEmpty()) {
            MessageEmbed embed = embeds.get(0);
            if (embed.getColorRaw() == EmbedColors.CHAT && embed.getAuthor() != null) {
                String authorName = embed.getAuthor()
                    .getName();
                if (
                    authorName != null && !authorName.trim()
                        .isEmpty()
                ) {
                    return authorName.trim();
                }
            }
        }

        // All other bot messages (death, join/leave, achievement, server status, etc.)
        return "GregLink";
    }
}
