package ht.treechop.compat.polymer;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.properties.ChoppedWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mining chips for Polymer barrier clients only. TreeChop clients sync the real
 * chopped-log block and play normal client-side hit sounds/particles themselves.
 */
public final class ChoppedLogPolymerParticles {
    private ChoppedLogPolymerParticles() {
    }

    public static BlockParticleOption particleFor(BlockState choppedState) {
        ChoppedWood wood = choppedState.hasProperty(ChoppedLogBlock.WOOD)
                ? choppedState.getValue(ChoppedLogBlock.WOOD)
                : ChoppedWood.OAK;
        return new BlockParticleOption(ParticleTypes.BLOCK, wood.polymerDisplayBlockState());
    }

    /**
     * Continuous chips while a vanilla client is mining the barrier stand-in.
     * Sounds come from Polymer server mining / break-event state — do not play extra hits here.
     * {@code progress < 0} means cancelled/finished — do not spawn.
     */
    public static void spawnMining(ServerPlayer miner, BlockPos pos, BlockState choppedState, int progress) {
        if (progress < 0 || TreeChopPolymer.hasTreeChopClient(miner)) {
            return;
        }
        ServerLevel level = miner.level();
        BlockParticleOption particle = particleFor(choppedState);
        RandomSource random = level.getRandom();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        for (int i = 0; i < 4; i++) {
            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = random.nextDouble() * 0.35;
            double vz = (random.nextDouble() - 0.5) * 0.5;
            sendNear(level, particle,
                    cx + vx * 0.4, cy + (random.nextDouble() - 0.5) * 0.6, cz + vz * 0.4,
                    vx, vy, vz);
        }
    }

    private static ClientboundLevelParticlesPacket packet(
            BlockParticleOption particle, double x, double y, double z, double vx, double vy, double vz) {
        return new ClientboundLevelParticlesPacket(particle, false, false, x, y, z, (float) vx, (float) vy, (float) vz, 1f, 0);
    }

    private static void sendNear(
            ServerLevel level, BlockParticleOption particle,
            double x, double y, double z, double vx, double vy, double vz) {
        ClientboundLevelParticlesPacket packet = packet(particle, x, y, z, vx, vy, vz);
        for (ServerPlayer player : level.players()) {
            if (TreeChopPolymer.hasTreeChopClient(player)) {
                continue;
            }
            if (player.distanceToSqr(x, y, z) > 64 * 64) {
                continue;
            }
            player.connection.send(packet);
        }
    }
}
