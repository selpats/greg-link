package cafe.zach.greglink.bridge.events.server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.GregLink;
import cpw.mods.fml.common.Loader;

public class Ae2WebIntegrationHandler {

    private static final String MOD_ID = "ae2webintegration";

    private static final class TrackedCraft {

        final long timeStarted;
        volatile String itemName;
        volatile long quantity;
        volatile boolean isPlayerRequested;
        final Set<String> playerRequesters = Collections.synchronizedSet(new LinkedHashSet<String>());

        TrackedCraft(long timeStarted, String itemName, long quantity) {
            this.timeStarted = timeStarted;
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }

    private static final ConcurrentMap<Object, TrackedCraft> ACTIVE_CRAFTS = new MapMaker().weakKeys()
        .makeMap();

    private static volatile boolean initialized = false;
    private static volatile boolean failed = false;

    private static Method cpuGetFinalOutputMethod;
    private static Method cpuGetNameMethod;
    private static Method gridGetCraftingGridMethod;
    private static Method craftingGridGetCpusMethod;

    private static Method snapshotCaptureMethod;
    private static Method snapshotDescriptionMethod;
    private static Method snapshotQuantityMethod;
    private static Method descriptorDisplayNameMethod;

    private static Method resolveRequesterMethod;
    private static Method requesterSourceMethod;
    private static Method requesterPlayerMethod;
    private static Field playerIdentityNameField;

    private static Method formatDurationMethod;

    private static volatile Method getPlayersFollowingMethod;
    private static volatile boolean getPlayersFollowingResolved = false;

    public static synchronized void register() {
        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.filterAe2CraftingNotifications) {
            ACTIVE_CRAFTS.clear();
            return;
        }

        if (!Loader.isModLoaded(MOD_ID)) {
            GregLink.LOG.info("AE2 Web Integration is not installed; skipping AE2 crafting notifications.");
            return;
        }

        if (failed) return;

        try {
            if (!initialized) {
                Class<?> cpuInterface = Class
                    .forName("pl.kuba6000.ae2webintegration.core.interfaces.ICraftingCPUCluster");
                cpuGetFinalOutputMethod = cpuInterface.getMethod("web$getFinalOutput");
                cpuGetNameMethod = cpuInterface.getMethod("web$getName");

                Class<?> gridInterface = Class.forName("pl.kuba6000.ae2webintegration.core.interfaces.IAEGrid");
                gridGetCraftingGridMethod = gridInterface.getMethod("web$getCraftingGrid");

                Class<?> craftingGridInterface = Class
                    .forName("pl.kuba6000.ae2webintegration.core.interfaces.service.IAECraftingGrid");
                craftingGridGetCpusMethod = craftingGridInterface.getMethod("web$getCPUs");

                Class<?> genericStackInterface = Class
                    .forName("pl.kuba6000.ae2webintegration.core.interfaces.IAEGenericStack");
                Class<?> snapshotClass = Class.forName("pl.kuba6000.ae2webintegration.core.tracking.ResourceSnapshot");
                snapshotCaptureMethod = snapshotClass.getMethod("capture", genericStackInterface);
                snapshotDescriptionMethod = snapshotClass.getMethod("description");
                snapshotQuantityMethod = snapshotClass.getMethod("quantity");

                Class<?> descriptorClass = Class
                    .forName("pl.kuba6000.ae2webintegration.core.tracking.ResourceDescriptor");
                descriptorDisplayNameMethod = descriptorClass.getMethod("displayName");

                Class<?> playerIdentityClass = Class.forName("pl.kuba6000.ae2webintegration.core.api.PlayerIdentity");
                playerIdentityNameField = playerIdentityClass.getField("name");

                Class<?> requesterContextClass = Class
                    .forName("pl.kuba6000.ae2webintegration.core.tracking.CraftingRequesterContext");
                resolveRequesterMethod = requesterContextClass.getMethod("resolve", playerIdentityClass);

                Class<?> requesterClass = Class.forName("pl.kuba6000.ae2webintegration.core.api.CraftingRequester");
                requesterSourceMethod = requesterClass.getMethod("source");
                requesterPlayerMethod = requesterClass.getMethod("player");

                Class<?> notificationManagerClass = Class
                    .forName("pl.kuba6000.ae2webintegration.core.notification.NotificationManager");
                formatDurationMethod = notificationManagerClass.getMethod("formatDuration", long.class);

                initialized = true;
            }

            Class.forName("cafe.zach.greglink.bridge.events.server.Ae2CallbacksInterceptor")
                .getMethod("install")
                .invoke(null);
            GregLink.LOG.info("AE2 crafting notifications via GregLink enabled.");
        } catch (Throwable t) {
            failed = true;
            GregLink.LOG.warn("Could not initialize AE2 crafting notifications via GregLink.", t);
        }
    }

    static void onJobStarted(Object cpuCluster, boolean isMerging, boolean isMachineRequest, Object nativeRequester) {
        if (!initialized || failed || cpuCluster == null) return;

        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.filterAe2CraftingNotifications) return;

