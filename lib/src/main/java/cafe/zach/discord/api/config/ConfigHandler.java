package cafe.zach.discord.api.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

public abstract class ConfigHandler {

    private static ConfigHandler INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final JsonParser PARSER = new JsonParser();

    // discord
    public String discordToken = "";

    // server
    // not yet used

    // channels
    public List<ChannelMapping> channels = new ArrayList<>(
        Collections.singletonList(
            new ChannelMapping(
                "Example",
                "0000000000000000",
                new ChannelDirections(false, false),
                new ChannelDiscordConfig(true, false),
                new ChannelMinecraftConfig(Collections.singletonList("*")),
                new ChannelFilters(true))));

    public static void setInstance(ConfigHandler instance) {
        INSTANCE = instance;
    }

    public static ConfigHandler getInstance() {
        return INSTANCE;
    }

    protected abstract File getConfigFile();

    protected void onLoad(JsonObject json) {}

    protected void onSave(JsonObject json) {}

    public final void load() {
        File file = getConfigFile();
        if (!Files.exists(file.toPath())) {
            save();
            return;
        }

        try {
            Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
            JsonElement element = PARSER.parse(new JsonReader(reader));
            reader.close();

            if (!element.isJsonObject()) return;
            JsonObject root = element.getAsJsonObject();

            if (root.has("bot")) {
                JsonObject bot = root.getAsJsonObject("bot");
                if (bot.has("token")) discordToken = bot.get("token")
                    .getAsString();
            }

            if (root.has("channels")) {
                channels = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("channels"))
                    channels.add(ChannelMapping.fromJson(el.getAsJsonObject()));
            }

            onLoad(root);

        } catch (Exception e) {
            // lib has no logger, let subclass handle if needed
        }
    }

    public final void save() {
        File file = getConfigFile();
        try {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Unable to create directory");
            }

            JsonObject root = new JsonObject();

            JsonObject bot = new JsonObject();
            bot.addProperty("token", discordToken);
            root.add("bot", bot);

            JsonArray channelArray = new JsonArray();
            for (ChannelMapping mapping : channels) channelArray.add(mapping.toJson());
            root.add("channels", channelArray);

            onSave(root);

            Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            writer.write(GSON.toJson(root));
            writer.close();

        } catch (Exception e) {
            // let subclass handle if needed
        }
    }

    public List<ChannelMapping> getListenChannels() {
        return channels.stream()
            .filter(ChannelMapping::acceptsFromDiscord)
            .collect(Collectors.toList());
    }

    public List<ChannelMapping> getRelayChannels() {
        return channels.stream()
            .filter(ChannelMapping::acceptsFromMinecraft)
            .collect(Collectors.toList());
    }

    public List<ChannelMapping> getChannelsForDimension(String dimension) {
        return getRelayChannels().stream()
            .filter(m -> m.matchesDimension(dimension))
            .collect(Collectors.toList());
    }
}
