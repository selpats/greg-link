package cafe.zach.discord.api.action.format;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

public class EmojiRegistry {

    private static final Map<String, String> EMOJI_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> EMOJI_MAP_LOWER = new ConcurrentHashMap<>();
    private static final Pattern MINECRAFT_EMOJI_PATTERN = Pattern.compile("(?<!<a?):([A-Za-z0-9_]+):(?!\\d+>)");

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_INTERVAL_MINUTES = 15;

    private static ScheduledExecutorService scheduler;

    public static synchronized void loadEmojis(JDA jda) {
        if (jda == null) return;

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "GregLink-EmojiRetry");
                t.setDaemon(true);
                return t;
            });
        }

        List<Guild> guilds = jda.getGuilds();
        for (Guild guild : guilds) {
            fetchGuildEmojis(guild, 0);
        }
    }

    private static void fetchGuildEmojis(Guild guild, int attempt) {
        guild.retrieveEmojis()
            .queue(emojis -> {
                if (emojis == null || emojis.isEmpty()) {
                    handleFailure(guild, attempt, new IllegalStateException("Guild returned empty emoji list"));
                    return;
                }

                for (RichCustomEmoji emoji : emojis) {
                    String formatted = (emoji.isAnimated() ? "<a:" : "<:") + emoji.getName()
                        + ":"
                        + emoji.getId()
                        + ">";
                    EMOJI_MAP.put(emoji.getName(), formatted);
                    EMOJI_MAP_LOWER.put(
                        emoji.getName()
                            .toLowerCase(),
                        formatted);
                }
                System.out.println(
                    "[GregLink-Discord] Successfully loaded " + emojis.size()
                        + " custom emojis from "
                        + guild.getName()
                        + (attempt > 0 ? " (retry #" + attempt + ")" : ""));
            }, error -> { handleFailure(guild, attempt, error); });
    }

    private static void handleFailure(Guild guild, int attempt, Throwable error) {
        int nextAttempt = attempt + 1;
        if (nextAttempt <= MAX_RETRIES) {
            System.err.println(
                "[GregLink-Discord] Failed to load emojis from " + guild.getName()
                    + ": "
                    + (error != null ? error.getMessage() : "Unknown error")
                    + ". Scheduling retry "
                    + nextAttempt
                    + "/"
                    + MAX_RETRIES
                    + " in "
                    + RETRY_INTERVAL_MINUTES
                    + " minutes.");
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler
                    .schedule(() -> fetchGuildEmojis(guild, nextAttempt), RETRY_INTERVAL_MINUTES, TimeUnit.MINUTES);
            }
        } else {
            System.err.println(
                "[GregLink-Discord] Failed to load emojis from " + guild.getName()
                    + " after "
                    + MAX_RETRIES
                    + " retries: "
                    + (error != null ? error.getMessage() : "Unknown error"));
        }
    }

    public static String replaceEmojis(String text) {
        if (text == null || text.isEmpty()) return text;
        if (EMOJI_MAP.isEmpty()) return text;

        Matcher matcher = MINECRAFT_EMOJI_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = EMOJI_MAP.get(name);
            if (replacement == null) {
                replacement = EMOJI_MAP_LOWER.get(name.toLowerCase());
            }
            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public static Map<String, String> getEmojiMap() {
        return Collections.unmodifiableMap(EMOJI_MAP);
    }
}
