package net.createteleporters.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.integration.ImmersivePortalsIntegration;
import net.createteleporters.init.CreateteleportersModBlocks;

public class CustomPortalBaseBlockDestroyedByPlayerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		BlockPos basePos = BlockPos.containing(x, y, z);
		BlockEntity blockEntity = world.getBlockEntity(basePos);
		CustomPortalBaseOnTickUpdateProcedure.releasePortalChunks(world, basePos);
		
		
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
			
			
			if (!world.isClientSide()) {
				nbt.putBoolean("portalActive", false);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(basePos, world.getBlockState(basePos), world.getBlockState(basePos), 3);
			}
		}

		
		if (CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.get()
				&& ImmersivePortalsIntegration.isImmersivePortalsLoaded()
				&& world instanceof ServerLevel sourceLevel) {
			ImmersivePortalsIntegration.removeImmersivePortal(world, x, y, z);

			if (blockEntity != null) {
				CompoundTag nbt = blockEntity.getPersistentData();
				if (nbt.getBoolean("isLinked")) {
					String linkedDimId = nbt.getString("linkedDim");
					ResourceLocation linkedDimLoc = ResourceLocation.tryParse(linkedDimId);
					if (linkedDimLoc != null) {
						ResourceKey<net.minecraft.world.level.Level> linkedDimKey = ResourceKey
								.create(net.minecraft.core.registries.Registries.DIMENSION, linkedDimLoc);
						ServerLevel linkedLevel = sourceLevel.getServer().getLevel(linkedDimKey);
						if (linkedLevel != null) {
							double linkedX = nbt.getDouble("linkedX");
							double linkedY = nbt.getDouble("linkedY");
							double linkedZ = nbt.getDouble("linkedZ");
							ImmersivePortalsIntegration.removeImmersivePortal(linkedLevel, linkedX, linkedY, linkedZ);
						}
					}
				}
			}
		}
		
		
		if ("east".equals(rotation) || "west".equals(rotation)) {
			world.removeBlock(basePos.offset(0, 0, 1), false);  
			world.removeBlock(basePos.offset(0, 0, -1), false); 
		} else {
			world.removeBlock(basePos.offset(1, 0, 0), false);  
			world.removeBlock(basePos.offset(-1, 0, 0), false); 
		}
		
		
		if (world instanceof ServerLevel _level && portalHeight > 0) {
			int fillHeight = portalHeight - 1; 
			
			if ("east".equals(rotation) || "west".equals(rotation)) {
				
				for (int dy = 1; dy <= fillHeight; dy++) {
					for (int dz = minExtent + 1; dz <= maxExtent - 1; dz++) {
						BlockPos pos = basePos.offset(0, dy, dz);
						if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
							_level.removeBlock(pos, false);
					}
				}
			} else if ("north".equals(rotation) || "south".equals(rotation)) {
				
				for (int dy = 1; dy <= fillHeight; dy++) {
					for (int dx = minExtent + 1; dx <= maxExtent - 1; dx++) {
						BlockPos pos = basePos.offset(dx, dy, 0);
						if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
							_level.removeBlock(pos, false);
					}
				}
			} else {
				
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
