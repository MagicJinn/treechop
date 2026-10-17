package ht.treechop.common;

import ht.treechop.TreeChop;
import ht.treechop.TreeChopException;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.config.ConfigHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class NeoForgeCommon {

    public static void onBreakEvent(BreakBlockEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer agent)) {
            return;
        }

        ItemStack tool = agent.getMainHandItem();
        BlockState blockState = event.getState();
        BlockPos pos = event.getPos();

        try {
            if (ChopUtil.chop(agent, level, pos, blockState, tool, event)) {
                event.setCanceled(true);
            }
        } catch (TreeChopException e) {
            TreeChop.cry(e);
        }
    }

}
