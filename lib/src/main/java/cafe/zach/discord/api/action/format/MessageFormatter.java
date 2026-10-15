package cafe.zach.discord.api.action.format;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.discord.api.action.context.MinecraftContext;

public class MessageFormatter {

    public static byte[] fetchAvatar(MinecraftContext context) {
        return AvatarUtil.fetchPaddedAvatar(context.username);
    }

    // Minecraft -> Discord

    public static String formatChat(MinecraftContext context) {
        return String.format("**%s** » %s", context.username, context.content);
    }

    public static MessageEmbed embedChat(MinecraftContext context) {
        return new EmbedBuilder().setAuthor(context.username, null, "attachment://author.png")
            .setDescription(context.content)
            .setColor(EmbedColors.CHAT)
            .build();
    }

    public static String formatChatCommand(MinecraftContext context) {
        return String.format("**%s** ran: %s", context.username, context.content);
    }

    public static MessageEmbed embedChatCommand(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(String.format("%s ran a command", context.username), null, "attachment://author.png")
            .setDescription(context.content)
            .setColor(EmbedColors.COMMAND)
            .build();
    }

    public static String formatJoin(MinecraftContext context) {
        return String.format("**%s** joined", context.username);
    }

    public static MessageEmbed embedJoin(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(String.format("%s joined the server", context.username), null, "attachment://author.png")
            .setColor(EmbedColors.JOIN)
            .build();
    }

    public static String formatLeave(MinecraftContext context) {
        return String.format("**%s** left", context.username);
    }

    public static MessageEmbed embedLeave(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(String.format("%s left the server", context.username), null, "attachment://author.png")
            .setColor(EmbedColors.LEAVE)
            .build();
    }

    public static String formatServerStarting(MinecraftContext context) {
        return "\uD83D\uDD04  Server starting...";
    }

    public static MessageEmbed embedServerStarting(MinecraftContext context) {
        return new EmbedBuilder().setTitle(" \uD83D\uDD04  Server starting...")
            .setColor(EmbedColors.GREY)
            .build();
    }

    public static String formatServerStarted(MinecraftContext context) {
        return "✅  Server started!";
    }

    public static MessageEmbed embedServerStarted(MinecraftContext context) {
        return new EmbedBuilder().setTitle(" ✅  Server started!")
            .setColor(EmbedColors.GREEN)
            .build();
    }

    public static String formatServerStopping(MinecraftContext context) {
        return "\uD83D\uDD04  Server stopping...";
    }

    public static MessageEmbed embedServerStopping(MinecraftContext context) {
        return new EmbedBuilder().setTitle(" \uD83D\uDD04  Server stopping...")
            .setColor(EmbedColors.GREY)
            .build();
    }

    public static String formatServerStopped(MinecraftContext context) {
        return "\uD83D\uDEAB  Server stopped";
    }

    public static MessageEmbed embedServerStopped(MinecraftContext context) {
        return new EmbedBuilder().setTitle(" \uD83D\uDEAB  Server stopped")
            .setColor(EmbedColors.RED)
            .build();
    }

    public static String formatPlayerDeath(MinecraftContext context) {
        return String.format("☠️ %s", context.content);
    }

    public static MessageEmbed embedPlayerDeath(MinecraftContext context) {
        return new EmbedBuilder().setAuthor(context.content, null, "attachment://author.png")
            .setColor(EmbedColors.RED)
            .build();
    }

    public static String formatAchievement(MinecraftContext context) {
        return String.format("**%s** achieved **%s**", context.username, context.content);
    }

    public static MessageEmbed embedAchievement(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(
                String.format("%s achieved %s", context.username, context.content),
                null,
                "attachment://author.png")
            .setColor(EmbedColors.GOLD)
            .build();
    }

    // Discord -> Minecraft

    private static final Pattern EMOJI_LINK_PATTERN = Pattern.compile("\\[[^\\]]*\\]\\((https?://[^)\\s]+)\\)");
    private static final Pattern EMOJI_NAME_PARAM_PATTERN = Pattern.compile("[?&]name=([^&\\s)]+)");

    public static String replaceEmojiLinks(String content) {
        if (content == null || content.isEmpty()) return content;
        Matcher matcher = EMOJI_LINK_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url.contains("emojis") || url.contains("stickers")) {
                Matcher nameMatcher = EMOJI_NAME_PARAM_PATTERN.matcher(url);
                if (nameMatcher.find()) {
                    try {
                        String rawName = nameMatcher.group(1);
                        String decoded = URLDecoder.decode(rawName, StandardCharsets.UTF_8.name());
                        if (
                            !decoded.trim()
                                .isEmpty()
                        ) {
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(":" + decoded + ":"));
                            continue;
                        }
                    } catch (Exception ignored) {}
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String formatDiscordMessage(DiscordContext context) {
        return String.format("[Discord] %s: %s", context.username, replaceEmojiLinks(context.content));
    }

    public static String formatDiscordReady(DiscordContext context) {
        return String
            .format("[Discord] Ready on %s %s!", context.guildCount, context.guildCount == 1 ? "guild" : "guilds");
    }
}
