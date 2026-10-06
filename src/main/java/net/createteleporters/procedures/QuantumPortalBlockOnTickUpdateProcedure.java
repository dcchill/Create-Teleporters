package net.createteleporters.procedures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.createteleporters.init.CreateteleportersModBlocks;

public class QuantumPortalBlockOnTickUpdateProcedure {
	private static final int MAX_PORTAL_OFFSET = 23;

	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos portalPos = BlockPos.containing(x, y, z);

		if (!isInsideValidPortal(world, portalPos)) {
			if (!world.isClientSide()) {
				world.setBlock(portalPos, Blocks.AIR.defaultBlockState(), 3);
			}
			return;
		}

		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.PORTAL, x, y, z, 2, 1, 1, 1, 0.1);
	}

	private static boolean isInsideValidPortal(LevelAccessor world, BlockPos portalPos) {
		for (int yOffset = 1; yOffset < MAX_PORTAL_OFFSET; yOffset++) {
			int baseY = portalPos.getY() - yOffset;
			for (int horizontalOffset = -MAX_PORTAL_OFFSET; horizontalOffset <= MAX_PORTAL_OFFSET; horizontalOffset++) {
				if (isValidForCandidateBase(world, portalPos, new BlockPos(portalPos.getX() - horizontalOffset, baseY, portalPos.getZ()))) {
					return true;
				}
				if (isValidForCandidateBase(world, portalPos, new BlockPos(portalPos.getX(), baseY, portalPos.getZ() - horizontalOffset))) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isValidForCandidateBase(LevelAccessor world, BlockPos portalPos, BlockPos basePos) {
		if (world.getBlockState(basePos).getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get()) {
			return false;
		}

		BlockEntity blockEntity = world.getBlockEntity(basePos);
		if (blockEntity == null) {
			return false;
		}

		CompoundTag nbt = blockEntity.getPersistentData();
		if (!nbt.getBoolean("portalActive") || !containsStoredPortalDimensions(nbt)) {
			return false;
		}

		String rotation = nbt.getString("rotation");
		// The controller validates the whole frame every tick; interiors only check ownership.
		return isPortalInteriorBlock(basePos, portalPos, nbt, rotation);
	}

	private static boolean containsStoredPortalDimensions(CompoundTag nbt) {
		return nbt.contains("portalHeight") && nbt.contains("portalMinExtent") && nbt.contains("portalMaxExtent");
	}

	private static boolean isPortalInteriorBlock(BlockPos basePos, BlockPos portalPos, CompoundTag nbt, String rotation) {
		int dy = portalPos.getY() - basePos.getY();
		int portalHeight = nbt.getInt("portalHeight");
		int interiorMin = nbt.getInt("portalMinExtent") + 1;
		int interiorMax = nbt.getInt("portalMaxExtent") - 1;

		if (dy < 1 || dy > portalHeight - 1) {
			return false;
		}

		if (isEastWest(rotation)) {
			int dz = portalPos.getZ() - basePos.getZ();
			return portalPos.getX() == basePos.getX() && dz >= interiorMin && dz <= interiorMax;
		}

		if (isNorthSouth(rotation)) {
			int dx = portalPos.getX() - basePos.getX();
			return portalPos.getZ() == basePos.getZ() && dx >= interiorMin && dx <= interiorMax;
		}

		return false;
	}

	private static boolean isEastWest(String rotation) {
		return "east".equals(rotation) || "west".equals(rotation);
	}

	private static boolean isNorthSouth(String rotation) {
		return "north".equals(rotation) || "south".equals(rotation);
	}
}
