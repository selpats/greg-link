package cafe.zach.discord.api.config;

import com.google.gson.JsonObject;

public class ChannelFilters {

    public final boolean ignoreBots;
    public final boolean relayAfk;

    public ChannelFilters(boolean ignoreBots) {
        this(ignoreBots, true);
    }

    public ChannelFilters(boolean ignoreBots, boolean relayAfk) {
        this.ignoreBots = ignoreBots;
        this.relayAfk = relayAfk;
    }

    public static ChannelFilters fromJson(JsonObject json) {
        boolean ignoreBots = !json.has("ignoreBots") || json.get("ignoreBots")
            .getAsBoolean();
        boolean relayAfk = !json.has("relayAfk") || json.get("relayAfk")
            .getAsBoolean();
        if (
            json.has("ignoreAfk") && json.get("ignoreAfk")
                .getAsBoolean()
        ) {
            relayAfk = false;
        }
        return new ChannelFilters(ignoreBots, relayAfk);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("ignoreBots", ignoreBots);
        json.addProperty("relayAfk", relayAfk);
        return json;
    }
}
