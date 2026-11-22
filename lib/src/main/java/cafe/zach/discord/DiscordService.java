package cafe.zach.discord;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
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
                .createLight(
                    config.discordToken,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT)
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
        sendMessage(channelId, message, Collections.emptyList());
    }

    public void sendMessage(String channelId, String message, Collection<String> allowedUserMentions) {
        if (!running.get() || api == null) return;

        TextChannel channel = api.getTextChannelById(channelId);
        if (channel == null) return;

        MessageCreateAction action = channel.sendMessage(message)
            .setAllowedMentions(Collections.emptyList());
        if (allowedUserMentions != null && !allowedUserMentions.isEmpty()) {
            action.mentionUsers(allowedUserMentions);
        }
        action.queue(null, error -> {}); // silently drop failed sends for now, hook for logging later
    }

    public void replyToMessage(String channelId, String messageId, String message) {
        if (!running.get() || api == null) return;

        TextChannel channel = api.getTextChannelById(channelId);
        if (channel == null) return;

        MessageCreateAction action = channel.sendMessage(message)
            .setAllowedMentions(Collections.emptyList())
            .mentionRepliedUser(false);
        if (messageId != null && !messageId.isEmpty()) {
            action.setMessageReference(messageId);
        }
        action.queue(null, error -> {});
    }

    public void sendDirectMessage(String userId, String message) {
        if (!running.get() || api == null || userId == null) return;

        User user = api.getUserById(userId);
        if (user == null) {
            api.retrieveUserById(userId)
                .queue(retrieved -> sendDirectMessage(retrieved, message), error -> {});
            return;
        }
        sendDirectMessage(user, message);
    }

    private void sendDirectMessage(User user, String message) {
        user.openPrivateChannel()
            .queue(
                channel -> channel.sendMessage(message)
                    .setAllowedMentions(Collections.emptyList())
                    .queue(null, error -> {}),
                error -> {});
    }

    public void sendEmbed(String channelId, MessageEmbed embed) {
        if (!running.get() || api == null) return;

        TextChannel channel = api.getTextChannelById(channelId);
        if (channel == null) return;

        channel.sendMessageEmbeds(embed)
            .setAllowedMentions(Collections.emptyList())
            .queue(null, error -> {});
    }

    public void sendEmbedWithAvatar(String channelId, MessageEmbed embed, byte[] avatar) {
        sendEmbedWithAvatar(channelId, embed, avatar, Collections.emptyList());
    }

    public void sendEmbedWithAvatar(String channelId, MessageEmbed embed, byte[] avatar,
        Collection<String> allowedUserMentions) {
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

            MessageCreateAction action = channel.sendFiles(FileUpload.fromData(avatar, filename))
                .setEmbeds(embed);
            sendEmbedAction(action, allowedUserMentions);
        } else {
            // fall back to embed without avatar if fetch failed
            sendEmbedAction(channel.sendMessageEmbeds(embed), allowedUserMentions);
        }
    }

    public void sendEmbedWithAvatarAsync(String channelId, MessageEmbed embed, String avatarIdentifier) {
        sendEmbedWithAvatarAsync(channelId, embed, avatarIdentifier, Collections.emptyList());
    }

    public void sendEmbedWithAvatarAsync(String channelId, MessageEmbed embed, String avatarIdentifier,
        Collection<String> allowedUserMentions) {
        if (!running.get() || api == null) return;

        byte[] cachedAvatar = AvatarUtil.getCachedAvatar(avatarIdentifier);
        if (cachedAvatar != null) {
            sendEmbedWithAvatar(channelId, embed, cachedAvatar, allowedUserMentions);
            return;
        }

        ExecutorService pool = avatarPool;
        if (pool == null || pool.isShutdown()) {
            sendEmbedWithAvatar(channelId, embed, null, allowedUserMentions);
            return;
        }

        try {
            pool.execute(() -> {
                byte[] avatar = AvatarUtil.fetchPaddedAvatar(avatarIdentifier);
                sendEmbedWithAvatar(channelId, embed, avatar, allowedUserMentions);
            });
        } catch (RejectedExecutionException ignored) {
            sendEmbedWithAvatar(channelId, embed, null, allowedUserMentions);
        }
    }

    private static void sendEmbedAction(MessageCreateAction action, Collection<String> allowedUserMentions) {
        action.setAllowedMentions(Collections.emptyList());
        if (allowedUserMentions != null && !allowedUserMentions.isEmpty()) {
            StringBuilder content = new StringBuilder();
            for (String userId : allowedUserMentions) {
                if (content.length() > 0) content.append(' ');
                content.append("<@")
                    .append(userId)
                    .append('>');
            }
            action.setContent(content.toString())
                .mentionUsers(allowedUserMentions);
        }
        action.queue(null, error -> {});
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
