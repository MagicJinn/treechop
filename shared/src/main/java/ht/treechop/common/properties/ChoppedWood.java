package ht.treechop.common.properties;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finite vanilla wood kinds for the {@code WOOD} blockstate property.
 * <p>
 * Exists because Polymer (and blockstate JSON) need the wood kind on the blockstate itself:
 * vanilla clients do not get the chopped-log BE, so a tag like {@code #logs} cannot carry
 * "which wood is this" across the wire. {@link net.minecraft.world.level.block.state.properties.EnumProperty}
 * requires a fixed enum known at registration time.
 * <p>
 * Textures resolve from the block registry (stripped log/stem, else mushroom pores, else oak).
 * Modded woods still fall back to {@link #OAK}; TreeChop clients use the BE original instead.
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
    POPLAR("poplar"),
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

    /**
     * Best-effort unstripped column for this wood. Used to seed client BEs from the
     * {@code WOOD} blockstate before the real original-state packet arrives.
     */
    public BlockState fallbackOriginalState() {
        return resolve().originalBlock.defaultBlockState();
    }

    /**
     * BlockState shown on Polymer BlockDisplays. Mushroom stems use all-pores faces
     * (no stripped block); other woods use the stripped column default.
     */
    public BlockState polymerDisplayBlockState() {
        BlockState state = polymerBreakBlock().defaultBlockState();
        if (this == MUSHROOM) {
            for (var property : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
                if (state.hasProperty(property)) {
                    state = state.setValue(property, false);
                }
            }
        }
        return state;
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

    private record Resolved(String sideTexture, String endTexture, Block breakBlock, Block originalBlock) {
        private static final Identifier MUSHROOM_STEM_ID = Identifier.withDefaultNamespace("mushroom_stem");
        private static final String MUSHROOM_INSIDE = "minecraft:block/mushroom_block_inside";

        static Resolved forWoodName(String name) {
            Block strippedLog = find("stripped_" + name + "_log");
            Block unstrippedLog = find(name + "_log");
            if (strippedLog != null && unstrippedLog != null) {
                return fromStrippedColumn(strippedLog, unstrippedLog);
            }

            Block strippedStem = find("stripped_" + name + "_stem");
            Block unstrippedStem = find(name + "_stem");
            if (strippedStem != null && unstrippedStem != null) {
                return fromStrippedColumn(strippedStem, unstrippedStem);
            }

            // No stripped variant: mushroom stem uses the pores/"inside" texture.
            if (BuiltInRegistries.BLOCK.containsKey(MUSHROOM_STEM_ID)
                    && (name.equals("mushroom") || unstrippedStem != null && strippedStem == null)) {
                Block stem = BuiltInRegistries.BLOCK.getValue(MUSHROOM_STEM_ID);
                return new Resolved(MUSHROOM_INSIDE, MUSHROOM_INSIDE, stem, stem);
            }

            return fromStrippedColumn(Blocks.STRIPPED_OAK_LOG, Blocks.OAK_LOG);
        }

        private static Resolved fromStrippedColumn(Block stripped, Block original) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(stripped);
            String base = id.getNamespace() + ":block/" + id.getPath();
            return new Resolved(base, base + "_top", stripped, original);
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
