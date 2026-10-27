package cafe.zach.discord.api.action.context;

import cafe.zach.discord.api.action.format.MessageFormatter;

public class MinecraftContext {

    public final String username;
    public final String uuid;
    public final String content;
    public final String worldName;
    public final String dimensionId;
    public final String dimensionName;
    public final boolean isAfk;

    private MinecraftContext(Builder builder) {
        this.username = MessageFormatter.stripFormatting(builder.username);
        this.uuid = builder.uuid;
        this.content = MessageFormatter.stripFormatting(builder.content);
        this.worldName = builder.worldName;
        this.dimensionId = builder.dimensionId;
        this.dimensionName = builder.dimensionName;
        this.isAfk = builder.isAfk;
    }

    public static MinecraftContext forChat(String username, String uuid, String content, String worldName,
        String dimensionId) {
        return new Builder().username(username)
            .uuid(uuid)
            .content(content)
            .worldName(worldName)
            .dimensionId(dimensionId)
            .build();
    }

    public static MinecraftContext forPlayerJoin(String username, String uuid) {
        return new Builder().username(username)
            .uuid(uuid)
            .build();
    }

    public static MinecraftContext forPlayerLeave(String username, String uuid) {
        return new Builder().username(username)
            .uuid(uuid)
            .build();
    }

    public static MinecraftContext forAchievement(String username, String uuid, String content, String worldName,
        String dimensionId) {
        return new Builder().username(username)
            .uuid(uuid)
            .content(content)
            .worldName(worldName)
            .dimensionId(dimensionId)
            .build();
    }

    public static MinecraftContext forDeath(String username, String uuid, String content, String worldName,
        String dimensionId) {
        return new Builder().username(username)
            .uuid(uuid)
            .content(content)
            .worldName(worldName)
            .dimensionId(dimensionId)
            .build();
    }

    public static MinecraftContext forAfk(String username, String uuid, boolean isAfk) {
        return new Builder().username(username)
            .uuid(uuid)
            .isAfk(isAfk)
            .build();
    }

    public static MinecraftContext forAe2Craft(String username, String content) {
        return new Builder().username(username)
            .content(content)
            .build();
    }

    public static MinecraftContext forEmpty() {
        return new Builder().build();
    }

    private static class Builder {

        private String username;
        private String uuid;
        private String content;
        private String worldName;
        private String dimensionId;
        private String dimensionName;
        private boolean isAfk;

        private Builder username(String username) {
            this.username = username;
            return this;
        }

        private Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        private Builder content(String content) {
            this.content = content;
            return this;
        }

        private Builder worldName(String worldName) {
            this.worldName = worldName;
            return this;
        }

        private Builder dimensionId(String dimensionId) {
            this.dimensionId = dimensionId;
            return this;
        }

        private Builder dimensionName(String dimensionName) {
            this.dimensionName = dimensionName;
            return this;
        }

        private Builder isAfk(boolean isAfk) {
            this.isAfk = isAfk;
            return this;
        }

        private MinecraftContext build() {
            return new MinecraftContext(this);
        }
    }
}
