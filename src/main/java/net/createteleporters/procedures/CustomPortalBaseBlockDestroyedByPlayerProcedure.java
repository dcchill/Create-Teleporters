package net.createteleporters.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.createteleporters.init.CreateteleportersModBlocks;

public class CustomPortalBaseBlockDestroyedByPlayerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		// First: Deactivate the portal to prevent tick-based interference
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null) {
				_blockEntity.getPersistentData().putBoolean("portalActive", false);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
		}

		// Remove dummy blocks based on facing direction (without drops)
		net.minecraft.core.Direction facing = getDirectionFromBlockState(blockstate);
		if (facing == net.minecraft.core.Direction.WEST || facing == net.minecraft.core.Direction.EAST) {
			// Remove dummy blocks without drops (false = don't trigger drops)
			world.removeBlock(BlockPos.containing(x, y, z + 1), false);
			world.removeBlock(BlockPos.containing(x, y, z - 1), false);
		} else if (facing == net.minecraft.core.Direction.SOUTH || facing == net.minecraft.core.Direction.NORTH) {
			world.removeBlock(BlockPos.containing(x + 1, y, z), false);
			world.removeBlock(BlockPos.containing(x - 1, y, z), false);
		}

		// Remove quantum portal blocks without dropping items (prevents duplication)
		if (world instanceof ServerLevel _level) {
			// Remove portal blocks in both possible orientations
			// East/West orientation portal area
			for (int dy = 1; dy <= 3; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos pos = BlockPos.containing(x, y + dy, z + dz);
					if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
						_level.removeBlock(pos, false);
				}
			}
			// North/South orientation portal area
			for (int dy = 1; dy <= 3; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					BlockPos pos = BlockPos.containing(x + dx, y + dy, z);
					if (_level.getBlockState(pos).getBlock() == CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())
						_level.removeBlock(pos, false);
				}
			}
		}
	}

	private static net.minecraft.core.Direction getDirectionFromBlockState(BlockState blockState) {
		net.minecraft.world.level.block.state.properties.Property<?> prop = blockState.getBlock().getStateDefinition().getProperty("facing");
		if (prop instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dp)
			return blockState.getValue(dp);
		prop = blockState.getBlock().getStateDefinition().getProperty("axis");
		return prop instanceof net.minecraft.world.level.block.state.properties.EnumProperty ep && ep.getPossibleValues().toArray()[0] instanceof net.minecraft.core.Direction.Axis ? net.minecraft.core.Direction.fromAxisAndDirection((net.minecraft.core.Direction.Axis) blockState.getValue(ep), net.minecraft.core.Direction.AxisDirection.POSITIVE) : net.minecraft.core.Direction.NORTH;
	}
}