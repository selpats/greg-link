package cafe.zach.discord.api.config;

import com.google.gson.JsonObject;

public class ChannelFilters {

    public final boolean ignoreBots;
    public final boolean relayCommands;
    public final boolean relayAfk;

    public ChannelFilters(boolean ignoreBots) {
        this(ignoreBots, false, true);
    }

    public ChannelFilters(boolean ignoreBots, boolean relayCommands) {
        this(ignoreBots, relayCommands, true);
    }

    public ChannelFilters(boolean ignoreBots, boolean relayCommands, boolean relayAfk) {
        this.ignoreBots = ignoreBots;
        this.relayCommands = relayCommands;
        this.relayAfk = relayAfk;
    }

    public static ChannelFilters fromJson(JsonObject json) {
        boolean ignoreBots = !json.has("ignoreBots") || json.get("ignoreBots")
            .getAsBoolean();
        boolean relayCommands = json.has("relayCommands") && json.get("relayCommands")
            .getAsBoolean();
        if (
            json.has("ignoreCommands") && json.get("ignoreCommands")
                .getAsBoolean()
        ) {
            relayCommands = false;
        }
        boolean relayAfk = !json.has("relayAfk") || json.get("relayAfk")
            .getAsBoolean();
        if (
            json.has("ignoreAfk") && json.get("ignoreAfk")
                .getAsBoolean()
        ) {
            relayAfk = false;
        }
        return new ChannelFilters(ignoreBots, relayCommands, relayAfk);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("ignoreBots", ignoreBots);
        json.addProperty("relayCommands", relayCommands);
        json.addProperty("relayAfk", relayAfk);
        return json;
    }
}
