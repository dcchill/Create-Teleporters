package net.createteleporters.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.createteleporters.init.CreateteleportersModBlocks;

/**
 * Checks for a valid scalable custom portal frame.
 *
 * Portal structure:
 * - Bottom row: quantum_casing(s) - dummy - base - dummy - quantum_casing(s)
 * - Side rows: quantum casing on both ends, air/portal interior
 * - Top row: all quantum casing
 *
 * Minimum size: 5 wide x 5 tall (3x3 interior)
 * Maximum size: 23 wide x 23 tall (21x21 interior)
 * The base can be anywhere on the bottom row (must have dummy on each side)
 *
 * Note: Portal frames must be perfect squares.
 */
public class ScalablePortalCheckerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		String rotation = getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation");
		boolean isHorizontal = "east".equals(rotation) || "west".equals(rotation);
		boolean isVertical = "north".equals(rotation) || "south".equals(rotation);

		if (!isHorizontal && !isVertical) {
			storeErrorReason(world, BlockPos.containing(x, y, z), "Incorrect Portal Frame");
			setPortalActive(world, BlockPos.containing(x, y, z), false);
			return;
		}

		
		if (256 > drainTankSimulate(world, BlockPos.containing(x, y, z), 256, null)) {
			storeErrorReason(world, BlockPos.containing(x, y, z), "Incorrect Portal Frame");
			setPortalActive(world, BlockPos.containing(x, y, z), false);
			return;
		}

		
		PortalDimensions dims = findValidPortalDimensions(world, BlockPos.containing(x, y, z), isHorizontal);

		if (dims != null) {
			
			storePortalDimensions(world, BlockPos.containing(x, y, z), dims.width, dims.height, dims.minExtent, dims.maxExtent);
			storeErrorReason(world, BlockPos.containing(x, y, z), ""); 
			setPortalActive(world, BlockPos.containing(x, y, z), true);
		} else {
			setPortalActive(world, BlockPos.containing(x, y, z), false);
		}
	}

	/**
	 * Finds valid portal dimensions by scanning the frame structure.
	 * Structure: qqqqq / q   q / q   q / q   q / qdcdq
	 * Where q=quantum casing, d=dummy, c=base
	 */
	private static PortalDimensions findValidPortalDimensions(LevelAccessor world, BlockPos basePos, boolean horizontal) {
		
		BlockPos leftDummyPos = horizontal ? basePos.offset(0, 0, -1) : basePos.offset(-1, 0, 0);
		BlockPos rightDummyPos = horizontal ? basePos.offset(0, 0, 1) : basePos.offset(1, 0, 0);

		if (!isDummyBlock(world, leftDummyPos) || !isDummyBlock(world, rightDummyPos)) {
			storeErrorReason(world, basePos, "Incorrect Portal Frame");
			return null; 
		}

		
		int minExtent = -1; 
		for (int i = 2; i <= 23; i++) {
			BlockPos checkPos = horizontal ? basePos.offset(0, 0, -i) : basePos.offset(-i, 0, 0);
			if (world.getBlockState(checkPos).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
				minExtent = -i;
			} else {
				break;
			}
		}

		
		int maxExtent = 1; 
		for (int i = 2; i <= 23; i++) {
			BlockPos checkPos = horizontal ? basePos.offset(0, 0, i) : basePos.offset(i, 0, 0);
			if (world.getBlockState(checkPos).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
				maxExtent = i;
			} else {
				break;
			}
		}

		
		int totalWidth = maxExtent - minExtent + 1;
		if (totalWidth < 5 || totalWidth > 23) {
			storeErrorReason(world, basePos, "Incorrect Portal Frame");
			return null;
		}

		
		
		int maxHeight = 0;
		for (int h = 1; h <= 23; h++) {
			
			BlockPos leftPos = horizontal ? basePos.offset(0, h, minExtent) : basePos.offset(minExtent, h, 0);
			BlockPos rightPos = horizontal ? basePos.offset(0, h, maxExtent) : basePos.offset(maxExtent, h, 0);

			
			if (world.getBlockState(leftPos).getBlock() != CreateteleportersModBlocks.QUANTUM_CASING.get() || world.getBlockState(rightPos).getBlock() != CreateteleportersModBlocks.QUANTUM_CASING.get()) {
				break;
			}

			
			boolean isCompleteRow = true;
			for (int w = minExtent; w <= maxExtent; w++) {
				BlockPos checkPos = horizontal ? basePos.offset(0, h, w) : basePos.offset(w, h, 0);
				if (world.getBlockState(checkPos).getBlock() != CreateteleportersModBlocks.QUANTUM_CASING.get()) {
					isCompleteRow = false;
					break;
				}
			}

			if (isCompleteRow) {
				
				
				boolean interiorIsValid = true;
				if (h > 1) {
					for (int w = minExtent + 1; w < maxExtent; w++) {
						BlockPos interiorPos = horizontal ? basePos.offset(0, h - 1, w) : basePos.offset(w, h - 1, 0);
						if (world.getBlockState(interiorPos).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
							interiorIsValid = false;
							break;
						}
					}
				}

				if (interiorIsValid) {
					maxHeight = h;
					break; 
				} else {
					
					storeErrorReason(world, basePos, "Incorrect Portal Frame");
					return null;
				}
			}
		}

		
		int actualHeight = maxHeight + 1; 
		if (actualHeight < 5) {
			storeErrorReason(world, basePos, "Incorrect Portal Frame");
			return null;
		}

		
		if (totalWidth != actualHeight) {
			storeErrorReason(world, basePos, "Incorrect Portal Frame");
			return null;
		}

		
		for (int h = 1; h < maxHeight; h++) {
			for (int w = minExtent + 1; w < maxExtent; w++) {
				BlockPos interiorPos = horizontal ? basePos.offset(0, h, w) : basePos.offset(w, h, 0);
				net.minecraft.world.level.block.state.BlockState state = world.getBlockState(interiorPos);
				
				if (!state.isAir() && state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL.get() && state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get()
						&& state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_ON.get() && state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get()
						&& state.getBlock() != CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get()) {
					storeErrorReason(world, basePos, "Incorrect Portal Frame");
					return null;
				}
			}
		}

		
		return new PortalDimensions(totalWidth, maxHeight, minExtent, maxExtent);
	}

	/**
	 * Checks if a block is a custom portal dummy block.
	 */
	private static boolean isDummyBlock(LevelAccessor world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() == CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get();
	}

	/**
	 * Stores the portal dimensions in the block entity NBT.
	 */
	private static void storePortalDimensions(LevelAccessor world, BlockPos pos, int width, int height, int minExtent, int maxExtent) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			CompoundTag nbt = blockEntity.getPersistentData();
			if (nbt.getInt("portalWidth") == width && nbt.getInt("portalHeight") == height
					&& nbt.getInt("portalMinExtent") == minExtent && nbt.getInt("portalMaxExtent") == maxExtent) return;
			nbt.putInt("portalWidth", width);
			nbt.putInt("portalHeight", height);
			nbt.putInt("portalMinExtent", minExtent);
			nbt.putInt("portalMaxExtent", maxExtent);
			syncController(world, blockEntity);
		}
	}

	/**
	 * Sets the portal active state.
	 */
	private static void setPortalActive(LevelAccessor world, BlockPos pos, boolean active) {
		if (!world.isClientSide()) {
			BlockEntity _blockEntity = world.getBlockEntity(pos);
			if (_blockEntity == null || _blockEntity.getPersistentData().getBoolean("portalActive") == active) return;
			_blockEntity.getPersistentData().putBoolean("portalActive", active);
			syncController(world, _blockEntity);
		}
	}

	private static int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.drain(amount, IFluidHandler.FluidAction.SIMULATE).getAmount();
		}
		return 0;
	}

	private static String getBlockNBTString(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getString(tag);
		return "";
	}

	/**
	 * Stores a detailed error reason in the block entity NBT for GUI display.
	 */
	private static void storeErrorReason(LevelAccessor world, BlockPos pos, String reason) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null) {
			if (reason.equals(blockEntity.getPersistentData().getString("portalError"))) return;
			blockEntity.getPersistentData().putString("portalError", reason);
			syncController(world, blockEntity);
		}
	}

	private static void syncController(LevelAccessor world, BlockEntity controller) {
		controller.setChanged();
		if (world instanceof Level level && !level.isClientSide()) {
			level.sendBlockUpdated(controller.getBlockPos(), controller.getBlockState(), controller.getBlockState(), 3);
		}
	}

	/**
	 * Helper class to store portal dimensions.
	 */
	private static class PortalDimensions {
		final int width;
		final int height;
		final int minExtent;
		final int maxExtent;

		PortalDimensions(int width, int height, int minExtent, int maxExtent) {
			this.width = width;
			this.height = height;
			this.minExtent = minExtent;
			this.maxExtent = maxExtent;
		}
	}
}
