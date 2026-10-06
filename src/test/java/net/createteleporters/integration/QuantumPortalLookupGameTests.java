package net.createteleporters.integration;

import net.createteleporters.block.CustomPortalBaseBlock;
import net.createteleporters.block.QuantumPortalBlockBlock;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.procedures.QuantumPortalBlockOnTickUpdateProcedure;
import net.createteleporters.procedures.CustomPortalBaseOnTickUpdateProcedure;
import net.createteleporters.procedures.ScalablePortalCheckerProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;

@GameTestHolder("createteleporters")
@PrefixGameTestTemplate(false)
public final class QuantumPortalLookupGameTests {
	@GameTest(template = "train_test")
	public static void teleportRequiresPortalInterior(GameTestHelper helper) throws ReflectiveOperationException {
		var inside = CustomPortalBaseOnTickUpdateProcedure.class.getDeclaredMethod("isInsidePortal", AABB.class, AABB.class);
		inside.setAccessible(true);
		for (boolean eastWest : new boolean[] { false, true }) for (int height : new int[] { 4, 22 }) {
			AABB opening = eastWest ? new AABB(0, 1, -1, 1, height, height - 2) : new AABB(-1, 1, 0, height - 2, height, 1);
			// Player-sized bounds in portal-local coordinates: horizontal, feet Y, depth.
			for (double[] position : new double[][] {
				{ 0.5, 1, 0.5, 1 }, { 0.5, 2, 0.8, 1 },
				{ -1.3, 1, 0.5, 0 }, { -1.4, 1, 0.5, 0 },
				{ height - 1.6, 1, 0.5, 0 }, { 0.5, 1, -0.3, 0 },
				{ 0.5, 1, 1.3, 0 }, { 0.5, 0.25, 0.5, 0 },
				{ 0.5, height, 0.5, 0 }, { 0.5, height + 0.1, 0.5, 0 }
			}) {
				double h = position[0], y = position[1], depth = position[2];
				AABB player = eastWest ? new AABB(depth - 0.3, y, h - 0.3, depth + 0.3, y + 1.8, h + 0.3)
					: new AABB(h - 0.3, y, depth - 0.3, h + 0.3, y + 1.8, depth + 0.3);
				helper.assertTrue((boolean) inside.invoke(null, opening, player) == (position[3] == 1), "Only the opening activates teleporting");
			}
		}
		helper.succeed();
	}

