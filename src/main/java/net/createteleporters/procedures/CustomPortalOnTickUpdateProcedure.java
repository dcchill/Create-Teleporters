package net.createteleporters.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import net.createteleporters.init.CreateteleportersModBlocks;

public class CustomPortalOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		world.setBlock(BlockPos.containing(x, y, z), CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get().defaultBlockState(), 3);
	}
}