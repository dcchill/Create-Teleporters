package net.mcreator.createteleporters.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

import java.util.List;

public class EntityTeleporterEntityWalksOnTheBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate, Entity entity) {
		if (entity == null || world.isClientSide()) return;

		BlockPos pos = BlockPos.containing(x, y, z);
		boolean isCharging = false;

		// Expanded detection box: half block below to one block above
		AABB detectionBox = new AABB(x, y - 0.5, z, x + 1, y + 1.5, z + 1);

		List<Entity> entities = world.getEntitiesOfClass(Entity.class, detectionBox, e ->
				!e.getType().toString().equals("minecraft:item") && e.isAlive());

		if (!entities.isEmpty()) {
			isCharging = true;
		}

		BlockEntity blockEntity = world.getBlockEntity(pos);
		BlockState state = world.getBlockState(pos);

		if (blockEntity != null) {
			blockEntity.getPersistentData().putBoolean("charging", isCharging);
		}

		if (world instanceof Level level) {
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}
}
