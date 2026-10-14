package ht.treechop.compat.polymer;

import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import ht.treechop.TreeChop;
import ht.treechop.common.loot.CountBlockChopsLootItemCondition;
import ht.treechop.common.loot.TreeFelledLootItemCondition;
import ht.treechop.common.registry.FabricModBlocks;
import ht.treechop.server.FabricServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;

public final class TreeChopPolymer {
    private TreeChopPolymer() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("polymer-bundled") && !FabricLoader.getInstance().isModLoaded("polymer-core")) {
            TreeChop.LOGGER.info("Polymer not present; vanilla clients require TreeChop on the client.");
            return;
        }

        PolymerResourcePackUtils.markAsRequired();
        PolymerResourcePackUtils.addModAssets(TreeChop.MOD_ID);

        // Server-only registry entries (vanilla clients never see these ids during sync).
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.BLOCK_ENTITY_TYPE, FabricModBlocks.CHOPPED_LOG_ENTITY);
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.SOUND_EVENT, TreeChop.CHOP_WOOD_EVENT.get());
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.LOOT_CONDITION_TYPE, CountBlockChopsLootItemCondition.ID);
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.LOOT_CONDITION_TYPE, TreeFelledLootItemCondition.ID);

        FabricServer.setPacketFilter(TreeChopPolymer::shouldSendPacket);
        TreeChop.LOGGER.info("Polymer compatibility enabled for Fabric (server-side vanilla clients).");
    }

    public static boolean shouldSendPacket(ServerPlayer player, CustomPacketPayload payload) {
        if (player == null) {
            return true;
        }
        return ServerPlayNetworking.canSend(player, payload.type());
    }
}