        try {
            Object rawOutput = cpuGetFinalOutputMethod.invoke(cpuCluster);
            Object snapshot = snapshotCaptureMethod.invoke(null, rawOutput);
            if (snapshot == null) {
                ACTIVE_CRAFTS.remove(cpuCluster);
                return;
            }

            Object descriptor = snapshotDescriptionMethod.invoke(snapshot);
            String itemName = (String) descriptorDisplayNameMethod.invoke(descriptor);
            long quantity = ((Long) snapshotQuantityMethod.invoke(snapshot)).longValue();

            TrackedCraft craft = isMerging ? ACTIVE_CRAFTS.get(cpuCluster) : null;
            if (craft == null) {
                craft = new TrackedCraft(System.currentTimeMillis(), itemName, quantity);
                ACTIVE_CRAFTS.put(cpuCluster, craft);
            } else {
                craft.itemName = itemName;
                craft.quantity = quantity;
            }

            Object requester = resolveRequesterMethod.invoke(null, nativeRequester);
            if (requester != null) {
                Object source = requesterSourceMethod.invoke(requester);
                String sourceName = source != null ? source.toString() : "";
                if ("WEB".equals(sourceName)) {
                    craft.isPlayerRequested = true;
                } else if (!isMachineRequest && "PLAYER".equals(sourceName)) {
                    craft.isPlayerRequested = true;
                    Object playerIdentity = requesterPlayerMethod.invoke(requester);
                    if (playerIdentity != null) {
                        Object rawName = playerIdentityNameField.get(playerIdentity);
                        if (rawName instanceof String) {
                            String trimmed = ((String) rawName).trim();
                            if (!trimmed.isEmpty()) {
                                craft.playerRequesters.add(trimmed);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    static void onBeforeJobCancelled(Object cpu) {
        if (cpu != null) {
            ACTIVE_CRAFTS.remove(cpu);
        }
    }

    static void onBeforeJobCompleted(Object grid, Object cpu) {
        if (!initialized || failed || cpu == null) return;

        TrackedCraft craft = ACTIVE_CRAFTS.remove(cpu);
        if (craft == null) return;

        ConfigHandler config = ConfigHandler.getInstance();
        if (config == null || !config.filterAe2CraftingNotifications) return;

        try {
            long durationMillis = Math.max(0L, System.currentTimeMillis() - craft.timeStarted);
            List<String> followers = getFollowers(cpu);
            List<String> playerRequesters = new ArrayList<String>(craft.playerRequesters);

            if (!shouldSendNotification(
                followers,
                craft.isPlayerRequested,
                playerRequesters,
                durationMillis,
                config.ae2AlwaysNotifyAfterMinutes)) {
                return;
            }

            String avatarPlayer = pickTargetPlayer(followers, playerRequesters);
            String durationStr = (String) formatDurationMethod.invoke(null, Long.valueOf(durationMillis));
            String cpuName = getCpuName(grid, cpu);

            String content = String.format("%dx %s (%s, %s)", craft.quantity, craft.itemName, durationStr, cpuName);
            ActionRegistry.fireMinecraft(
                ActionRegistry.ON_MINECRAFT_AE2_CRAFT,
                MinecraftContext.forAe2Craft(avatarPlayer, content));
        } catch (Throwable ignored) {}
    }

    private static String pickTargetPlayer(List<String> followers, List<String> playerRequesters) {
        if (followers != null && !followers.isEmpty()) {
            for (String follower : followers) {
                if (follower != null && !follower.trim()
                    .isEmpty() && AfkHandler.isPlayerOfflineOrAfk(follower)) {
                    return follower.trim();
                }
            }
            return followers.get(0)
                .trim();
        }
        if (playerRequesters != null && !playerRequesters.isEmpty()) {
            for (String requester : playerRequesters) {
                if (requester != null && !requester.trim()
                    .isEmpty() && AfkHandler.isPlayerOfflineOrAfk(requester)) {
                    return requester.trim();
                }
            }
            return playerRequesters.get(0)
                .trim();
        }
        return null;
    }

    private static String getCpuName(Object grid, Object cpu) {
        try {
            if (grid != null) {
                Object craftingGrid = gridGetCraftingGridMethod.invoke(grid);
                if (craftingGrid != null) {
                    craftingGridGetCpusMethod.invoke(craftingGrid);
                }
            }
            Object name = cpuGetNameMethod.invoke(cpu);
            if (name instanceof String && !((String) name).isEmpty()) {
                return (String) name;
            }
        } catch (Throwable ignored) {}
        return "CPU";
    }

    @SuppressWarnings("unchecked")
    private static List<String> getFollowers(Object cpu) {
        try {
            Method method = getPlayersFollowingMethod;
            if (!getPlayersFollowingResolved) {
                synchronized (Ae2WebIntegrationHandler.class) {
                    if (!getPlayersFollowingResolved) {
                        try {
                            method = cpu.getClass()
                                .getMethod("getPlayersFollowingCurrentCraft");
                            getPlayersFollowingMethod = method;
                        } catch (Throwable ignored) {}
                        getPlayersFollowingResolved = true;
                    }
                }
            }
            if (method == null) return Collections.emptyList();
            Object raw = method.invoke(cpu);
            if (raw instanceof List) {
                List<?> rawList = (List<?>) raw;
                if (rawList.isEmpty()) return Collections.emptyList();
                List<String> copy = new ArrayList<String>(rawList.size());
                for (Object item : rawList) {
                    if (item instanceof String) {
                        copy.add((String) item);
                    }
                }
                return copy;
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    private static boolean shouldSendNotification(List<String> followers, boolean isPlayerRequested,
        List<String> playerRequesters, long durationMillis, int alwaysNotifyAfterMinutes) {

        boolean durationExceeded = isPlayerRequested && alwaysNotifyAfterMinutes > 0
            && durationMillis >= alwaysNotifyAfterMinutes * 60_000L;

        // Case 1: Someone subscribed via the Follow button in the Crafting CPU GUI
        if (followers != null && !followers.isEmpty()) {
            if (durationExceeded) return true;
            for (String follower : followers) {
                if (follower != null && !follower.trim()
                    .isEmpty() && AfkHandler.isPlayerOfflineOrAfk(follower)) {
                    return true;
                }
            }
            return false;
        }

        // Case 2: Nobody pressed Follow -> always notify if requested by a player, ignore if machine
        return isPlayerRequested;
    }
}
