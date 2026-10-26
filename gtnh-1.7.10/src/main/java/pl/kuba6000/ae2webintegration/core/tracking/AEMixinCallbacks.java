package pl.kuba6000.ae2webintegration.core.tracking;

import pl.kuba6000.ae2webintegration.core.api.PlayerIdentity;
import pl.kuba6000.ae2webintegration.core.interfaces.IAEGrid;
import pl.kuba6000.ae2webintegration.core.interfaces.ICraftingCPUCluster;

public class AEMixinCallbacks {

    public static AEMixinCallbacks INSTANCE = new AEMixinCallbacks();

    public void jobStarted(ICraftingCPUCluster cpuCluster, IAEGrid grid, boolean isMerging, boolean isMachineRequest,
        PlayerIdentity nativeRequester) {}

    public void jobCompleted(IAEGrid grid, ICraftingCPUCluster cpu) {}

    public void jobCancelled(IAEGrid grid, ICraftingCPUCluster cpu) {}
}
