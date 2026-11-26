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

    private static final Pattern FORMATTING_PATTERN = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private static final int MAX_EMBED_AUTHOR = 256;
    private static final int MAX_EMBED_TITLE = 256;
    private static final int MAX_EMBED_DESCRIPTION = 4096;
    private static final int MAX_MESSAGE_CONTENT = 2000;

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, Math.max(0, maxLength));
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty() || text.indexOf('\u00a7') == -1) return text;
        return FORMATTING_PATTERN.matcher(text)
            .replaceAll("");
    }

    public static byte[] fetchAvatar(MinecraftContext context) {
        return AvatarUtil.fetchPaddedAvatar(stripFormatting(context.username));
    }

    // Minecraft -> Discord

    public static String formatChat(MinecraftContext context) {
        return formatChat(context, context.content);
    }

    public static String formatChat(MinecraftContext context, String content) {
        return truncate(
            String.format(
                "**%s** » %s",
                stripFormatting(context.username),
                EmojiRegistry.replaceEmojis(stripFormatting(content))),
            MAX_MESSAGE_CONTENT);
    }

    public static MessageEmbed embedChat(MinecraftContext context) {
        return embedChat(context, context.content);
    }

    public static MessageEmbed embedChat(MinecraftContext context, String content) {
        return new EmbedBuilder()
            .setAuthor(truncate(stripFormatting(context.username), MAX_EMBED_AUTHOR), null, "attachment://author.png")
            .setDescription(truncate(EmojiRegistry.replaceEmojis(stripFormatting(content)), MAX_EMBED_DESCRIPTION))
            .setColor(EmbedColors.CHAT)
            .build();
    }

    public static String formatJoin(MinecraftContext context) {
        return String.format("**%s** joined", stripFormatting(context.username));
    }

    public static MessageEmbed embedJoin(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(
                String.format("%s joined the server", stripFormatting(context.username)),
                null,
                "attachment://author.png")
            .setColor(EmbedColors.JOIN)
            .build();
    }

    public static String formatLeave(MinecraftContext context) {
        return String.format("**%s** left", stripFormatting(context.username));
    }

    public static MessageEmbed embedLeave(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(
                String.format("%s left the server", stripFormatting(context.username)),
                null,
                "attachment://author.png")
            .setColor(EmbedColors.LEAVE)
            .build();
    }

    public static String formatAfk(MinecraftContext context) {
        String name = stripFormatting(context.username);
        return context.isAfk ? String.format("**%s** is now AFK", name)
            : String.format("**%s** is no longer AFK", name);
    }

    public static MessageEmbed embedAfk(MinecraftContext context) {
        String name = stripFormatting(context.username);
        String status = context.isAfk ? String.format("%s is now AFK", name)
            : String.format("%s is no longer AFK", name);
        return new EmbedBuilder().setAuthor(status, null, "attachment://author.png")
            .setColor(EmbedColors.GREY)
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
        return truncate(String.format("☠️ %s", stripFormatting(context.content)), MAX_MESSAGE_CONTENT);
    }

    public static MessageEmbed embedPlayerDeath(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(truncate(stripFormatting(context.content), MAX_EMBED_AUTHOR), null, "attachment://author.png")
            .setColor(EmbedColors.RED)
            .build();
    }

    public static String formatAchievement(MinecraftContext context) {
        return truncate(
            String
                .format("**%s** achieved **%s**", stripFormatting(context.username), stripFormatting(context.content)),
            MAX_MESSAGE_CONTENT);
    }

    public static MessageEmbed embedAchievement(MinecraftContext context) {
        return new EmbedBuilder()
            .setAuthor(
                truncate(
                    String
                        .format("%s achieved %s", stripFormatting(context.username), stripFormatting(context.content)),
                    MAX_EMBED_AUTHOR),
                null,
                "attachment://author.png")
            .setColor(EmbedColors.GOLD)
            .build();
    }

    public static String formatAe2Craft(MinecraftContext context) {
        String itemInfo = stripFormatting(context.content);
        String name = stripFormatting(context.username);
        if (name != null && !name.isEmpty()) {
            return truncate(String.format("**%s** crafted **%s**", name, itemInfo), MAX_MESSAGE_CONTENT);
        }
        return truncate(String.format("Craft completed: **%s**", itemInfo), MAX_MESSAGE_CONTENT);
    }

    public static MessageEmbed embedAe2Craft(MinecraftContext context) {
        String itemInfo = stripFormatting(context.content);
        String name = stripFormatting(context.username);
        if (name != null && !name.isEmpty()) {
            return new EmbedBuilder()
                .setAuthor(
                    truncate(String.format("%s crafted %s", name, itemInfo), MAX_EMBED_AUTHOR),
                    null,
                    "attachment://author.png")
                .setColor(EmbedColors.GREEN)
                .build();
        }
        return new EmbedBuilder().setTitle(truncate(String.format("Craft completed: %s", itemInfo), MAX_EMBED_TITLE))
            .setColor(EmbedColors.GREEN)
            .build();
    }

    public static String formatPowerfail(String ownerName, String machineInfo, int count) {
        String info = stripFormatting(machineInfo);
        String name = stripFormatting(ownerName);
        String countSuffix = count > 1 ? String.format(" (x%d)", count) : "";
        if (name != null && !name.isEmpty()) {
            return truncate(String.format("\u26A1 **%s**: **%s**%s", name, info, countSuffix), MAX_MESSAGE_CONTENT);
        }
        return truncate(String.format("\u26A1 Powerfail: **%s**%s", info, countSuffix), MAX_MESSAGE_CONTENT);
    }

    public static MessageEmbed embedPowerfail(String ownerName, String machineInfo, int count) {
        String info = stripFormatting(machineInfo);
        String name = stripFormatting(ownerName);
        String countSuffix = count > 1 ? String.format(" (x%d)", count) : "";
        if (name != null && !name.isEmpty()) {
            return new EmbedBuilder()
                .setAuthor(
                    truncate(String.format("%s: %s%s", name, info, countSuffix), MAX_EMBED_AUTHOR),
                    null,
                    "attachment://author.png")
                .setColor(EmbedColors.ORANGE)
                .build();
        }
        return new EmbedBuilder()
            .setTitle(truncate(String.format("\u26A1 Powerfail: %s%s", info, countSuffix), MAX_EMBED_TITLE))
            .setColor(EmbedColors.ORANGE)
            .build();
    }

    // Discord -> Minecraft

    private static final Pattern EMOJI_LINK_PATTERN = Pattern.compile("\\[[^\\[\\]]*+\\]\\((https?://[^)\\s]++)\\)");
    private static final Pattern EMOJI_NAME_PARAM_PATTERN = Pattern.compile("[?&]name=([^&\\s)]++)");

    public static String replaceEmojiLinks(String content) {
        if (content == null || content.isEmpty()) return content;
        if (content.indexOf('[') == -1 || content.indexOf("](") == -1 || !content.contains("name=")) return content;

        Matcher matcher = EMOJI_LINK_PATTERN.matcher(content);
        StringBuffer sb = null;
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
                            if (sb == null) {
                                sb = new StringBuffer(content.length());
                            }
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(":" + decoded + ":"));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (sb == null) {
            return content;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String formatDiscordMessage(DiscordContext context) {
        String formattedContent = replaceEmojiLinks(context.content);
        if (
            context.replyTo != null && !context.replyTo.trim()
                .isEmpty()
        ) {
            return String.format("[Discord] %s replied to %s: %s", context.username, context.replyTo, formattedContent);
        }
        return String.format("[Discord] %s: %s", context.username, formattedContent);
    }

    public static String formatDiscordReady(DiscordContext context) {
        return String
            .format("[Discord] Ready on %s %s!", context.guildCount, context.guildCount == 1 ? "guild" : "guilds");
    }
}
