package cafe.zach.discord.api.action.context;

public class DiscordContext {

    public final String username;
    public final String replyTo;
    public final String content;
    public final String channelId;
    public final String guildId;
    public final String userId;
    public final String messageId;
    public final boolean directMessage;
    public final boolean isBot;
    public final int guildCount;

    private DiscordContext(Builder builder) {
        this.username = builder.username;
        this.replyTo = builder.replyTo;
        this.content = builder.content;
        this.channelId = builder.channelId;
        this.guildId = builder.guildId;
        this.userId = builder.userId;
        this.messageId = builder.messageId;
        this.directMessage = builder.directMessage;
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
        return forMessage(username, replyTo, content, channelId, guildId, null, isBot);
    }

    public static DiscordContext forMessage(String username, String replyTo, String content, String channelId,
        String guildId, String userId, boolean isBot) {
        return forMessage(username, replyTo, content, channelId, guildId, userId, null, isBot);
    }

    public static DiscordContext forMessage(String username, String replyTo, String content, String channelId,
        String guildId, String userId, String messageId, boolean isBot) {
        return new Builder().username(username)
            .replyTo(replyTo)
            .content(content)
            .channelId(channelId)
            .guildId(guildId)
            .userId(userId)
            .messageId(messageId)
            .isBot(isBot)
            .build();
    }

    public static DiscordContext forDirectMessage(String username, String userId, String content, String channelId,
        boolean isBot) {
        return forDirectMessage(username, userId, content, channelId, null, isBot);
    }

    public static DiscordContext forDirectMessage(String username, String userId, String content, String channelId,
        String messageId, boolean isBot) {
        return new Builder().username(username)
            .userId(userId)
            .content(content)
            .channelId(channelId)
            .messageId(messageId)
            .isBot(isBot)
            .directMessage(true)
            .build();
    }

    private static class Builder {

        private String username;
        private String replyTo;
        private String content;
        private String channelId;
        private String guildId;
        private String userId;
        private String messageId;
        private boolean isBot;
        private boolean directMessage;
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

        private Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        private Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        private Builder isBot(boolean isBot) {
            this.isBot = isBot;
            return this;
        }

        private Builder directMessage(boolean directMessage) {
            this.directMessage = directMessage;
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
