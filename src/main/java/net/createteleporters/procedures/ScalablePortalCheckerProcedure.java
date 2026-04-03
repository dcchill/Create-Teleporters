package net.createteleporters.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

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
			setPortalActive(world, BlockPos.containing(x, y, z), false);
			return;
		}

		// Check if we have enough fluid (at least 8000 mB)
		if (8000 > drainTankSimulate(world, BlockPos.containing(x, y, z), 8000, null)) {
			setPortalActive(world, BlockPos.containing(x, y, z), false);
			return;
		}

		// Find the valid portal dimensions
		PortalDimensions dims = findValidPortalDimensions(world, BlockPos.containing(x, y, z), isHorizontal);

		if (dims != null) {
			// Store the portal dimensions for later use
			storePortalDimensions(world, BlockPos.containing(x, y, z), dims.width, dims.height, dims.minExtent, dims.maxExtent);
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
		// The base must have dummy blocks on both sides on the bottom row
		Direction.Axis axis = horizontal ? Direction.Axis.Z : Direction.Axis.X;

		// First, verify dummy blocks are adjacent to the base
		BlockPos leftDummyPos = horizontal ? basePos.offset(0, 0, -1) : basePos.offset(-1, 0, 0);
		BlockPos rightDummyPos = horizontal ? basePos.offset(0, 0, 1) : basePos.offset(1, 0, 0);

		if (!isDummyBlock(world, leftDummyPos) || !isDummyBlock(world, rightDummyPos)) {
			return null; // Must have dummy blocks on both sides
		}

		// Scan left/bottom from the left dummy to find quantum casing extent
		int minExtent = -1; // Start at left dummy position
		for (int i = 2; i <= 23; i++) {
			BlockPos checkPos = horizontal ? basePos.offset(0, 0, -i) : basePos.offset(-i, 0, 0);
			if (world.getBlockState(checkPos).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
				minExtent = -i;
			} else {
				break;
			}
		}

		// Scan right/top from the right dummy to find quantum casing extent
		int maxExtent = 1; // Start at right dummy position
		for (int i = 2; i <= 23; i++) {
			BlockPos checkPos = horizontal ? basePos.offset(0, 0, i) : basePos.offset(i, 0, 0);
			if (world.getBlockState(checkPos).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
				maxExtent = i;
			} else {
				break;
			}
		}

		// Calculate total width (must be at least 5: 1q + 1d + 1c + 1d + 1q)
		int totalWidth = maxExtent - minExtent + 1;
		if (totalWidth < 5 || totalWidth > 23) {
			return null;
		}

		// Find the first valid top row (first height where all blocks across are quantum casing)
		// The interior (row below top) must be air/portal blocks, NOT quantum casing
		int maxHeight = 0;
		for (int h = 1; h <= 23; h++) {
			// Check both sides at this height
			BlockPos leftPos = horizontal ? basePos.offset(0, h, minExtent) : basePos.offset(minExtent, h, 0);
			BlockPos rightPos = horizontal ? basePos.offset(0, h, maxExtent) : basePos.offset(maxExtent, h, 0);

			// Both sides must be quantum casing
			if (world.getBlockState(leftPos).getBlock() != CreateteleportersModBlocks.QUANTUM_CASING.get() || world.getBlockState(rightPos).getBlock() != CreateteleportersModBlocks.QUANTUM_CASING.get()) {
				break;
			}

			// Check if this row is a complete top row (all quantum casing across)
			boolean isCompleteRow = true;
			for (int w = minExtent; w <= maxExtent; w++) {
				BlockPos checkPos = horizontal ? basePos.offset(0, h, w) : basePos.offset(w, h, 0);
				if (world.getBlockState(checkPos).getBlock() != CreateteleportersModBlocks.QUANTUM_CASING.get()) {
					isCompleteRow = false;
					break;
				}
			}

			if (isCompleteRow) {
				// Verify the interior (row below) has air/portal blocks, not QC
				// This ensures we found the actual frame top, not a random QC row
				boolean interiorIsValid = true;
				if (h > 1) {
					for (int w = minExtent + 1; w < maxExtent; w++) {
						BlockPos interiorPos = horizontal ? basePos.offset(0, h - 1, w) : basePos.offset(w, h - 1, 0);
						BlockState interiorState = world.getBlockState(interiorPos);
						// Interior should NOT be quantum casing
						if (interiorState.getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
							interiorIsValid = false;
							break;
						}
					}
				}

				if (interiorIsValid) {
					maxHeight = h;
					break; // Found the actual top row
				} else {
					// Interior has QC - this is not a valid portal frame
					return null;
				}
			}
		}

		// Minimum height is 5 (bottom row + 4 more for 3-block interior + top)
		if (maxHeight < 5) {
			return null;
		}

		// Restrict valid portal frame to perfect square dimensions
		if (totalWidth != maxHeight) {
			return null;
		}

		// Verify all interior blocks are air or portal blocks (not frame blocks)
		for (int h = 1; h < maxHeight; h++) {
			for (int w = minExtent + 1; w < maxExtent; w++) {
				BlockPos interiorPos = horizontal ? basePos.offset(0, h, w) : basePos.offset(w, h, 0);
				BlockState state = world.getBlockState(interiorPos);
				// Allow air, custom portal blocks, quantum portal block (filled interior), or the base/dummy at bottom
				if (!state.isAir() && state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL.get() && state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get()
						&& state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_ON.get() && state.getBlock() != CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get()
						&& state.getBlock() != CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get()) {
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
			blockEntity.getPersistentData().putInt("portalWidth", width);
			blockEntity.getPersistentData().putInt("portalHeight", height);
			blockEntity.getPersistentData().putInt("portalMinExtent", minExtent);
			blockEntity.getPersistentData().putInt("portalMaxExtent", maxExtent);
		}
	}

	/**
	 * Sets the portal active state.
	 */
	private static void setPortalActive(LevelAccessor world, BlockPos pos, boolean active) {
		if (!world.isClientSide()) {
			BlockEntity _blockEntity = world.getBlockEntity(pos);
			BlockState _bs = world.getBlockState(pos);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putBoolean("portalActive", active);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(pos, _bs, _bs, 3);
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
