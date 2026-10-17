package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class FabricChoppedLogBlockStateModel extends ChoppedLogBlockStateModel implements FabricBlockStateModel {

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            BlockState strippedState = ChopUtil.getStrippedState(level, pos, entity.getOriginalState());
            Map<Direction, BlockState> strippedNeighbors = getStrippedNeighbors(level, pos, entity);

            getQuads(
                    strippedState,
                    entity.getShape(),
                    entity.getRadius(),
                    random,
                    strippedNeighbors
            ).forEach(quad -> emitter.fromBakedQuad(quad).emit());
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            return new GeometryKey(
                    entity.getOriginalState(),
                    entity.getShape(),
                    entity.getRadius(),
                    getStrippedNeighbors(level, pos, entity)
            );
        }
        return null;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            BlockState stripped = ChopUtil.getStrippedState(level, pos, entity.getOriginalState());
            return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(stripped).particleMaterial();
        }
        return particleMaterial();
    }

    private record GeometryKey(BlockState original, ChoppedLogShape shape, int radius, Map<Direction, BlockState> neighbors) {
        private GeometryKey {
            neighbors = Map.copyOf(neighbors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GeometryKey key)) return false;
            return radius == key.radius
                    && shape == key.shape
                    && Objects.equals(original, key.original)
                    && Objects.equals(neighbors, key.neighbors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(original, shape, radius, neighbors);
        }
    }
}
