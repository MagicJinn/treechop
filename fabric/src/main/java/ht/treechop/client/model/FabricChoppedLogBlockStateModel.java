package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.treechop.common.properties.ChoppedWood;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class FabricChoppedLogBlockStateModel extends ChoppedLogBlockStateModel implements FabricBlockStateModel {
    private final ChoppedWood bakedWood;
    private final int bakedRadius;

    public FabricChoppedLogBlockStateModel(BlockState bakedState) {
        this.bakedWood = bakedState.hasProperty(ChoppedLogBlock.WOOD)
                ? bakedState.getValue(ChoppedLogBlock.WOOD)
                : ChoppedWood.OAK;
        this.bakedRadius = bakedState.hasProperty(ChoppedLogBlock.RADIUS)
                ? bakedState.getValue(ChoppedLogBlock.RADIUS)
                : ChoppedLogBlock.DEFAULT_UNCHOPPED_RADIUS;
    }

    private static BlockState worldState(BlockAndTintGetter level, BlockPos pos, BlockState queried) {
        BlockState inWorld = level.getBlockState(pos);
        return inWorld.getBlock() instanceof ChoppedLogBlock ? inWorld : queried;
    }

    private BlockState strippedFor(BlockAndTintGetter level, BlockPos pos, BlockState world, @Nullable ChoppedLogBlock.MyEntity entity) {
        ChoppedWood wood = world.hasProperty(ChoppedLogBlock.WOOD)
                ? world.getValue(ChoppedLogBlock.WOOD)
                : this.bakedWood;
        // Vanilla kinds in the enum: trust WOOD (fixes first-frame flash).
        if (wood != ChoppedWood.OAK) {
            return wood.polymerDisplayBlockState();
        }
        // WOOD=OAK is also the bucket for modded woods. Prefer BE original.
        if (entity != null) {
            return ChopUtil.getStrippedState(level, pos, entity.getOriginalState());
        }
        return ChopUtil.getStrippedState(level, pos, visualLogState(world, null));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        // No level context: still honor baked WOOD so we never fall back to a shared oak JSON.
        BlockState stripped = this.bakedWood.polymerDisplayBlockState();
        List<BakedQuad> quads = getQuads(stripped, ChoppedLogShape.PILLAR_Y, this.bakedRadius, random, Map.of()).toList();
        parts.add(new StaticPart(quads, Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(stripped).particleMaterial()));
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
        BlockState world = worldState(level, pos, state);
        ChoppedLogBlock.MyEntity entity = level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity e ? e : null;
        BlockState strippedState = strippedFor(level, pos, world, entity);
        Map<Direction, BlockState> strippedNeighbors = entity != null
                ? getStrippedNeighbors(level, pos, entity)
                : Collections.emptyMap();

        getQuads(
                strippedState,
                visualShape(entity),
                visualRadius(world, entity),
                random,
                strippedNeighbors
        ).forEach(quad -> emitter.fromBakedQuad(quad).emit());
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        BlockState world = worldState(level, pos, state);
        ChoppedLogBlock.MyEntity entity = level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity e ? e : null;
        Map<Direction, BlockState> neighbors = entity != null
                ? getStrippedNeighbors(level, pos, entity)
                : Collections.emptyMap();
        return new GeometryKey(
                strippedFor(level, pos, world, entity),
                visualShape(entity),
                visualRadius(world, entity),
                neighbors
        );
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        BlockState world = worldState(level, pos, state);
        ChoppedLogBlock.MyEntity entity = level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity e ? e : null;
        BlockState stripped = strippedFor(level, pos, world, entity);
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(stripped).particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial() {
        BlockState stripped = this.bakedWood.polymerDisplayBlockState();
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(stripped).particleMaterial();
    }

    private record StaticPart(List<BakedQuad> quads, Material.Baked particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            return direction == null ? quads : List.of();
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

    private record GeometryKey(BlockState stripped, ChoppedLogShape shape, int radius, Map<Direction, BlockState> neighbors) {
        private GeometryKey {
            neighbors = Map.copyOf(neighbors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GeometryKey key)) return false;
            return radius == key.radius
                    && shape == key.shape
                    && Objects.equals(stripped, key.stripped)
                    && Objects.equals(neighbors, key.neighbors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stripped, shape, radius, neighbors);
        }
    }
}
