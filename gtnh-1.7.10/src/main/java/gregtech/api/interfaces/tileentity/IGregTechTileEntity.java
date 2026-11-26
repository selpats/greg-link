package gregtech.api.interfaces.tileentity;

import net.minecraft.world.World;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

public interface IGregTechTileEntity {

    World getWorld();

    int getXCoord();

    short getYCoord();

    int getZCoord();

    String getOwnerName();

    IMetaTileEntity getMetaTileEntity();
}
