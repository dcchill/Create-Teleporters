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

public class PortalBlockCheckerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (8000 == drainTankSimulate(world, BlockPos.containing(x, y, z), 8000, null)) {
			if ((getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("east") || (getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("west")) {
				if ((world.getBlockState(BlockPos.containing(x, y, z - 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y, z + 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 1, z + 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 1, z - 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 2, z + 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 2, z - 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 3, z + 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 3, z - 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 4, z + 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 4, z - 2))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 4, z + 1))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 4, z - 1))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 4, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("portalActive", true);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				} else {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("portalActive", false);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				}
			} else if ((getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("north") || (getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("south")) {
				if ((world.getBlockState(BlockPos.containing(x - 2, y, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x + 2, y, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x + 2, y + 1, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x - 2, y + 1, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x + 2, y + 2, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x - 2, y + 2, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x + 2, y + 3, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x - 2, y + 3, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x + 2, y + 4, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x - 2, y + 4, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x + 1, y + 4, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x - 1, y + 4, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()
						&& (world.getBlockState(BlockPos.containing(x, y + 4, z))).getBlock() == CreateteleportersModBlocks.QUANTUM_CASING.get()) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("portalActive", true);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				} else {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("portalActive", false);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				}
			}
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
}