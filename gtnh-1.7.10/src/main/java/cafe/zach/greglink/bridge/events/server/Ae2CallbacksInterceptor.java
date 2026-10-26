package cafe.zach.greglink.bridge.events.server;

import pl.kuba6000.ae2webintegration.core.api.PlayerIdentity;
import pl.kuba6000.ae2webintegration.core.interfaces.IAEGrid;
import pl.kuba6000.ae2webintegration.core.interfaces.ICraftingCPUCluster;
import pl.kuba6000.ae2webintegration.core.tracking.AEMixinCallbacks;

public class Ae2CallbacksInterceptor extends AEMixinCallbacks {

    public static void install() {
        if (!(AEMixinCallbacks.INSTANCE instanceof Ae2CallbacksInterceptor)) {
            AEMixinCallbacks.INSTANCE = new Ae2CallbacksInterceptor();
        }
    }

    @Override
    public void jobStarted(ICraftingCPUCluster cpuCluster, IAEGrid grid, boolean isMerging, boolean isMachineRequest,
        PlayerIdentity nativeRequester) {
        super.jobStarted(cpuCluster, grid, isMerging, isMachineRequest, nativeRequester);
        Ae2WebIntegrationHandler.onJobStarted(cpuCluster, isMerging, isMachineRequest, nativeRequester);
    }

    @Override
    public void jobCompleted(IAEGrid grid, ICraftingCPUCluster cpu) {
        Ae2WebIntegrationHandler.onBeforeJobCompleted(grid, cpu);
        super.jobCompleted(grid, cpu);
    }

    @Override
    public void jobCancelled(IAEGrid grid, ICraftingCPUCluster cpu) {
        Ae2WebIntegrationHandler.onBeforeJobCancelled(cpu);
        super.jobCancelled(grid, cpu);
    }
}
