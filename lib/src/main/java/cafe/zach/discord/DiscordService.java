package cafe.zach.discord;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

import cafe.zach.discord.api.action.format.AvatarUtil;
import cafe.zach.discord.api.action.format.EmojiRegistry;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.discord.api.exceptions.InvalidDiscordConfigurationException;
import cafe.zach.discord.listeners.OnMessageReceived;
import cafe.zach.discord.listeners.ReadyListener;

class DiscordService {

    private JDA api;
    private ExecutorService eventPool;
    private ExecutorService avatarPool;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DiscordService(ConfigHandler config) throws InterruptedException, InvalidDiscordConfigurationException {
        initialize(config);
    }

    public synchronized void initialize(ConfigHandler config)
        throws InterruptedException, InvalidDiscordConfigurationException {

        if (running.get()) {
            throw new IllegalStateException("DiscordService is already running — call restart() instead");
        }

        if (
            config == null || config.discordToken == null
                || config.discordToken.trim()
                    .isEmpty()
        ) {
            throw new InvalidDiscordConfigurationException("provided token is empty");
        }

        JDA pendingApi = null;
        try {
            eventPool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "GregLink-Discord");
                t.setDaemon(true); // won't block MC shutdown
                return t;
            });

            avatarPool = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), r -> {
                Thread t = new Thread(r, "GregLink-Avatar");
                t.setDaemon(true);
                return t;
            }, new ThreadPoolExecutor.DiscardOldestPolicy());

            pendingApi = JDABuilder
                .createLight(config.discordToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setEventPool(eventPool)
                .addEventListeners(new ReadyListener(), new OnMessageReceived())
                .build();
            pendingApi.awaitReady();
            api = pendingApi;

            running.set(true);

        } catch (InvalidTokenException | IllegalArgumentException e) {
            shutdownPendingApi(pendingApi);
            destroyPool();
            throw new InvalidDiscordConfigurationException("provided token is invalid");
        } catch (InterruptedException e) {
            shutdownPendingApi(pendingApi);
            destroyPool();
            Thread.currentThread()
                .interrupt();
            throw e;
        } catch (Throwable t) {
            shutdownPendingApi(pendingApi);
            destroyPool();
            throw new InvalidDiscordConfigurationException("failed to initialize JDA: " + t.getMessage());
        }
    }

    public synchronized void restart(ConfigHandler config)
        throws InterruptedException, InvalidDiscordConfigurationException {
        destroy();
        initialize(config);
    }

    public synchronized void destroy() {
        running.set(false);

        EmojiRegistry.shutdown();
        AvatarUtil.clearCache();

        if (api != null) {
            api.shutdown();
            api = null;
        }

        destroyPool();
    }

    public synchronized void awaitDestroy() {
        running.set(false);

        EmojiRegistry.shutdown();
        AvatarUtil.clearCache();

        if (api != null) {
            api.shutdown();
            try {
                if (!api.awaitShutdown(Duration.ofSeconds(5))) {
                    // if the queue is not empty after 5 seconds, force immediate shutdown
                    api.shutdownNow();
                    api.awaitShutdown(Duration.ofSeconds(2));
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            api = null;
        }

        destroyPool();
    }

    public void sendMessage(String channelId, String message) {
        if (!running.get() || api == null) return;

        TextChannel channel = api.getTextChannelById(channelId);
        if (channel == null) return;

        channel.sendMessage(message)
            .queue(null, error -> {}); // silently drop failed sends for now, hook for logging later
    }

    public void sendEmbed(String channelId, MessageEmbed embed) {
        if (!running.get() || api == null) return;

        TextChannel channel = api.getTextChannelById(channelId);
        if (channel == null) return;

        channel.sendMessageEmbeds(embed)
            .queue(null, error -> {});
    }

    public void sendEmbedWithAvatar(String channelId, MessageEmbed embed, byte[] avatar) {
        if (!running.get() || api == null) return;

        TextChannel channel = api.getTextChannelById(channelId);
        if (channel == null) return;

        if (
            avatar != null && embed.getAuthor() != null
                && embed.getAuthor()
                    .getIconUrl() != null
        ) {
            String filename = embed.getAuthor()
                .getIconUrl()
                .substring("attachment://".length());

            channel.sendFiles(FileUpload.fromData(avatar, filename))
                .setEmbeds(embed)
                .queue(null, error -> {});
        } else {
            // fall back to embed without avatar if fetch failed
            channel.sendMessageEmbeds(embed)
                .queue(null, error -> {});
        }
    }

    public void sendEmbedWithAvatarAsync(String channelId, MessageEmbed embed, String avatarIdentifier) {
        if (!running.get() || api == null) return;

        byte[] cachedAvatar = AvatarUtil.getCachedAvatar(avatarIdentifier);
        if (cachedAvatar != null) {
            sendEmbedWithAvatar(channelId, embed, cachedAvatar);
            return;
        }

        ExecutorService pool = avatarPool;
        if (pool == null || pool.isShutdown()) {
            sendEmbed(channelId, embed);
            return;
        }

        try {
            pool.execute(() -> {
                byte[] avatar = AvatarUtil.fetchPaddedAvatar(avatarIdentifier);
                sendEmbedWithAvatar(channelId, embed, avatar);
            });
        } catch (RejectedExecutionException ignored) {
            sendEmbed(channelId, embed);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void destroyPool() {
        if (avatarPool != null) {
            avatarPool.shutdownNow();
            avatarPool = null;
        }
        if (eventPool != null) {
            eventPool.shutdownNow();
            eventPool = null;
        }
    }

    private static void shutdownPendingApi(JDA pendingApi) {
        if (pendingApi == null) return;

        pendingApi.shutdownNow();
        try {
            pendingApi.awaitShutdown(Duration.ofSeconds(2));
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }
    }
}
