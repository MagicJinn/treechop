package ht.treechop.common.block;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.utils.PolymerClientDecoded;
import ht.treechop.common.properties.ChoppedWood;
import ht.treechop.compat.polymer.PolymerChoppedLogModels;
import ht.treechop.compat.polymer.TreeChopPolymer;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Polymer stand-in for vanilla clients (sized cubes by wood+radius) and raw
 * TreeChop block+model for clients that have the mod.
 */
public class PolymerFabricChoppedLogBlock extends FabricChoppedLogBlock implements PolymerTexturedBlock, PolymerClientDecoded {
    private final BlockState polymerFallbackState;
    private final Map<ChoppedWood, BlockState[]> polymerByWood = new EnumMap<>(ChoppedWood.class);
    private final Map<ChoppedWood, BlockState[]> polymerByWoodWaterlogged = new EnumMap<>(ChoppedWood.class);

    public PolymerFabricChoppedLogBlock(Properties properties) {
        super(properties);
        this.polymerFallbackState = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        PolymerChoppedLogModels.ensureRegistered();

        // Leaves-type, not FULL_BLOCK: full noteblocks make neighbors cull faces, which
        // x-rays through the gaps around a thinner chopped log.
        int[] radii = PolymerChoppedLogModels.RADIUS_BUCKETS;
        for (ChoppedWood wood : ChoppedWood.values()) {
            BlockState[] dry = new BlockState[radii.length];
            BlockState[] wet = new BlockState[radii.length];
            for (int i = 0; i < radii.length; i++) {
                int radius = radii[i];
                var model = PolymerBlockModel.of(PolymerChoppedLogModels.modelId(wood, radius));
                dry[i] = PolymerBlockResourceUtils.requestBlock(BlockModelType.LEAVES, model);
                wet[i] = PolymerBlockResourceUtils.requestBlock(BlockModelType.LEAVES_WATERLOGGED, model);
            }
            polymerByWood.put(wood, dry);
            polymerByWoodWaterlogged.put(wood, wet);
        }
    }

    @Override
    public boolean forceLightUpdates(BlockState state) {
        return true;
    }

    @Override
    public boolean canSyncRawToClient(PacketContext context) {
        return TreeChopPolymer.hasTreeChopClient(context);
    }

    /**
     * Break FX use this state (then Polymer maps it for the client). Returning a real
     * stripped log keeps wood break sounds/particles instead of the leaves stand-in.
     */
    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        if (TreeChopPolymer.hasTreeChopClient(context)) {
            return state;
        }
        ChoppedWood wood = state.hasProperty(WOOD) ? state.getValue(WOOD) : ChoppedWood.OAK;
        return wood.polymerBreakBlock().defaultBlockState();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context) {
        if (TreeChopPolymer.hasTreeChopClient(context)) {
            return state;
        }

        ChoppedWood wood = state.hasProperty(WOOD) ? state.getValue(WOOD) : ChoppedWood.OAK;
        int radius = state.hasProperty(RADIUS) ? state.getValue(RADIUS) : DEFAULT_UNCHOPPED_RADIUS;
        boolean waterlogged = state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED);
        BlockState[] table = waterlogged ? polymerByWoodWaterlogged.get(wood) : polymerByWood.get(wood);
        if (table == null) {
            table = polymerByWood.get(ChoppedWood.OAK);
        }
        BlockState polymerState = table[bucketIndex(radius)];
        if (polymerState != null) {
            return polymerState;
        }
        // Fall back across waterlogged / oak if a request failed (budget exhausted).
        polymerState = polymerByWood.get(ChoppedWood.OAK)[bucketIndex(radius)];
        return polymerState != null ? polymerState : polymerFallbackState;
    }

    @Override
    public Block getPolymerReplacement(Block block, PacketContext context) {
        if (TreeChopPolymer.hasTreeChopClient(context)) {
            return this;
        }
        BlockState sample = polymerByWood.get(ChoppedWood.OAK)[bucketIndex(DEFAULT_UNCHOPPED_RADIUS)];
        return (sample != null ? sample : polymerFallbackState).getBlock();
    }

    private static int bucketIndex(int radius) {
        radius = Mth.clamp(radius, 1, 8);
        int[] radii = PolymerChoppedLogModels.RADIUS_BUCKETS;
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < radii.length; i++) {
            int dist = Math.abs(radii[i] - radius);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }
}
