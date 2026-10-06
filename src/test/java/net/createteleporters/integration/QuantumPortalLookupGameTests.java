package net.createteleporters.integration;

import net.createteleporters.block.CustomPortalBaseBlock;
import net.createteleporters.block.QuantumPortalBlockBlock;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("createteleporters")
@PrefixGameTestTemplate(false)
public final class QuantumPortalLookupGameTests {
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
}
