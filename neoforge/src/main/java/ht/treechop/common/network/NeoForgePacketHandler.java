package ht.treechop.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgePacketHandler {

    private NeoForgePacketHandler() {}

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("treechop");

        registrar.playToServer(
                ClientRequestSettingsPacket.TYPE,
                ClientRequestSettingsPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> payload.handle(context.player(), context::reply)
                )
        );

        registrar.playToClient(
                ServerConfirmSettingsPacket.TYPE,
                ServerConfirmSettingsPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handle)
        );

        registrar.playToClient(
                ServerPermissionsPacket.TYPE,
                ServerPermissionsPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handle)
        );

        registrar.playToClient(
                ServerUpdateChopsPacket.TYPE,
                ServerUpdateChopsPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handle)
        );
    }
}
