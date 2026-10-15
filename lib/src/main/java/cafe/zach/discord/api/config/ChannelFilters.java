package cafe.zach.discord.api.config;

import com.google.gson.JsonObject;

public class ChannelFilters {

    public final boolean ignoreBots;
    public final boolean relayCommands;

    public ChannelFilters(boolean ignoreBots) {
        this(ignoreBots, false);
    }

    public ChannelFilters(boolean ignoreBots, boolean relayCommands) {
        this.ignoreBots = ignoreBots;
        this.relayCommands = relayCommands;
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
        return new ChannelFilters(ignoreBots, relayCommands);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("ignoreBots", ignoreBots);
        json.addProperty("relayCommands", relayCommands);
        return json;
    }
}
