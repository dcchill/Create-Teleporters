package net.createteleporters.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.createteleporters.init.CreateteleportersModBlocks;

public class CustomPortalBaseBlockDestroyedByPlayerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		BlockPos basePos = BlockPos.containing(x, y, z);
		BlockEntity blockEntity = world.getBlockEntity(basePos);
		
		// Get portal dimensions from NBT
		int portalHeight = 0;
		int minExtent = -2;
		int maxExtent = 2;
		String rotation = "north";
		
		if (blockEntity != null) {
			CompoundTag nbt = blockEntity.getPersistentData();
			portalHeight = nbt.getInt("portalHeight");
			minExtent = nbt.getInt("portalMinExtent");
			maxExtent = nbt.getInt("portalMaxExtent");
			rotation = nbt.getString("rotation");
			
			// Deactivate the portal
			if (!world.isClientSide()) {
				nbt.putBoolean("portalActive", false);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(basePos, world.getBlockState(basePos), world.getBlockState(basePos), 3);
			}
		}
		
		// Remove dummy blocks (always at offset +/- 1 from base on bottom row)
		if ("east".equals(rotation) || "west".equals(rotation)) {
			world.removeBlock(basePos.offset(0, 0, 1), false);  // Remove dummy at +1
			world.removeBlock(basePos.offset(0, 0, -1), false); // Remove dummy at -1
		} else {
			world.removeBlock(basePos.offset(1, 0, 0), false);  // Remove dummy at +1
			world.removeBlock(basePos.offset(-1, 0, 0), false); // Remove dummy at -1
		}
		
		// Remove quantum portal blocks using stored dimensions
		if (world instanceof ServerLevel _level && portalHeight > 0) {
			int fillHeight = portalHeight - 1; // Height of portal interior (excludes bottom row)
			
			if ("east".equals(rotation) || "west".equals(rotation)) {
				// Horizontal axis is Z - portal spans along Z axis
				for (int dy = 1; dy <= fillHeight; dy++) {
					for (int dz = minExtent + 1; dz <= maxExtent - 1; dz++) {
						BlockPos pos = basePos.offset(0, dy, dz);
						if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
							_level.removeBlock(pos, false);
					}
				}
			} else if ("north".equals(rotation) || "south".equals(rotation)) {
				// Vertical axis is X - portal spans along X axis
				for (int dy = 1; dy <= fillHeight; dy++) {
					for (int dx = minExtent + 1; dx <= maxExtent - 1; dx++) {
						BlockPos pos = basePos.offset(dx, dy, 0);
						if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
							_level.removeBlock(pos, false);
					}
				}
			} else {
				// Fallback to old 3x3 removal if no dimensions stored
				for (int dy = 1; dy <= 3; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos pos = basePos.offset(0, dy, dz);
						if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
							_level.removeBlock(pos, false);
					}
				}
				for (int dy = 1; dy <= 3; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						BlockPos pos = basePos.offset(dx, dy, 0);
						if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
							_level.removeBlock(pos, false);
					}
				}
			}
		}
	}
}
