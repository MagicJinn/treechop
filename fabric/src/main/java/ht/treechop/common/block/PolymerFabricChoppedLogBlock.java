package ht.treechop.common.block;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.utils.PolymerClientDecoded;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import ht.treechop.common.properties.ChoppedWood;
import ht.treechop.compat.polymer.ChoppedLogPolymerModel;
import ht.treechop.compat.polymer.TreeChopPolymer;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Polymer stand-in: barrier + BlockDisplay for vanilla clients; raw TreeChop
 * block+model for clients that have the mod.
 */
public class PolymerFabricChoppedLogBlock extends FabricChoppedLogBlock
        implements PolymerBlock, PolymerClientDecoded, BlockWithElementHolder {

    public PolymerFabricChoppedLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean forceLightUpdates(BlockState state) {
        return true;
    }

    @Override
    public boolean forceLightInsideBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean canSyncRawToClient(PacketContext context) {
        return TreeChopPolymer.hasTreeChopClient(context);
    }

    /**
     * Polymer defaults this to {@code true} (server mining). TreeChop clients sync the
     * real block and must mine client-side; otherwise after log→chopped_log the crack
     * freezes (barrier-style server mining against a replaced block).
     * Vanilla clients still need server mining for the barrier stand-in.
     */
    @Override
    public boolean handleMiningOnServer(ItemStack tool, BlockState state, BlockPos pos, ServerPlayer player) {
        return !TreeChopPolymer.hasTreeChopClient(player);
    }

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
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public Block getPolymerReplacement(Block block, PacketContext context) {
        if (TreeChopPolymer.hasTreeChopClient(context)) {
            return this;
        }
        return Blocks.BARRIER;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new ChoppedLogPolymerModel(initialBlockState);
    }
}
