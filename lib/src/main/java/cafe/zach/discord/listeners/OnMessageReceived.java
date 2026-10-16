package cafe.zach.discord.listeners;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;

public class OnMessageReceived extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        if (
            event.getAuthor() == event.getJDA()
                .getSelfUser()
        ) return;

        String content = event.getMessage()
            .getContentDisplay();

        List<StickerItem> stickers = event.getMessage()
            .getStickers();
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

        List<Message.Attachment> attachments = event.getMessage()
            .getAttachments();
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

        DiscordContext context = DiscordContext.forMessage(
            event.getAuthor()
                .getName(),
            content,
            event.getChannel()
                .getId(),
            event.getGuild()
                .getId(),
            event.getAuthor()
                .isBot());

        ActionRegistry.fireDiscord(ActionRegistry.ON_DISCORD_MESSAGE, context);
    }
}
