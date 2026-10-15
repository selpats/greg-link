package cafe.zach.greglink.config;

import java.io.File;

import com.google.gson.*;

import cafe.zach.discord.api.config.ConfigHandler;
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
}
