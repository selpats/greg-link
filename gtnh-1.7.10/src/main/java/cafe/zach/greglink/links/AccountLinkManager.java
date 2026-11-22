package cafe.zach.greglink.links;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.context.DiscordContext;
import cafe.zach.greglink.GregLink;
import cafe.zach.greglink.config.GregLinkConfig;
import cpw.mods.fml.common.Loader;

public final class AccountLinkManager {

    private static final int CODE_LENGTH = 10;
    private static final long CODE_TTL_MS = 10 * 60 * 1000L;
    private static final long CODE_COOLDOWN_MS = 15 * 1000L;
    private static final int MAX_PENDING_CODES = 2000;
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-HJ-NP-Z2-9]{10}$");
    private static final Pattern RAW_DISCORD_MENTION = Pattern
        .compile("<@!?\\d{17,20}>|<@&\\d{17,20}>|<#\\d{17,20}>|@everyone\\b|@here\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_MENTION = Pattern
        .compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_](?:[A-Za-z0-9_.-]{0,30}[A-Za-z0-9_])?)(?![A-Za-z0-9_.-])");
    private static final String AMBIGUOUS = "<ambiguous>";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final JsonParser PARSER = new JsonParser();
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final Map<UUID, LinkedAccount> LINKS = new LinkedHashMap<>();
    private static final Map<String, PendingLink> PENDING = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_CODE_REQUEST = new LinkedHashMap<>();

    private AccountLinkManager() {}

    public static synchronized void load() {
        LINKS.clear();
        File file = getLinksFile();
        if (!file.isFile()) return;

        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonElement rootElement = PARSER.parse(reader);
            if (!rootElement.isJsonObject()) throw new IOException("Link database root must be a JSON object");

            JsonArray entries = rootElement.getAsJsonObject()
                .getAsJsonArray("links");
            if (entries == null) return;

            for (JsonElement element : entries) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                try {
                    UUID minecraftUuid = UUID.fromString(
                        entry.get("minecraftUuid")
                            .getAsString());
                    String minecraftName = entry.get("minecraftName")
                        .getAsString();
                    String discordId = entry.get("discordId")
                        .getAsString();
                    String discordName = entry.get("discordName")
                        .getAsString();
                    if (minecraftName.isEmpty() || discordName.isEmpty() || !discordId.matches("\\d{17,20}")) continue;

                    removeConflictingLinks(minecraftUuid, discordId);
                    LINKS.put(minecraftUuid, new LinkedAccount(minecraftUuid, minecraftName, discordId, discordName));
                } catch (RuntimeException e) {
                    GregLink.LOG.warn("Skipping invalid account link entry in " + file.getAbsolutePath(), e);
                }
            }
        } catch (Exception e) {
            LINKS.clear();
            GregLink.LOG.error("Failed to load GregLink account links from " + file.getAbsolutePath(), e);
        }
    }

    public static synchronized String createCode(UUID minecraftUuid, String minecraftName) {
        long now = System.currentTimeMillis();
        expireOldCodes(now);
        Long lastRequest = LAST_CODE_REQUEST.get(minecraftUuid);
        if (lastRequest != null && now - lastRequest < CODE_COOLDOWN_MS) return null;
        if (PENDING.size() >= MAX_PENDING_CODES) return null;

        Iterator<Map.Entry<String, PendingLink>> iterator = PENDING.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            if (iterator.next()
                .getValue().minecraftUuid.equals(minecraftUuid)) {
                iterator.remove();
            }
        }

        String code;
        do {
            StringBuilder value = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) value.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            code = value.toString();
        } while (PENDING.containsKey(code));

        PENDING.put(code, new PendingLink(minecraftUuid, minecraftName, now + CODE_TTL_MS));
        LAST_CODE_REQUEST.put(minecraftUuid, now);
        LAST_CODE_REQUEST.entrySet()
            .removeIf(entry -> now - entry.getValue() > CODE_TTL_MS);
        return code;
    }

    public static synchronized boolean unlink(UUID minecraftUuid) {
        LinkedAccount removed = LINKS.remove(minecraftUuid);
        if (removed == null) return false;
        if (saveLinks()) return true;
        LINKS.put(minecraftUuid, removed);
        return false;
    }

    public static synchronized boolean isLinked(UUID minecraftUuid) {
        return LINKS.containsKey(minecraftUuid);
    }

    public static synchronized void refreshMinecraftName(String minecraftUuid, String minecraftName) {
        if (minecraftUuid == null || minecraftName == null || minecraftName.isEmpty()) return;
        try {
            UUID uuid = UUID.fromString(minecraftUuid);
            LinkedAccount previous = LINKS.get(uuid);
            if (previous == null || previous.minecraftName.equals(minecraftName)) return;

            LINKS.put(uuid, new LinkedAccount(uuid, minecraftName, previous.discordId, previous.discordName));
            if (!saveLinks()) LINKS.put(uuid, previous);
        } catch (IllegalArgumentException ignored) {}
    }

    public static synchronized void refreshDiscordName(DiscordContext context) {
        if (context == null || context.isBot || context.userId == null || context.username == null) return;

        for (LinkedAccount previous : LINKS.values()) {
            if (!previous.discordId.equals(context.userId) || previous.discordName.equals(context.username)) continue;

            LINKS.put(
                previous.minecraftUuid,
                new LinkedAccount(
                    previous.minecraftUuid,
                    previous.minecraftName,
                    previous.discordId,
                    context.username));
            if (!saveLinks()) LINKS.put(previous.minecraftUuid, previous);
            return;
        }
    }

    public static void handleDirectMessage(DiscordContext context) {
        if (context == null || !context.directMessage
            || context.isBot
            || context.content == null
            || context.userId == null
            || !context.userId.matches("\\d{17,20}")
            || context.username == null
            || context.username.isEmpty()) return;
        String code = context.content.trim()
            .toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code)
            .matches()) return;

        PendingLink pending;
        synchronized (AccountLinkManager.class) {
            pending = PENDING.remove(code);
        }
        if (pending == null) {
            DiscordBridge.sendDirectMessage(context.userId, "That link code is invalid, expired, or already used.");
            return;
        }
        if (pending.expiresAt < System.currentTimeMillis()) {
            DiscordBridge.sendDirectMessage(context.userId, "That link code has expired. Run /greglink link again.");
            return;
        }

        boolean saved;
        synchronized (AccountLinkManager.class) {
            saved = addLink(pending.minecraftUuid, pending.minecraftName, context.userId, context.username);
        }
        if (saved) {
            String message = "Linked Minecraft account " + pending.minecraftName + " to this Discord account.";
            if (!isMentionsEnabled()) {
                message += " (Note: Discord mentions from Minecraft chat are currently disabled in the server config.)";
            }
            DiscordBridge.sendDirectMessage(context.userId, message);
        } else {
            DiscordBridge.sendDirectMessage(
                context.userId,
                "The link could not be saved. Please ask the server administrator to check the GregLink log.");
        }
    }

    public static boolean isMentionsEnabled() {
        GregLinkConfig config = GregLinkConfig.get();
        return config == null || config.enableMentions;
    }

    public static MentionedMessage resolveMinecraftMentions(String content) {
        if (content == null || content.isEmpty()) return new MentionedMessage(content, Collections.emptyList());

        String safeContent = RAW_DISCORD_MENTION.matcher(content)
            .replaceAll(" ");
        if (!isMentionsEnabled()) {
            return new MentionedMessage(safeContent, Collections.emptyList());
        }

        Matcher matcher = NAMED_MENTION.matcher(safeContent);
        StringBuffer output = new StringBuffer(safeContent.length());
        Set<String> mentionIds = new LinkedHashSet<>();

        while (matcher.find()) {
            String discordId = resolveLinkedDiscordId(matcher.group(1));
            if (discordId == null) continue;
            mentionIds.add(discordId);
            matcher.appendReplacement(output, Matcher.quoteReplacement("<@" + discordId + ">"));
        }
        matcher.appendTail(output);
        return new MentionedMessage(output.toString(), new ArrayList<>(mentionIds));
    }

    private static synchronized String resolveLinkedDiscordId(String name) {
        GregLinkConfig config = GregLinkConfig.get();
        GregLinkConfig.MentionPriority priority = config != null ? config.mentionPriority
            : GregLinkConfig.MentionPriority.DISCORD;

        String result = findByName(name, priority == GregLinkConfig.MentionPriority.DISCORD);
        if (result != null) return AMBIGUOUS.equals(result) ? null : result;
        result = findByName(name, priority != GregLinkConfig.MentionPriority.DISCORD);
        return result == null || AMBIGUOUS.equals(result) ? null : result;
    }

    private static String findByName(String name, boolean discordName) {
        String result = null;
        for (LinkedAccount link : LINKS.values()) {
            String candidate = discordName ? link.discordName : link.minecraftName;
            if (!candidate.equalsIgnoreCase(name)) continue;
            if (result != null && !result.equals(link.discordId)) return AMBIGUOUS;
            result = link.discordId;
        }
        return result;
    }

    private static synchronized boolean addLink(UUID minecraftUuid, String minecraftName, String discordId,
        String discordName) {
        Map<UUID, LinkedAccount> previous = new LinkedHashMap<>(LINKS);
        removeConflictingLinks(minecraftUuid, discordId);
        LINKS.put(minecraftUuid, new LinkedAccount(minecraftUuid, minecraftName, discordId, discordName));
        if (saveLinks()) return true;
        LINKS.clear();
        LINKS.putAll(previous);
        return false;
    }

    private static void removeConflictingLinks(UUID minecraftUuid, String discordId) {
        Iterator<Map.Entry<UUID, LinkedAccount>> iterator = LINKS.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            LinkedAccount link = iterator.next()
                .getValue();
            if (link.minecraftUuid.equals(minecraftUuid) || link.discordId.equals(discordId)) iterator.remove();
        }
    }

    private static boolean saveLinks() {
        File file = getLinksFile();
        Path tempFile = null;
        try {
            JsonObject root = new JsonObject();
            JsonArray entries = new JsonArray();
            for (LinkedAccount link : LINKS.values()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("minecraftUuid", link.minecraftUuid.toString());
                entry.addProperty("minecraftName", link.minecraftName);
                entry.addProperty("discordId", link.discordId);
                entry.addProperty("discordName", link.discordName);
                entries.add(entry);
            }
            root.add("links", entries);

            Path target = file.toPath()
                .toAbsolutePath();
            Path parent = target.getParent();
            Files.createDirectories(parent);
            tempFile = Files.createTempFile(parent, file.getName(), ".tmp");
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(tempFile), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(root));
            }
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile = null;
            return true;
        } catch (Exception e) {
            GregLink.LOG.error("Failed to save GregLink account links to " + file.getAbsolutePath(), e);
            return false;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    GregLink.LOG.warn("Failed to remove temporary account link file " + tempFile, e);
                }
            }
        }
    }

    private static void expireOldCodes(long now) {
        PENDING.entrySet()
            .removeIf(entry -> entry.getValue().expiresAt < now);
    }

    private static File getLinksFile() {
        return new File(
            Loader.instance()
                .getConfigDir(),
            "greglink" + File.separatorChar + "links.json");
    }

    private static final class LinkedAccount {

        final UUID minecraftUuid;
        final String minecraftName;
        final String discordId;
        final String discordName;

        LinkedAccount(UUID minecraftUuid, String minecraftName, String discordId, String discordName) {
            this.minecraftUuid = minecraftUuid;
            this.minecraftName = minecraftName;
            this.discordId = discordId;
            this.discordName = discordName;
        }
    }

    private static final class PendingLink {

        final UUID minecraftUuid;
        final String minecraftName;
        final long expiresAt;

        PendingLink(UUID minecraftUuid, String minecraftName, long expiresAt) {
            this.minecraftUuid = minecraftUuid;
            this.minecraftName = minecraftName;
            this.expiresAt = expiresAt;
        }
    }

    public static final class MentionedMessage {

        public final String content;
        public final List<String> discordUserIds;

        MentionedMessage(String content, Collection<String> discordUserIds) {
            this.content = content;
            this.discordUserIds = Collections.unmodifiableList(new ArrayList<>(discordUserIds));
        }
    }
}
