package net.createteleporters.integration;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;

import net.createmod.catnip.math.BlockFace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;

/**
 * Registers custom portal support for Create train portal tracks.
 *
 * <p>
 * Create trains do not use vanilla entity teleportation logic. They ask
 * {@link PortalTrackProvider} implementations for a destination track face.
 * This class bridges Create's train portal API with this mod's linked custom
 * portal data stored on the custom portal base block entity.
 */
public final class CreateTrainPortalIntegration {
	private static final int SEARCH_RADIUS = 16;

	private CreateTrainPortalIntegration() {
	}

	public static void register() {
		PortalTrackProvider.REGISTRY.register(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get(), CreateTrainPortalIntegration::findExit);
		CreateteleportersMod.LOGGER.info("Registered Create train portal provider for quantum portal blocks.");
	}

	private static PortalTrackProvider.Exit findExit(ServerLevel level, BlockFace entryFace) {
		BlockPos sourcePortalPos = entryFace.getConnectedPos();
		PortalBaseData sourceBase = findPortalBaseForPortalBlock(level, sourcePortalPos);
		if (sourceBase == null || !sourceBase.nbt.getBoolean("isLinked") || !sourceBase.nbt.getBoolean("portalActive")) {
			return null;
		}

		ResourceLocation targetDimLoc = ResourceLocation.tryParse(sourceBase.nbt.getString("linkedDim"));
		if (targetDimLoc == null) {
			return null;
		}

		ResourceKey<net.minecraft.world.level.Level> targetDim = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
		ServerLevel targetLevel = level.getServer().getLevel(targetDim);
		if (targetLevel == null) {
			return null;
		}

		BlockPos targetBasePos = BlockPos.containing(
			sourceBase.nbt.getDouble("linkedX"),
			sourceBase.nbt.getDouble("linkedY"),
			sourceBase.nbt.getDouble("linkedZ")
		);
		BlockEntity targetBE = targetLevel.getBlockEntity(targetBasePos);
		if (targetBE == null) {
			return null;
		}

		CompoundTag targetNbt = targetBE.getPersistentData();
		if (!targetNbt.getBoolean("portalActive")) {
			return null;
		}
		String sourceRotation = sourceBase.nbt.getString("rotation");
		String targetRotation = targetNbt.getString("rotation");

		int localHorizontalOffset = getLocalHorizontalOffset(sourceBase.basePos, sourcePortalPos, sourceRotation);
		int localY = sourcePortalPos.getY() - sourceBase.basePos.getY();
		BlockPos targetPortalPos = toPortalPos(targetBasePos, targetRotation, localHorizontalOffset, localY);
		if (!targetLevel.getBlockState(targetPortalPos).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())) {
			return null;
		}

		Direction sourceNormal = rotationToNormal(sourceRotation);
		Direction exitDirection = rotationToNormal(targetRotation);
		boolean enteredFromPositiveSide = entryFace.getFace() == sourceNormal.getOpposite();
		Direction targetOffset = enteredFromPositiveSide ? exitDirection : exitDirection.getOpposite();

		return new PortalTrackProvider.Exit(targetLevel, new BlockFace(targetPortalPos.relative(targetOffset), targetOffset.getOpposite()));
	}

	private static PortalBaseData findPortalBaseForPortalBlock(ServerLevel level, BlockPos portalPos) {
		BlockPos min = portalPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
		BlockPos max = portalPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			if (!level.getBlockState(cursor).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) {
				continue;
			}

			BlockEntity be = level.getBlockEntity(cursor);
			if (be == null) {
				continue;
			}

			CompoundTag nbt = be.getPersistentData();
			if (isPortalInteriorBlock(cursor, portalPos, nbt)) {
				return new PortalBaseData(cursor.immutable(), nbt);
			}
		}
		return null;
	}

	private static boolean isPortalInteriorBlock(BlockPos basePos, BlockPos portalPos, CompoundTag nbt) {
		if (!nbt.contains("portalHeight") || !nbt.contains("portalMinExtent") || !nbt.contains("portalMaxExtent")) {
			return false;
		}

		int portalHeight = nbt.getInt("portalHeight");
		int interiorMin = nbt.getInt("portalMinExtent") + 1;
		int interiorMax = nbt.getInt("portalMaxExtent") - 1;
		String rotation = nbt.getString("rotation");
		int dy = portalPos.getY() - basePos.getY();

		if (dy < 1 || dy > (portalHeight - 1)) {
			return false;
		}

		if ("east".equals(rotation) || "west".equals(rotation)) {
			return portalPos.getX() == basePos.getX() && portalPos.getZ() - basePos.getZ() >= interiorMin && portalPos.getZ() - basePos.getZ() <= interiorMax;
		}

		return portalPos.getZ() == basePos.getZ() && portalPos.getX() - basePos.getX() >= interiorMin && portalPos.getX() - basePos.getX() <= interiorMax;
	}

	private static int getLocalHorizontalOffset(BlockPos basePos, BlockPos portalPos, String rotation) {
		if ("east".equals(rotation) || "west".equals(rotation)) {
			return portalPos.getZ() - basePos.getZ();
		}
		return portalPos.getX() - basePos.getX();
	}

	private static BlockPos toPortalPos(BlockPos basePos, String rotation, int horizontalOffset, int localY) {
		if ("east".equals(rotation) || "west".equals(rotation)) {
			return basePos.offset(0, localY, horizontalOffset);
		}
		return basePos.offset(horizontalOffset, localY, 0);
	}

	private static Direction rotationToNormal(String rotation) {
		return switch (rotation) {
			case "east" -> Direction.EAST;
			case "west" -> Direction.WEST;
			case "south" -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
	}

	private record PortalBaseData(BlockPos basePos, CompoundTag nbt) {
	}
}