	@GameTest(template = "train_test")
	public static void portalUpdatesSkipUnchangedBlocks(GameTestHelper helper) throws ReflectiveOperationException {
		var update = CustomPortalBaseOnTickUpdateProcedure.class.getDeclaredMethod("updateQuantumPortalBlocks",
			LevelAccessor.class, BlockPos.class, String.class, int.class, int.class, int.class, BlockState.class);
		update.setAccessible(true);
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(new BlockPos(30, 80, 30));
		BlockState purple = CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState();
		BlockState red = purple.setValue(QuantumPortalBlockBlock.COLOR, DyeColor.RED);
		for (Direction rotation : Direction.Plane.HORIZONTAL) for (int height : new int[] { 4, 6, 22 }) {
			Direction horizontal = rotation.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
			int min = -2, max = height - 2, count = (height - 1) * (height - 1);
			for (int x = min; x <= max; x++) for (int y = 0; y <= height; y++) {
				level.setBlock(base.relative(horizontal, x).above(y), (x == min || x == max || y == 0 || y == height)
					? CreateteleportersModBlocks.QUANTUM_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
			}
			helper.assertTrue((int) update.invoke(null, level, base, rotation.getName(), height, min, max, purple) == count, "Fill every interior block");
			helper.assertTrue((int) update.invoke(null, level, base, rotation.getName(), height, min, max, purple) == 0, "Unchanged fill makes zero updates");
			BlockPos interior = base.above();
			BlockState beforeColor = level.getBlockState(interior);
			helper.assertTrue((int) update.invoke(null, level, base, rotation.getName(), height, min, max, red) == count, "Recolor the whole interior");
			helper.assertTrue(level.getBlockState(interior) == beforeColor.setValue(QuantumPortalBlockBlock.COLOR, DyeColor.RED), "Recolor preserves pane connections");
			level.setBlock(interior, Blocks.AIR.defaultBlockState(), 2);
			helper.assertTrue((int) update.invoke(null, level, base, rotation.getName(), height, min, max, red) == 1, "Repair only the missing block");
			level.setBlock(interior, Blocks.STONE.defaultBlockState(), 2);
			helper.assertTrue((int) update.invoke(null, level, base, rotation.getName(), height, min, max, Blocks.AIR.defaultBlockState()) == count - 1, "Clear only portal blocks");
			helper.assertTrue(level.getBlockState(interior).is(Blocks.STONE), "Clearing preserves other blocks");
			helper.assertTrue(level.getBlockState(base.above(height)).is(CreateteleportersModBlocks.QUANTUM_CASING.get()), "Preserve the frame");
			helper.assertTrue((int) update.invoke(null, level, base, rotation.getName(), height, min, max, Blocks.AIR.defaultBlockState()) == 0, "Repeated clearing makes zero updates");
		}
		helper.succeed();
	}

	@GameTest(template = "train_test")
	public static void maximumPortalLookup(GameTestHelper helper) throws ReflectiveOperationException {
		var lookup = QuantumPortalBlockBlock.class.getDeclaredMethod("getReadyLinkedPortalRefreshKey", ServerLevel.class, BlockPos.class);
		lookup.setAccessible(true);
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(new BlockPos(30, 2, 30));
		for (Direction rotation : Direction.Plane.HORIZONTAL) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				level.setBlock(base.relative(side), Blocks.AIR.defaultBlockState(), 2);
			}
			level.setBlock(base, CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get().defaultBlockState().setValue(CustomPortalBaseBlock.FACING, rotation), 2);
			CompoundTag tag = level.getBlockEntity(base).getPersistentData();
			tag.putString("rotation", rotation.getName());
			tag.putInt("portalHeight", 22);
			tag.putBoolean("portalActive", true);
			tag.putBoolean("isLinked", true);
			tag.putString("linkedDim", level.dimension().location().toString());
			tag.putDouble("linkedX", base.getX());
			tag.putDouble("linkedY", base.getY());
			tag.putDouble("linkedZ", base.getZ());
			for (int min : new int[] { -20, -2 }) {
				tag.putInt("portalMinExtent", min);
				tag.putInt("portalMaxExtent", min + 22);
				Direction horizontal = rotation.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
				for (int extent : new int[] { min + 1, min + 21 }) {
					BlockPos interior = base.relative(horizontal, extent).above(21);
					String expected = base.asLong() + "|" + level.dimension().location() + "|" + (double) base.getX() + "|" + (double) base.getY() + "|" + (double) base.getZ();
					helper.assertTrue(expected.equals(lookup.invoke(null, level, interior)), "Find maximum-size portal controller for " + rotation);
					level.setBlock(interior, CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState(), 2);
					helper.assertTrue(base.equals(CreateTrainPortalIntegration.activeBaseForTrack(level, interior.relative(rotation))), "Train provider finds the same maximum-size controller");
					level.setBlock(interior, Blocks.AIR.defaultBlockState(), 2);
					helper.assertTrue(lookup.invoke(null, level, interior.relative(rotation)) == null, "Reject positions outside the portal plane");
					tag.putBoolean("isLinked", false);
					helper.assertTrue(lookup.invoke(null, level, interior) == null, "Reject unlinked controllers");
					tag.putBoolean("isLinked", true);
					tag.putBoolean("portalActive", false);
					helper.assertTrue(lookup.invoke(null, level, interior) == null, "Reject inactive controllers");
					tag.putBoolean("portalActive", true);
				}
			}
			level.setBlock(base, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	@GameTest(template = "train_test")
	public static void controllerOwnsFrameValidation(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(new BlockPos(30, 40, 30));
		for (Direction side : Direction.Plane.HORIZONTAL) level.setBlock(base.relative(side), Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(base, CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get().defaultBlockState(), 2);
		CustomPortalBaseBlockEntity controller = (CustomPortalBaseBlockEntity) level.getBlockEntity(base);
		controller.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 32000));
		BlockPos interior = base.above();
		for (int radius : new int[] { 2, 3 }) {
			for (int x = -radius; x <= radius; x++) for (int y = 0; y <= radius * 2; y++) {
				if (y == 0 && Math.abs(x) <= 1) continue;
				level.setBlock(base.offset(x, y, 0), (y == 0 || y == radius * 2 || Math.abs(x) == radius)
					? CreateteleportersModBlocks.QUANTUM_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
			}
			ScalablePortalCheckerProcedure.execute(level, base.getX(), base.getY(), base.getZ());
			CompoundTag tag = controller.getPersistentData();
			helper.assertTrue(tag.getBoolean("portalActive") && tag.getInt("portalWidth") == radius * 2 + 1, "Activate and resize the frame");
			level.setBlock(interior, CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState(), 2);
			ScalablePortalCheckerProcedure.execute(level, base.getX(), base.getY(), base.getZ());
			QuantumPortalBlockOnTickUpdateProcedure.execute(level, interior.getX(), interior.getY(), interior.getZ());
			helper.assertTrue(level.getBlockState(interior).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get()), "Keep interiors with an active owning controller");
		}
		level.setBlock(base.above(6), Blocks.AIR.defaultBlockState(), 2);
		ScalablePortalCheckerProcedure.execute(level, base.getX(), base.getY(), base.getZ());
		helper.assertTrue(!controller.getPersistentData().getBoolean("portalActive"), "Broken frames deactivate at the controller");
		QuantumPortalBlockOnTickUpdateProcedure.execute(level, interior.getX(), interior.getY(), interior.getZ());
		helper.assertTrue(level.getBlockState(interior).isAir(), "Inactive controllers remove their interiors");
		level.setBlock(base, Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(interior, CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState(), 2);
		QuantumPortalBlockOnTickUpdateProcedure.execute(level, interior.getX(), interior.getY(), interior.getZ());
		helper.assertTrue(level.getBlockState(interior).isAir(), "Orphaned interiors still remove themselves");
		helper.succeed();
	}
}
