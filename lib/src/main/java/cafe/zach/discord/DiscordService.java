package cafe.zach.discord;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

import cafe.zach.discord.api.action.format.EmojiRegistry;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.discord.api.exceptions.InvalidDiscordConfigurationException;
import cafe.zach.discord.listeners.OnMessageReceived;
import cafe.zach.discord.listeners.ReadyListener;

class DiscordService {

    private JDA api;
    private ExecutorService eventPool;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DiscordService(ConfigHandler config) throws InterruptedException, InvalidDiscordConfigurationException {
        initialize(config);
    }

    public synchronized void initialize(ConfigHandler config)
        throws InterruptedException, InvalidDiscordConfigurationException {

        if (running.get()) {
            throw new IllegalStateException("DiscordService is already running — call restart() instead");
        }

        try {
            eventPool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "GregLink-Discord");
                t.setDaemon(true); // won't block MC shutdown
                return t;
            });

            api = JDABuilder
                .createLight(
                    config.discordToken,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .setEventPool(eventPool)
                .addEventListeners(new ReadyListener(), new OnMessageReceived())
                .build()
                .awaitReady();

            running.set(true);

        } catch (InvalidTokenException | IllegalArgumentException e) {
            destroyPool();
            throw new InvalidDiscordConfigurationException("provided token is invalid");
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

        if (api != null) {
            api.shutdown();
            api = null;
        }

        destroyPool();
    }

    public synchronized void awaitDestroy() {
        running.set(false);

        if (api != null) {
            api.shutdown();
            try {
                if (!api.awaitShutdown(Duration.ofSeconds(5))) {
                    // if the queue is not empty after 5 seconds, force immediate shutdown
                    api.shutdownNow();
                    api.awaitShutdown();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    public boolean isRunning() {
        return running.get();
    }

    private void destroyPool() {
        if (eventPool != null) {
            eventPool.shutdownNow();
            eventPool = null;
        }
    }
}
