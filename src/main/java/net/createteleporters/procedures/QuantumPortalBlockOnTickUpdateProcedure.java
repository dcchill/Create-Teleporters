package net.createteleporters.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.createteleporters.block.QuantumPortalBlockBlock;

public class QuantumPortalBlockOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos portalPos = BlockPos.containing(x, y, z);

		if (!isInsideValidPortal(world, portalPos)) {
			if (!world.isClientSide()) {
				world.setBlock(portalPos, Blocks.AIR.defaultBlockState(), 3);
			}
			return;
		}

		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 1, 1, 1, 0.1);
	}

	private static boolean isInsideValidPortal(LevelAccessor world, BlockPos portalPos) {
		BlockPos basePos = QuantumPortalBlockBlock.findPortalBase(world, portalPos);
		if (basePos == null) return false;
		BlockEntity controller = world.getBlockEntity(basePos);
		return controller != null && controller.getPersistentData().getBoolean("portalActive");
	}
}
