package ht.treechop.mixin;

import ht.treechop.TreeChopException;
import ht.treechop.api.TreeData;
import ht.treechop.client.Client;
import ht.treechop.client.gui.screen.ChopIndicator;
import ht.treechop.common.chop.ChopUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chops cancel client-side block removal. Vanilla then leaves {@code isDestroying=false}
 * with a post-break delay, so continuous mining never sends a fresh START (no hit FX)
 * and releasing the mouse never clears the crack overlay. Force a remine session after
 * each non-fell chop prediction.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private float destroyProgress;
    @Shadow private float destroyTicks;
    @Shadow private int destroyDelay;
    @Shadow private boolean isDestroying;

    @Unique
    private boolean treechop$remineAfterChop;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    public void dontPredictBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        try {
            Player player = minecraft.player;
            Level level = minecraft.level;
            if (player != null && level != null) {
                if (ChopUtil.playerWantsToChop(player, Client.getChopSettings()) && ChopIndicator.blockCanBeChopped(pos)) {
                    TreeData tree = Client.treeCache.getTree(level, pos);
                    BlockState state = level.getBlockState(pos);
                    boolean felled = tree.readyToFell(tree.getChops() + ChopUtil.getNumChopsByTool(player.getMainHandItem(), state));

                    if (!felled) {
                        ChopUtil.thwack(player, level, pos, state);
                        this.treechop$remineAfterChop = true;
                        info.setReturnValue(false);
                    }
                }
            }
        } catch (TreeChopException e) {
            // Ignore
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void treechop$remineAfterChop(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> info) {
        if (!this.treechop$remineAfterChop) {
            return;
        }
        this.treechop$remineAfterChop = false;

        // Invalidate sameDestroyTarget so the next continue calls startDestroyBlock.
        this.isDestroying = false;
        this.destroyProgress = 0.0F;
        this.destroyTicks = 0.0F;
        this.destroyDelay = 0;
        this.destroyBlockPos = new BlockPos(-1, -1, -1);

        if (this.minecraft.player != null && this.minecraft.level != null) {
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), pos, -1);
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void treechop$clearStuckCrack(CallbackInfo info) {
        // After a chop cancel, isDestroying is false so vanilla skips clearing the overlay.
        if (!this.isDestroying && this.minecraft.player != null && this.minecraft.level != null) {
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
        }
    }
}
