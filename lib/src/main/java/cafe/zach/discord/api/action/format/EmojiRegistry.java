package cafe.zach.discord.api.action.format;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

public class EmojiRegistry {

    private static final Map<String, String> EMOJI_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> EMOJI_MAP_LOWER = new ConcurrentHashMap<>();
    private static final Pattern MINECRAFT_EMOJI_PATTERN = Pattern.compile("(?<!<a?):([A-Za-z0-9_]{2,32}+):(?!\\d+>)");

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_INTERVAL_MINUTES = 15;

    private static ScheduledExecutorService scheduler;
    private static volatile JDA cachedJda;

    public static synchronized void loadEmojis(JDA jda) {
        if (jda == null) return;
        cachedJda = jda;

        List<Guild> guilds = jda.getGuilds();
        for (Guild guild : guilds) {
            fetchGuildEmojis(guild, 0);
        }
    }

    private static synchronized ScheduledExecutorService getOrCreateScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "GregLink-EmojiRetry");
                t.setDaemon(true);
                return t;
            });
            exec.setRemoveOnCancelPolicy(true);
            scheduler = exec;
        }
        return scheduler;
    }

    private static void fetchGuildEmojis(Guild guild, int attempt) {
        if (guild == null || guild.getJDA() != cachedJda) return;

        guild.retrieveEmojis()
            .queue(emojis -> {
                if (guild.getJDA() != cachedJda) return;
                if (emojis == null) {
                    handleFailure(guild, attempt, new IllegalStateException("Guild returned null emoji list"));
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
                            .toLowerCase(Locale.ROOT),
                        formatted);
                }
                System.out.println(
                    "[GregLink-Discord] Successfully loaded " + emojis.size()
                        + " custom emojis from "
                        + guild.getName()
                        + (attempt > 0 ? " (retry #" + attempt + ")" : ""));
            }, error -> {
                if (guild.getJDA() != cachedJda) return;
                handleFailure(guild, attempt, error);
            });
    }

    private static synchronized void handleFailure(Guild guild, int attempt, Throwable error) {
        if (cachedJda == null || guild.getJDA() != cachedJda) return;
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
            getOrCreateScheduler()
                .schedule(() -> fetchGuildEmojis(guild, nextAttempt), RETRY_INTERVAL_MINUTES, TimeUnit.MINUTES);
        } else {
            System.err.println(
                "[GregLink-Discord] Failed to load emojis from " + guild.getName()
                    + " after "
                    + MAX_RETRIES
                    + " retries: "
                    + (error != null ? error.getMessage() : "Unknown error"));
        }
    }

    public static void refreshEmojis(Consumer<String> callback) {
        JDA jda = cachedJda;
        if (jda == null) {
            if (callback != null) callback.accept("Discord bot is not connected.");
            return;
        }

        List<Guild> guilds = jda.getGuilds();
        if (guilds.isEmpty()) {
            if (callback != null) callback.accept("Discord bot is not in any servers/guilds.");
            return;
        }

        AtomicInteger pending = new AtomicInteger(guilds.size());
        AtomicInteger succeededGuilds = new AtomicInteger(0);
        Map<String, String> combined = new ConcurrentHashMap<>();
        Map<String, String> combinedLower = new ConcurrentHashMap<>();

        for (Guild guild : guilds) {
            guild.retrieveEmojis()
                .queue(emojis -> {
                    if (emojis != null) {
                        succeededGuilds.incrementAndGet();
                        for (RichCustomEmoji emoji : emojis) {
                            String formatted = (emoji.isAnimated() ? "<a:" : "<:") + emoji.getName()
                                + ":"
                                + emoji.getId()
                                + ">";
                            combined.put(emoji.getName(), formatted);
                            combinedLower.put(
                                emoji.getName()
                                    .toLowerCase(Locale.ROOT),
                                formatted);
                        }
                    }
                    if (pending.decrementAndGet() == 0) {
                        applyRefreshedMaps(combined, combinedLower);

                        String msg = "Successfully refreshed custom emojis (" + EMOJI_MAP.size() + " total loaded)";
                        System.out.println("[GregLink-Discord] " + msg);
                        if (callback != null) callback.accept(msg);
                    }
                }, error -> {
                    System.err.println(
                        "[GregLink-Discord] Error refreshing emojis from " + guild.getName()
                            + ": "
                            + (error != null ? error.getMessage() : "Unknown error"));
                    if (pending.decrementAndGet() == 0) {
                        if (succeededGuilds.get() > 0) {
                            applyRefreshedMaps(combined, combinedLower);
                            String msg = "Finished refreshing emojis (" + EMOJI_MAP.size() + " loaded)";
                            if (callback != null) callback.accept(msg);
                        } else {
                            String msg = "Failed to refresh emojis from Discord: "
                                + (error != null ? error.getMessage() : "Unknown error");
                            if (callback != null) callback.accept(msg);
                        }
                    }
                });
        }
    }

    private static synchronized void applyRefreshedMaps(Map<String, String> combined,
        Map<String, String> combinedLower) {
        EMOJI_MAP.putAll(combined);
        Set<String> toRemove = new HashSet<>(EMOJI_MAP.keySet());
        toRemove.removeAll(combined.keySet());
        for (String key : toRemove) {
            EMOJI_MAP.remove(key);
        }

        EMOJI_MAP_LOWER.putAll(combinedLower);
        Set<String> toRemoveLower = new HashSet<>(EMOJI_MAP_LOWER.keySet());
        toRemoveLower.removeAll(combinedLower.keySet());
        for (String key : toRemoveLower) {
            EMOJI_MAP_LOWER.remove(key);
        }
    }

    public static String replaceEmojis(String text) {
        if (text == null || text.isEmpty()) return text;
        if (EMOJI_MAP.isEmpty()) return text;

        int firstColon = text.indexOf(':');
        if (firstColon == -1 || text.indexOf(':', firstColon + 1) == -1) {
            return text;
        }

        Matcher matcher = MINECRAFT_EMOJI_PATTERN.matcher(text);
        StringBuffer sb = null;
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = EMOJI_MAP.get(name);
            if (replacement == null) {
                replacement = EMOJI_MAP_LOWER.get(name.toLowerCase(Locale.ROOT));
            }
            if (replacement != null) {
                if (sb == null) {
                    sb = new StringBuffer(text.length() + 32);
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        if (sb == null) {
            return text;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static synchronized void shutdown() {
        cachedJda = null;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        EMOJI_MAP.clear();
        EMOJI_MAP_LOWER.clear();
    }

    public static Map<String, String> getEmojiMap() {
        return Collections.unmodifiableMap(EMOJI_MAP);
    }
}
