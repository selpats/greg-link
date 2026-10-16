package cafe.zach.greglink.bridge.events.server;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cafe.zach.greglink.compat.BetterQuestingHelper;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ServerChatHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerChat(ServerChatEvent event) {
        if (event.player == null || event.player instanceof FakePlayer) return;
        if (event.player.worldObj == null || event.player.worldObj.provider == null) return;

        String message = BetterQuestingHelper.formatQuestShare(event.message);

        ActionRegistry.fireMinecraft(
            ActionRegistry.ON_MINECRAFT_CHAT,
            MinecraftContext.forChat(
                event.player.getCommandSenderName(),
                event.player.getUniqueID()
                    .toString(),
                message,
                event.player.worldObj.getWorldInfo()
                    .getWorldName(),
                String.valueOf(event.player.worldObj.provider.dimensionId)));
    }
}
