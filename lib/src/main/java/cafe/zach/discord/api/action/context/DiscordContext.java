package cafe.zach.discord.api.action.context;

public class DiscordContext {

    public final String username;
    public final String replyTo;
    public final String content;
    public final String channelId;
    public final String guildId;
    public final boolean isBot;
    public final int guildCount;

    private DiscordContext(Builder builder) {
        this.username = builder.username;
        this.replyTo = builder.replyTo;
        this.content = builder.content;
        this.channelId = builder.channelId;
        this.guildId = builder.guildId;
        this.isBot = builder.isBot;
        this.guildCount = builder.guildCount;
    }

    public static DiscordContext forReady(int guildCount) {
        return new Builder().guildCount(guildCount)
            .build();
    }

    public static DiscordContext forMessage(String username, String content, String channelId, String guildId,
        boolean isBot) {
        return forMessage(username, null, content, channelId, guildId, isBot);
    }

    public static DiscordContext forMessage(String username, String replyTo, String content, String channelId,
        String guildId, boolean isBot) {
        return new Builder().username(username)
            .replyTo(replyTo)
            .content(content)
            .channelId(channelId)
            .guildId(guildId)
            .isBot(isBot)
            .build();
    }

    private static class Builder {

        private String username;
        private String replyTo;
        private String content;
        private String channelId;
        private String guildId;
        private boolean isBot;
        private int guildCount;

        private Builder username(String username) {
            this.username = username;
            return this;
        }

        private Builder replyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        private Builder content(String content) {
            this.content = content;
            return this;
        }

        private Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        private Builder guildId(String guildId) {
            this.guildId = guildId;
            return this;
        }

        private Builder isBot(boolean isBot) {
            this.isBot = isBot;
            return this;
        }

        private Builder guildCount(int guildCount) {
            this.guildCount = guildCount;
            return this;
        }

        private DiscordContext build() {
            return new DiscordContext(this);
        }
    }
}
