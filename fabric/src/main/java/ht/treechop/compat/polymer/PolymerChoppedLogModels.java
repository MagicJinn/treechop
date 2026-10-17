package ht.treechop.compat.polymer;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import ht.treechop.TreeChop;
import ht.treechop.common.properties.ChoppedWood;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Emits Polymer stand-in block models when the server resource pack is built.
 * Keeps wood×radius JSON out of source and makes adding woods (e.g. mushroom) one enum entry.
 */
public final class PolymerChoppedLogModels {
    public static final int[] RADIUS_BUCKETS = {1, 3, 5, 7};

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private PolymerChoppedLogModels() {
    }

    public static void ensureRegistered() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            for (ChoppedWood wood : ChoppedWood.values()) {
                for (int radius : RADIUS_BUCKETS) {
                    Identifier id = modelId(wood, radius);
                    String path = "assets/" + id.getNamespace() + "/models/" + id.getPath() + ".json";
                    builder.addData(path, modelJson(wood, radius).getBytes(StandardCharsets.UTF_8));
                }
            }
        });
    }

    public static Identifier modelId(ChoppedWood wood, int radius) {
        return TreeChop.resource("block/chopped_log_" + wood.getSerializedName() + "_r" + radius);
    }

    private static String modelJson(ChoppedWood wood, int radius) {
        int from = 8 - radius;
        int to = 8 + radius;
        String side = wood.sideTexture();
        String end = wood.endTexture();
        return """
                {
                  "parent": "minecraft:block/block",
                  "textures": {
                    "particle": "%s",
                    "end": "%s",
                    "side": "%s"
                  },
                  "elements": [
                    {
                      "from": [%d, 0, %d],
                      "to": [%d, 16, %d],
                      "faces": {
                        "down":  { "texture": "#end" },
                        "up":    { "texture": "#end" },
                        "north": { "texture": "#side" },
                        "south": { "texture": "#side" },
                        "west":  { "texture": "#side" },
                        "east":  { "texture": "#side" }
                      }
                    }
                  ]
                }
                """.formatted(side, end, side, from, from, to, to);
    }
}
