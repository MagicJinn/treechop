package ht.treechop.compat.polymer;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import ht.treechop.TreeChop;
import ht.treechop.common.block.PolymerFabricChoppedLogBlock;
import ht.treechop.common.loot.CountBlockChopsLootItemCondition;
import ht.treechop.common.loot.TreeChopLootContextParams;
import ht.treechop.common.loot.TreeFelledLootItemCondition;
import ht.treechop.common.network.ServerUpdateChopsPacket;
import ht.treechop.common.registry.FabricModBlocks;
import ht.treechop.server.FabricServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

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

        // Hide BE from vanilla clients; keep it for TreeChop clients (needed for thinning model).
        PolymerBlockUtils.registerBlockEntity(FabricModBlocks.CHOPPED_LOG_ENTITY, new PolymerSyncedObject<>() {
            @Override
            public BlockEntityType<?> getPolymerReplacement(BlockEntityType<?> type, PacketContext context) {
                return null;
            }

            @Override
            public boolean canSyncRawToClient(PacketContext context) {
                return hasTreeChopClient(context);
            }
        });

        // Barrier stand-ins: server mining FX for vanilla clients (TC clients mine locally).
        PolymerBlockUtils.BREAKING_PROGRESS_UPDATE.register((player, pos, state, progress) -> {
            if (state.getBlock() instanceof PolymerFabricChoppedLogBlock) {
                ChoppedLogPolymerParticles.spawnMining(player, pos, state, progress);
            }
        });

        // Hide TreeChop-only registry ids from vanilla Fabric registry sync.
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.SOUND_EVENT, TreeChop.CHOP_WOOD_EVENT.get());
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.LOOT_CONDITION_TYPE, CountBlockChopsLootItemCondition.ID);
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.LOOT_CONDITION_TYPE, TreeFelledLootItemCondition.ID);
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.CONTEXT_KEY_SET, TreeChopLootContextParams.SET_ID);

        FabricServer.setPacketFilter(TreeChopPolymer::shouldSendPacket);
        TreeChop.LOGGER.info("Polymer compatibility enabled for Fabric (server-side vanilla clients).");
    }

    /** True when the receiver has TreeChop networking (modded client). */
    public static boolean hasTreeChopClient(@Nullable PacketContext context) {
        if (context == null) {
            return false;
        }
        return hasTreeChopClient(PolymerCommonUtils.getPlayer(context));
    }

    public static boolean hasTreeChopClient(@Nullable ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return ServerPlayNetworking.canSend(player, ServerUpdateChopsPacket.TYPE);
    }

    public static boolean shouldSendPacket(ServerPlayer player, CustomPacketPayload payload) {
        if (player == null) {
            return true;
        }
        return ServerPlayNetworking.canSend(player, payload.type());
    }
}
