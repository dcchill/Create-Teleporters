package net.createteleporters.procedures;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.createteleporters.init.CreateteleportersModBlocks;

public class CustomPortalBaseBlockAddedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		BlockPos basePos = BlockPos.containing(x, y, z);
		Direction facing = getDirectionFromBlockState(blockstate);

		if (!world.isClientSide()) {
			BlockEntity be = world.getBlockEntity(basePos);
			BlockState bs = world.getBlockState(basePos);
			if (be != null)
				be.getPersistentData().putString("rotation", facing.getName());
			if (world instanceof Level level)
				level.sendBlockUpdated(basePos, bs, bs, 3);
		}

		// Define offsets based on facing
		BlockPos offset1;
		BlockPos offset2;
		boolean eastWest = facing == Direction.EAST || facing == Direction.WEST;
		if (eastWest) {
			offset1 = basePos.offset(0, 0, 1);
			offset2 = basePos.offset(0, 0, -1);
		} else {
			offset1 = basePos.offset(1, 0, 0);
			offset2 = basePos.offset(-1, 0, 0);
		}

		// Check if both sides are clear
		if (!canReplace(world, offset1) || !canReplace(world, offset2)) {
			// Not enough space → cancel placement
			Block.dropResources(blockstate, (Level) world, basePos);
			world.destroyBlock(basePos, false);
			return;
		}

		// Place dummy blocks
		world.setBlock(offset1, CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get().defaultBlockState(), 3);
		world.setBlock(offset2, CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get().defaultBlockState(), 3);

		// Assign main block NBT to both dummy blocks
		setMainData(world, offset1, x, y, z);
		setMainData(world, offset2, x, y, z);
	}

	private static boolean canReplace(LevelAccessor world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		// Compatible with 1.21.1 — checks for air or replaceable behavior
		return state.canBeReplaced() || state.isAir();
	}

	private static void setMainData(LevelAccessor world, BlockPos pos, double x, double y, double z) {
		if (!world.isClientSide()) {
			BlockEntity be = world.getBlockEntity(pos);
			BlockState bs = world.getBlockState(pos);
			if (be != null) {
				be.getPersistentData().putDouble("main_x", x);
				be.getPersistentData().putDouble("main_y", y);
				be.getPersistentData().putDouble("main_z", z);
			}
			if (world instanceof Level level)
				level.sendBlockUpdated(pos, bs, bs, 3);
		}
	}

	private static Direction getDirectionFromBlockState(BlockState blockState) {
		Property<?> prop = blockState.getBlock().getStateDefinition().getProperty("facing");
		if (prop instanceof DirectionProperty dp)
			return blockState.getValue(dp);
		prop = blockState.getBlock().getStateDefinition().getProperty("axis");
		return prop instanceof EnumProperty ep && ep.getPossibleValues().toArray()[0] instanceof Direction.Axis
				? Direction.fromAxisAndDirection((Direction.Axis) blockState.getValue(ep), Direction.AxisDirection.POSITIVE)
				: Direction.NORTH;
	}
}
