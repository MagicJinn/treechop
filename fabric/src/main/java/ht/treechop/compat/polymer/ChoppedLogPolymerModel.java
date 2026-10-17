package ht.treechop.compat.polymer;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.properties.ChoppedWood;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Virtual BlockDisplay for vanilla Polymer clients: scaled stripped log over a barrier.
 * Mining chips are spawned from {@link ChoppedLogPolymerParticles} while breaking only.
 */
public class ChoppedLogPolymerModel extends ElementHolder {
    private final BlockDisplayElement display = new BlockDisplayElement();

    public ChoppedLogPolymerModel(BlockState initialState) {
        this.display.setInterpolationDuration(0);
        this.display.setTeleportDuration(0);
        this.addElement(this.display);
        apply(initialState);
    }

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        if (TreeChopPolymer.hasTreeChopClient(player.getPlayer())) {
            return false;
        }
        return super.startWatching(player);
    }

    @Override
    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE
                && getAttachment() instanceof BlockAwareAttachment attachment) {
            apply(attachment.getBlockState());
            if (getAttachment() != null) {
                tick();
            }
        }
    }

    private void apply(BlockState state) {
        ChoppedWood wood = state.hasProperty(ChoppedLogBlock.WOOD)
                ? state.getValue(ChoppedLogBlock.WOOD)
                : ChoppedWood.OAK;
        int radius = state.hasProperty(ChoppedLogBlock.RADIUS)
                ? state.getValue(ChoppedLogBlock.RADIUS)
                : ChoppedLogBlock.DEFAULT_UNCHOPPED_RADIUS;
        radius = Mth.clamp(radius, 1, 8);

        float scaleXZ = radius / 8f;
        float inset = (1f - scaleXZ) / 2f;
        // Attachment is at block center; BlockDisplay's origin is the min corner.
        this.display.setBlockState(wood.polymerDisplayBlockState());
        this.display.setScale(new Vector3f(scaleXZ, 1f, scaleXZ));
        this.display.setTranslation(new Vector3f(-0.5f + inset, -0.5f, -0.5f + inset));
    }
}
