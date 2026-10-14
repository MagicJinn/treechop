package ht.treechop.common.block;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import ht.treechop.TreeChop;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PolymerFabricChoppedLogBlock extends FabricChoppedLogBlock implements PolymerTexturedBlock {
    private static final Identifier VANILLA_CLIENT_MODEL = TreeChop.resource("block/chopped_log_vanilla");
    private final BlockState polymerFallbackState;
    private final BlockState polymerTexturedState;

    public PolymerFabricChoppedLogBlock(Properties properties) {
        super(properties);
        // POLYMER WORKAROUND: borrow a note-block variant for a server-generated blockstate entry in the Polymer pack.
        this.polymerTexturedState = PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(VANILLA_CLIENT_MODEL)
        );
        this.polymerFallbackState = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context) {
        if (polymerTexturedState != null) {
            return polymerTexturedState;
        }
        return polymerFallbackState;
    }
}
