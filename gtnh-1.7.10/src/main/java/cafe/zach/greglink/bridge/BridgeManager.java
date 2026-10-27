package cafe.zach.greglink.bridge;

import net.minecraftforge.common.MinecraftForge;

import cafe.zach.discord.DiscordBridge;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.discord.api.exceptions.InvalidDiscordConfigurationException;
import cafe.zach.greglink.GregLink;
import cafe.zach.greglink.bridge.events.server.*;
import cafe.zach.greglink.bridge.registry.DiscordActionRegistry;
import cafe.zach.greglink.bridge.registry.MinecraftActionRegistry;
import cpw.mods.fml.common.FMLCommonHandler;

public class BridgeManager {

    public static void registerEventHandlers() {
        // Chat itself
        MinecraftForge.EVENT_BUS.register(new ServerChatHandler());

        // Misc
        MinecraftForge.EVENT_BUS.register(new AchievementHandler());
        MinecraftForge.EVENT_BUS.register(new LivingDeathHandler());

        // Join/leave events
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLoggedInHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLoggedOutHandler());

        // ServerUtilities AFK logger listener
        AfkHandler.register();
    }

    public static void registerActions() {
        ActionRegistry.clear();
        MinecraftActionRegistry.register();
        DiscordActionRegistry.register();
    }

    public static void connect() {
        AfkHandler.register();
        Ae2WebIntegrationHandler.register();
        try {
            DiscordBridge.createConnection();
        } catch (InvalidDiscordConfigurationException | InterruptedException e) {
            GregLink.LOG.fatal("Could not connect to Discord!", e);
        }
    }

    public static void disconnect() {
        AfkHandler.unregister();
        DiscordBridge.shutdown();
    }
}
