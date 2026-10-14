package ht.treechop.client.model;

import ht.treechop.common.registry.FabricModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;

@Environment(EnvType.CLIENT)
public class ChoppedLogModelLoadingPlugin implements ModelLoadingPlugin {

    @Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
            if (context.state().getBlock() != FabricModBlocks.CHOPPED_LOG) {
                return model;
            }
            if (ChoppedLogBlockStateModel.defaultParticleMaterial == null) {
                ChoppedLogBlockStateModel.defaultParticleMaterial = model.particleMaterial();
            }
            return new FabricChoppedLogBlockStateModel();
        });
    }
}
