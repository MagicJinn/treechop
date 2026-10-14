package ht.treechop.common.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class AxeAccessor {
    private AxeAccessor() {
    }

    public static boolean isStrippable(Block block) {
        return getStripped(block.defaultBlockState()) != null;
    }

    public static boolean isStripped(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id.getPath().startsWith("stripped_");
    }

    public static BlockState getStripped(BlockState blockState) {
        Block block = blockState.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id.getPath().startsWith("stripped_")) {
            return blockState;
        }
        Identifier strippedId = Identifier.fromNamespaceAndPath(id.getNamespace(), "stripped_" + id.getPath());
        if (!BuiltInRegistries.BLOCK.containsKey(strippedId)) {
            return null;
        }
        Block stripped = BuiltInRegistries.BLOCK.getValue(strippedId);
        return BlockUtil.copyStateProperties(stripped.defaultBlockState(), blockState);
    }
}
