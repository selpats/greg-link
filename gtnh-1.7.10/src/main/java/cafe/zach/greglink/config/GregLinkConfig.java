package cafe.zach.greglink.config;

import java.io.File;

import com.google.gson.*;

import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.GregLink;
import cpw.mods.fml.common.Loader;

public class GregLinkConfig extends ConfigHandler {

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
    protected void onLoad(JsonObject json) {}

    @Override
    protected void onSave(JsonObject json) {}

    @Override
    protected void onLoadError(File file, Exception error) {
        GregLink.LOG.error("Failed to load GregLink config from " + file.getAbsolutePath(), error);
    }

    @Override
    protected void onSaveError(File file, Exception error) {
        GregLink.LOG.error("Failed to save GregLink config to " + file.getAbsolutePath(), error);
    }
}
