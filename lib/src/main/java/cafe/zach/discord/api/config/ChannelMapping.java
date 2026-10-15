package cafe.zach.discord.api.config;

import com.google.gson.JsonObject;

public class ChannelMapping {

    public final String id;
    public final String discordChannelId;
    public final ChannelDirections directions;
    public final ChannelDiscordConfig discord;
    public final ChannelMinecraftConfig minecraft;
    public final ChannelFilters filters;

    public ChannelMapping(String id, String discordChannelId, ChannelDirections directions,
        ChannelDiscordConfig discord, ChannelMinecraftConfig minecraft, ChannelFilters filters) {
        this.id = id;
        this.discordChannelId = discordChannelId;
        this.directions = directions;
        this.discord = discord;
        this.minecraft = minecraft;
        this.filters = filters;
    }

    public boolean acceptsFromDiscord() {
        return directions.discordToMinecraft;
    }

    public boolean acceptsFromMinecraft() {
        return directions.minecraftToDiscord;
    }

    public boolean matchesDimension(String dimension) {
        return minecraft.matchesDimension(dimension);
    }

    public static ChannelMapping fromJson(JsonObject json) {
        ChannelFilters filters = json.has("filters") ? ChannelFilters.fromJson(json.getAsJsonObject("filters"))
            : new ChannelFilters(true);

        if (json.has("relayCommands")) {
            filters = new ChannelFilters(
                filters.ignoreBots,
                json.get("relayCommands")
                    .getAsBoolean());
        } else if (
            json.has("ignoreCommands") && json.get("ignoreCommands")
                .getAsBoolean()
        ) {
            filters = new ChannelFilters(filters.ignoreBots, false);
        }

        return new ChannelMapping(
            json.has("id") ? json.get("id")
                .getAsString() : "unnamed",
            json.has("discordChannelId") ? json.get("discordChannelId")
                .getAsString() : "",
            json.has("directions") ? ChannelDirections.fromJson(json.getAsJsonObject("directions"))
                : new ChannelDirections(true, true),
            json.has("discord") ? ChannelDiscordConfig.fromJson(json.getAsJsonObject("discord"))
                : new ChannelDiscordConfig(true, false),
            json.has("minecraft") ? ChannelMinecraftConfig.fromJson(json.getAsJsonObject("minecraft"))
                : new ChannelMinecraftConfig(java.util.Collections.singletonList("*")),
            filters);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("discordChannelId", discordChannelId);
        json.add("directions", directions.toJson());
        json.add("discord", discord.toJson());
        json.add("minecraft", minecraft.toJson());
        json.add("filters", filters.toJson());
        return json;
    }
}
