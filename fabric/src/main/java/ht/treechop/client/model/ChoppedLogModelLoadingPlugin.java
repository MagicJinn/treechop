package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

@Environment(EnvType.CLIENT)
public class ChoppedLogModelLoadingPlugin implements ModelLoadingPlugin {

    @Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
            // instanceof: avoid identity mismatch with PolymerFabricChoppedLogBlock vs static field
            if (!(context.state().getBlock() instanceof ChoppedLogBlock)) {
                return model;
            }
            if (ChoppedLogBlockStateModel.defaultParticleMaterial == null) {
                ChoppedLogBlockStateModel.defaultParticleMaterial = model.particleMaterial();
            }
            return new FabricChoppedLogBlockStateModel(context.state());
        });
    }
}
