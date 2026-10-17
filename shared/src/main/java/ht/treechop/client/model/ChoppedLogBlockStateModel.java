package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.treechop.common.properties.ChoppedWood;
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
import net.minecraft.world.level.block.Blocks;
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

    /**
     * Prefer the synced {@code WOOD} property when the BE still has the default oak
     * original (NBT not arrived yet). Once the BE has a real original, use that
     * (needed for modded woods that map to {@link ChoppedWood#OAK}).
     */
    protected static BlockState visualLogState(BlockState blockState, @Nullable ChoppedLogBlock.MyEntity entity) {
        BlockState original = entity != null ? entity.getOriginalState() : null;
        boolean beLooksDefault = original == null || original.is(Blocks.OAK_LOG);
        if (blockState.hasProperty(ChoppedLogBlock.WOOD)) {
            ChoppedWood wood = blockState.getValue(ChoppedLogBlock.WOOD);
            if (wood != ChoppedWood.OAK && beLooksDefault) {
                return wood.fallbackOriginalState();
            }
        }
        return original != null ? original : Blocks.OAK_LOG.defaultBlockState();
    }

    protected static int visualRadius(BlockState blockState, @Nullable ChoppedLogBlock.MyEntity entity) {
        if (blockState.hasProperty(ChoppedLogBlock.RADIUS)) {
            return blockState.getValue(ChoppedLogBlock.RADIUS);
        }
        return entity != null ? entity.getRadius() : ChoppedLogBlock.DEFAULT_UNCHOPPED_RADIUS;
    }

    protected static ChoppedLogShape visualShape(@Nullable ChoppedLogBlock.MyEntity entity) {
        return entity != null ? entity.getShape() : ChoppedLogShape.PILLAR_Y;
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
        final Direction[] allDirections = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, null};
        AABB box = shape.getBoundingBox(radius);
        Vector3 mins = new Vector3(box.minX, box.minY, box.minZ);
        Vector3 maxes = new Vector3(box.maxX, box.maxY, box.maxZ);

        return Stream.concat(
                Arrays.stream(allDirections)
                        .flatMap(side -> getBlockQuads(strippedState, side, random).stream()
                                .map(quad -> ModelUtil.trimQuad(quad, mins, maxes))),
                strippedNeighbors.entrySet().stream().flatMap(entry -> {
                    Direction side = entry.getKey();
                    BlockState strippedNeighbor = entry.getValue();
                    Vec3i normal = side.getUnitVec3i().multiply(16);
                    Vector3 transform = new Vector3(normal.getX(), normal.getY(), normal.getZ());
                    return getBlockQuads(strippedNeighbor, side.getOpposite(), random).stream()
                            .map(quad -> ModelUtil.translateQuad(quad, transform));
                })
        ).filter(Objects::nonNull);
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
