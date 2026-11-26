package cafe.zach.greglink.bridge.events.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.format.MessageFormatter;
import cafe.zach.discord.api.config.ChannelMapping;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.GregLink;
import cpw.mods.fml.common.Loader;

public final class GtPowerfailHandler {

    private static final String MOD_ID = "gregtech";
    private static final long EDIT_DEBOUNCE_MS = 1500L;
    private static final long PRUNE_INTERVAL_MS = 60_000L;

    private static final class ChannelTarget {

        final String channelId;
        final boolean useEmbed;
        volatile String messageId;

        ChannelTarget(String channelId, boolean useEmbed) {
            this.channelId = channelId;
            this.useEmbed = useEmbed;
        }
    }

    private static final class ActivePowerfail {

        final String ownerName;
        final String machineInfo;
        final Map<String, ChannelTarget> targets = new HashMap<String, ChannelTarget>();
        int count = 1;
        volatile long lastTimestamp;
        long lastEditSent;
        ScheduledFuture<?> pendingEdit;

        ActivePowerfail(String ownerName, String machineInfo, long now) {
            this.ownerName = ownerName;
            this.machineInfo = machineInfo;
            this.lastTimestamp = now;
        }
    }

    private static final ConcurrentHashMap<String, ActivePowerfail> ACTIVE_POWERFAILS = new ConcurrentHashMap<String, ActivePowerfail>();
    private static volatile long lastPruneTime = 0L;
    private static final ScheduledExecutorService EDIT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GregLink-PowerfailEdit");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean failed = false;

    private GtPowerfailHandler() {}

    public static synchronized void register() {
        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.enableGtPowerfailNotifications) {
            clear();
            return;
        }

        if (!Loader.isModLoaded(MOD_ID)) {
            GregLink.LOG.info("GregTech is not installed; skipping powerfail notifications.");
            return;
        }

        if (failed) return;

        try {
            Object installed = Class.forName("cafe.zach.greglink.bridge.events.server.GtPowerfailInterceptor")
                .getMethod("install")
                .invoke(null);
            if (Boolean.TRUE.equals(installed)) {
                GregLink.LOG.info("GregTech powerfail notifications via GregLink enabled.");
            }
        } catch (Throwable t) {
            failed = true;
            GregLink.LOG.warn("Could not initialize GregTech powerfail notifications via GregLink.", t);
        }
    }

    public static void clear() {
        for (ActivePowerfail entry : ACTIVE_POWERFAILS.values()) {
            synchronized (entry) {
                if (entry.pendingEdit != null) {
                    entry.pendingEdit.cancel(false);
                    entry.pendingEdit = null;
                }
            }
        }
        ACTIVE_POWERFAILS.clear();
    }

    static void onPowerfail(int dim, String dimName, int x, int y, int z, String machineName, String ownerName) {
        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.enableGtPowerfailNotifications) return;

        String cleanOwner = normalizeOwner(ownerName);
        String cleanMachine = (machineName == null || machineName.trim()
            .isEmpty()) ? "Machine" : machineName.trim();
        String dimLabel = (dimName != null && !dimName.trim()
            .isEmpty()) ? dimName.trim() : ("Dim " + dim);
        String machineInfo = String.format("%s powerfailed at %d, %d, %d (%s)", cleanMachine, x, y, z, dimLabel);

        String key = buildKey(dim, x, y, z);
        long now = System.currentTimeMillis();
        long windowMs = Math.max(0, config.gtPowerfailMergeWindowMinutes) * 60_000L;

        pruneExpired(now, windowMs);

        ActivePowerfail existing = ACTIVE_POWERFAILS.get(key);
        if (existing != null) {
            synchronized (existing) {
                if (ACTIVE_POWERFAILS.get(key) == existing && windowMs > 0
                    && (now - existing.lastTimestamp) <= windowMs) {
                    existing.count++;
                    existing.lastTimestamp = now;
                    scheduleOrEditLocked(existing, now);
                    return;
                }
                if (existing.pendingEdit != null) {
                    existing.pendingEdit.cancel(false);
                    existing.pendingEdit = null;
                }
            }
        }

        List<ChannelMapping> mappings = config.getChannelsForDimension(String.valueOf(dim));
        if (mappings.isEmpty()) return;

        ActivePowerfail fresh = new ActivePowerfail(cleanOwner, machineInfo, now);
        for (ChannelMapping mapping : mappings) {
            fresh.targets.put(
                mapping.discordChannelId,
                new ChannelTarget(mapping.discordChannelId, mapping.discord.eventsUseEmbeds));
        }
        ACTIVE_POWERFAILS.put(key, fresh);

        for (final ChannelTarget target : fresh.targets.values()) {
            DiscordBridge.sendPowerfail(
                target.channelId,
                cleanOwner,
                machineInfo,
                1,
                target.useEmbed,
                messageId -> onMessageSent(fresh, target, messageId));
        }
    }

    private static void onMessageSent(ActivePowerfail entry, ChannelTarget target, String messageId) {
        if (messageId == null || messageId.isEmpty()) return;
        synchronized (entry) {
            target.messageId = messageId;
            if (entry.count > 1) {
                editTarget(entry, target, entry.count);
            }
        }
    }

    private static void scheduleOrEditLocked(final ActivePowerfail entry, long now) {
        long elapsed = now - entry.lastEditSent;
        if (elapsed >= EDIT_DEBOUNCE_MS) {
            if (entry.pendingEdit != null) {
                entry.pendingEdit.cancel(false);
                entry.pendingEdit = null;
            }
            entry.lastEditSent = now;
            editAllTargetsLocked(entry);
        } else if (entry.pendingEdit == null || entry.pendingEdit.isDone()) {
            long delay = EDIT_DEBOUNCE_MS - elapsed;
            entry.pendingEdit = EDIT_SCHEDULER.schedule(() -> {
                synchronized (entry) {
                    entry.pendingEdit = null;
                    entry.lastEditSent = System.currentTimeMillis();
                    editAllTargetsLocked(entry);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    private static void editAllTargetsLocked(ActivePowerfail entry) {
        int currentCount = entry.count;
        for (ChannelTarget target : entry.targets.values()) {
            if (target.messageId != null) {
                editTarget(entry, target, currentCount);
            }
        }
    }

    private static void editTarget(ActivePowerfail entry, ChannelTarget target, int count) {
        DiscordBridge.editPowerfail(
            target.channelId,
            target.messageId,
            entry.ownerName,
            entry.machineInfo,
            count,
            target.useEmbed);
    }

    private static String normalizeOwner(String ownerName) {
        if (ownerName == null) return null;
        String trimmed = MessageFormatter.stripFormatting(ownerName)
            .trim();
        if (trimmed.isEmpty() || "Player".equalsIgnoreCase(trimmed) || "None".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static void pruneExpired(long now, long windowMs) {
        if (windowMs <= 0) {
            if (!ACTIVE_POWERFAILS.isEmpty()) {
                ACTIVE_POWERFAILS.clear();
            }
            return;
        }
        if (now - lastPruneTime < PRUNE_INTERVAL_MS) {
            return;
        }
        lastPruneTime = now;
        ACTIVE_POWERFAILS.entrySet()
            .removeIf(e -> (now - e.getValue().lastTimestamp) > windowMs);
    }

    private static String buildKey(int dim, int x, int y, int z) {
        return dim + ":" + x + ":" + y + ":" + z;
    }
}
