package cafe.zach.greglink.bridge.events.server;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import cafe.zach.greglink.GregLink;
import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.GTMod;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.data.GTPowerfailTracker;

public class GtPowerfailInterceptor extends GTPowerfailTracker {

    public static boolean install() {
        if (GTMod.proxy == null || GTMod.proxy.powerfailTracker == null) {
            return false;
        }
        GTPowerfailTracker current = GTMod.proxy.powerfailTracker;
        if (current instanceof GtPowerfailInterceptor) {
            return true;
        }

        FMLCommonHandler.instance()
            .bus()
            .unregister(current);
        MinecraftForge.EVENT_BUS.unregister(current);

        GtPowerfailInterceptor interceptor = new GtPowerfailInterceptor();
        GTMod.proxy.powerfailTracker = interceptor;

        FMLCommonHandler.instance()
            .bus()
            .register(interceptor);
        MinecraftForge.EVENT_BUS.register(interceptor);
        return true;
    }

    @Override
    public void createPowerfailEvent(IGregTechTileEntity igte) {
        super.createPowerfailEvent(igte);
        if (igte == null) return;
        try {
            World world = igte.getWorld();
            if (world == null || world.provider == null) return;

            int dim = world.provider.dimensionId;
            String dimName = world.provider.getDimensionName();
            int x = igte.getXCoord();
            int y = igte.getYCoord();
            int z = igte.getZCoord();

            IMetaTileEntity mte = igte.getMetaTileEntity();
            String machineName = mte != null ? mte.getLocalName() : "Machine";
            String ownerName = igte.getOwnerName();

            GtPowerfailHandler.onPowerfail(dim, dimName, x, y, z, machineName, ownerName);
        } catch (Throwable t) {
            GregLink.LOG.warn("Error handling GregTech powerfail event.", t);
        }
    }
}
