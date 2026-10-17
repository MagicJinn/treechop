package ht.treechop.common.properties;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Vanilla wood kinds used for Polymer stand-in models (and blockstate sync).
 * Textures are resolved from the block registry (stripped log/stem, else mushroom pores, else oak).
 * Modded woods still fall back to {@link #OAK} for the {@code WOOD} property.
 */
public enum ChoppedWood implements StringRepresentable {
    OAK("oak"),
    SPRUCE("spruce"),
    BIRCH("birch"),
    JUNGLE("jungle"),
    ACACIA("acacia"),
    DARK_OAK("dark_oak"),
    MANGROVE("mangrove"),
    CHERRY("cherry"),
    PALE_OAK("pale_oak"),
    CRIMSON("crimson"),
    WARPED("warped"),
    MUSHROOM("mushroom");

    private final String name;
    private volatile Resolved resolved;

    ChoppedWood(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String sideTexture() {
        return resolve().sideTexture;
    }

    public String endTexture() {
        return resolve().endTexture;
    }

    /** Vanilla block used for Polymer break particles/sounds. */
    public Block polymerBreakBlock() {
        return resolve().breakBlock;
    }

    private Resolved resolve() {
        Resolved cached = resolved;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (resolved == null) {
                resolved = Resolved.forWoodName(name);
            }
            return resolved;
        }
    }

    public static ChoppedWood fromBlock(Block block) {
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (path.startsWith("stripped_")) {
            path = path.substring("stripped_".length());
        }
        path = path
                .replace("_log", "")
                .replace("_wood", "")
                .replace("_stem", "")
                .replace("_hyphae", "");
        for (ChoppedWood wood : values()) {
            if (wood.name.equals(path)) {
                return wood;
            }
        }
        return OAK;
    }

    private record Resolved(String sideTexture, String endTexture, Block breakBlock) {
        private static final Identifier MUSHROOM_STEM_ID = Identifier.withDefaultNamespace("mushroom_stem");
        private static final String MUSHROOM_INSIDE = "minecraft:block/mushroom_block_inside";

        static Resolved forWoodName(String name) {
            Block strippedLog = find("stripped_" + name + "_log");
            Block unstrippedLog = find(name + "_log");
            if (strippedLog != null && unstrippedLog != null) {
                return fromStrippedColumn(strippedLog);
            }

            Block strippedStem = find("stripped_" + name + "_stem");
            Block unstrippedStem = find(name + "_stem");
            if (strippedStem != null && unstrippedStem != null) {
                return fromStrippedColumn(strippedStem);
            }

            // No stripped variant: mushroom stem uses the pores/"inside" texture.
            if (BuiltInRegistries.BLOCK.containsKey(MUSHROOM_STEM_ID)
                    && (name.equals("mushroom") || unstrippedStem != null && strippedStem == null)) {
                return new Resolved(MUSHROOM_INSIDE, MUSHROOM_INSIDE, BuiltInRegistries.BLOCK.getValue(MUSHROOM_STEM_ID));
            }

            return fromStrippedColumn(Blocks.STRIPPED_OAK_LOG);
        }

        private static Resolved fromStrippedColumn(Block stripped) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(stripped);
            String base = id.getNamespace() + ":block/" + id.getPath();
            return new Resolved(base, base + "_top", stripped);
        }

        private static Block find(String path) {
            Identifier id = Identifier.withDefaultNamespace(path);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                return null;
            }
            return BuiltInRegistries.BLOCK.getValue(id);
        }
    }
}
