package cafe.zach.greglink.config;

import java.io.File;
import java.util.Locale;

import com.google.gson.*;

import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.GregLink;
import cpw.mods.fml.common.Loader;

public class GregLinkConfig extends ConfigHandler {

    public enum MentionPriority {
        DISCORD,
        MINECRAFT
    }

    public boolean enableMentions = true;
    public MentionPriority mentionPriority = MentionPriority.DISCORD;

    public static GregLinkConfig get() {
        return (GregLinkConfig) ConfigHandler.getInstance();
    }

    @Override
    protected File getConfigFile() {
        return new File(
            Loader.instance()
                .getConfigDir(),
            "greglink" + File.separatorChar + "config.json");
    }

    @Override
    protected void onLoad(JsonObject json) {
        if (json.has("enableMentions")) {
            try {
                enableMentions = json.get("enableMentions")
                    .getAsBoolean();
            } catch (RuntimeException e) {
                GregLink.LOG.warn("Invalid GregLink enableMentions; using true.", e);
                enableMentions = true;
            }
        }

        if (!json.has("mentionPriority")) return;

        try {
            mentionPriority = MentionPriority.valueOf(
                json.get("mentionPriority")
                    .getAsString()
                    .toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            GregLink.LOG.warn("Invalid GregLink mentionPriority; using DISCORD.", e);
            mentionPriority = MentionPriority.DISCORD;
        }
    }

    @Override
    protected void onSave(JsonObject json) {
        json.addProperty("enableMentions", enableMentions);
        json.addProperty("mentionPriority", mentionPriority.name());
    }

    @Override
    protected void onLoadError(File file, Exception error) {
        GregLink.LOG.error("Failed to load GregLink config from " + file.getAbsolutePath(), error);
    }

    @Override
    protected void onSaveError(File file, Exception error) {
        GregLink.LOG.error("Failed to save GregLink config to " + file.getAbsolutePath(), error);
    }
}
