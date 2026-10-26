package cafe.zach.greglink.bridge.events.server;

import net.minecraftforge.common.util.FakePlayer;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class PlayerLoggedOutHandler {

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == null || event.player instanceof FakePlayer) return;

        AfkHandler.clearPlayer(event.player.getCommandSenderName());

        ActionRegistry.fireMinecraft(
            ActionRegistry.ON_MINECRAFT_PLAYER_LEAVE,
            MinecraftContext.forPlayerLeave(
                event.player.getCommandSenderName(),
                event.player.getUniqueID()
                    .toString()));
    }
}
