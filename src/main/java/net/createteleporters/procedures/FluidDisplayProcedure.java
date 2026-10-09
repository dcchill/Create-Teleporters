package net.createteleporters.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class FluidDisplayProcedure {
	public static Component tooltip(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		return Component.translatable("gui.createteleporters.tank_tooltip",
			getFluidTankLevel(world, pos, 0, null), getFluidTankCapacity(world, pos, 0, null));
	}

	public static double execute(LevelAccessor world, double x, double y, double z) {
		int capacity = getFluidTankCapacity(world, BlockPos.containing(x, y, z), 1, null);
		if (capacity <= 0) {
			return 0;
		}
		return Math.ceil((getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) / (capacity * 0.9)) * 17);
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}

	private static int getFluidTankCapacity(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getTankCapacity(tank);
		}
		return 0;
	}
}
