package cafe.zach.greglink.bridge.events.server;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import cafe.zach.discord.api.action.context.MinecraftContext;
import cafe.zach.discord.api.action.registry.ActionRegistry;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class LivingDeathHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer player)) return;
        if (player instanceof FakePlayer) return;
        if (player.worldObj == null || player.worldObj.provider == null) return;

        String username = player.getCommandSenderName();
        String uuid = player.getUniqueID()
            .toString();

        String deathMessage = player.func_110142_aN()
            .func_151521_b()
            .getUnformattedText();

        String worldName = player.worldObj.getWorldInfo()
            .getWorldName();
        String dimensionId = String.valueOf(player.worldObj.provider.dimensionId);

        ActionRegistry.fireMinecraft(
            ActionRegistry.ON_MINECRAFT_DEATH,
            MinecraftContext.forDeath(username, uuid, deathMessage, worldName, dimensionId));
    }
}
