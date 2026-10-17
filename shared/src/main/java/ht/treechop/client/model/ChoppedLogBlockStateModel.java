package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.tuber.math.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ChoppedLogBlockStateModel implements BlockStateModel {
    public static Material.Baked defaultParticleMaterial;
    protected static final Identifier DEFAULT_TEXTURE_RESOURCE = Identifier.withDefaultNamespace("block/stripped_oak_log");

    private static BlockStateModel getBlockModel(BlockState blockState) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
    }

    private static BlockState getStrippedNeighbor(BlockAndTintGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        return ChopUtil.getStrippedState(level, pos, level.getBlockState(neighborPos));
    }

    protected static Map<Direction, BlockState> getStrippedNeighbors(BlockAndTintGetter level, BlockPos pos, ChoppedLogBlock.MyEntity entity) {
        if (entity.getOriginalState().isSolidRender()) {
            return entity.streamSolidSides(level, pos).collect(Collectors.toMap(
                    side -> side,
                    side -> getStrippedNeighbor(level, pos, side)
            ));
        } else {
            return Collections.emptyMap();
        }
    }

    protected List<BakedQuad> getBlockQuads(BlockState blockState, @Nullable Direction side, RandomSource rand) {
        BlockStateModel model = getBlockModel(blockState);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(rand, parts);
        return parts.stream().flatMap(part -> part.getQuads(side).stream()).toList();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        parts.add(new EmptyBlockStateModelPart());
    }

    @Override
    public @NotNull Material.Baked particleMaterial() {
        return defaultParticleMaterial != null ? defaultParticleMaterial : getBlockModel(net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()).particleMaterial();
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    protected Stream<BakedQuad> getQuads(BlockState strippedState, ChoppedLogShape shape, int radius, RandomSource random, Map<Direction, BlockState> strippedNeighbors) {
        // 26.3 block models use BlockStateModel parts; full per-shape trimming is not ported yet.
        List<BakedQuad> quads = new ArrayList<>(getBlockQuads(strippedState, null, random));
        for (BlockState neighborState : strippedNeighbors.values()) {
            quads.addAll(getBlockQuads(neighborState, null, random));
        }
        return quads.stream();
    }

    private static final class EmptyBlockStateModelPart implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            return List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return ChoppedLogBlockStateModel.defaultParticleMaterial;
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}
