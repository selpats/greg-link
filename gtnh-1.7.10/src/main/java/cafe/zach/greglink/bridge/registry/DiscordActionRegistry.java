package cafe.zach.greglink.bridge.registry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentText;

import cafe.zach.discord.api.action.ChatSender;
import cafe.zach.discord.api.action.common.discord.BroadcastMessageToChat;
import cafe.zach.discord.api.action.common.discord.BroadcastReadyToChat;
import cafe.zach.discord.api.action.registry.ActionRegistry;

public class DiscordActionRegistry {

    public static void register() {
        // doesn't really need to be DI, but keep in mind sender is made here
        ChatSender sender = msg -> {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) return;
            ServerConfigurationManager configManager = server.getConfigurationManager();
            if (configManager == null) return;
            configManager.sendChatMsg(new ChatComponentText(msg));
        };

        ActionRegistry.register(ActionRegistry.ON_DISCORD_MESSAGE, BroadcastMessageToChat.create(sender));
        ActionRegistry.register(ActionRegistry.ON_DISCORD_READY, BroadcastReadyToChat.create(sender));
    }
}
