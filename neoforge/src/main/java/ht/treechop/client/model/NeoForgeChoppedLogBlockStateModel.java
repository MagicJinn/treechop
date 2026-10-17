package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.NeoForgeRegistry;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NeoForgeChoppedLogBlockStateModel extends ChoppedLogBlockStateModel implements DynamicBlockStateModel {

    public static void overrideBlockStateModels(ModelEvent.ModifyBakingResult event) {
        var models = event.getBakingResult().blockStateModels();
        for (BlockState blockState : NeoForgeRegistry.Blocks.CHOPPED_LOG.get().getStateDefinition().getPossibleStates()) {
            var existing = models.get(blockState);
            if (existing == null) {
                continue;
            }
            if (ChoppedLogBlockStateModel.defaultParticleMaterial == null) {
                ChoppedLogBlockStateModel.defaultParticleMaterial = existing.particleMaterial();
            }
            models.put(blockState, new NeoForgeChoppedLogBlockStateModel());
        }
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            BlockState strippedState = ChopUtil.getStrippedState(level, pos, entity.getOriginalState());
            Map<Direction, BlockState> strippedNeighbors = getStrippedNeighbors(level, pos, entity);
            List<BakedQuad> quads = getQuads(strippedState, entity.getShape(), entity.getRadius(), random, strippedNeighbors).toList();
            Material.Baked particle = net.minecraft.client.Minecraft.getInstance()
                    .getModelManager().getBlockStateModelSet().get(strippedState).particleMaterial();
            parts.add(new QuadListPart(quads, particle));
        }
    }

    @Override
    public @NotNull Material.Baked particleMaterial() {
        return super.particleMaterial();
    }

    private record QuadListPart(List<BakedQuad> quads, Material.Baked particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction != null) {
                return Collections.emptyList();
            }
            return quads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle != null ? particle : ChoppedLogBlockStateModel.defaultParticleMaterial;
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}
