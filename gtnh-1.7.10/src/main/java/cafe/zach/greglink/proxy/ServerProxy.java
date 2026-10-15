package cafe.zach.greglink.proxy;

import static cafe.zach.greglink.GregLink.LOG;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.discord.api.config.ConfigHandler;
import cafe.zach.greglink.Tags;
import cafe.zach.greglink.bridge.BridgeManager;
import cafe.zach.greglink.command.ReloadEmojisCommand;
import cafe.zach.greglink.config.GregLinkConfig;
import cpw.mods.fml.common.event.*;

public class ServerProxy implements IProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GregLinkConfig config = new GregLinkConfig();
        ConfigHandler.setInstance(config);
        config.load();
        // pulse a save to update new defaults in the config
        config.save();

        BridgeManager.registerEventHandlers();

        LOG.info("Greg Link at version " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        BridgeManager.registerActions();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ReloadEmojisCommand());
        BridgeManager.connect();
        ActionRegistry.fireMinecraft(ActionRegistry.ON_MINECRAFT_SERVER_STARTING, MinecraftContext.forEmpty());
    }

    public void serverStarted(FMLServerStartedEvent event) {
        ActionRegistry.fireMinecraft(ActionRegistry.ON_MINECRAFT_SERVER_STARTED, MinecraftContext.forEmpty());
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        ActionRegistry.fireMinecraft(ActionRegistry.ON_MINECRAFT_SERVER_STOPPING, MinecraftContext.forEmpty());
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        ActionRegistry.fireMinecraft(ActionRegistry.ON_MINECRAFT_SERVER_STOPPED, MinecraftContext.forEmpty());
        BridgeManager.disconnect();
    }
}
