package net.createteleporters.integration;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackShape;
import net.createmod.catnip.math.BlockFace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.util.CustomPortalTeleportMode;
import java.util.List;


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
	private static final int SEARCH_RADIUS = 24;
	private static boolean registered;
	
	static {
		CreateteleportersMod.LOGGER.info("CreateTrainPortalIntegration class loaded!");
	}

	private CreateTrainPortalIntegration() {
	}

	public static void register() {
		if (registered) return;
		CreateteleportersMod.LOGGER.info("=== REGISTERING TRAIN PORTAL INTEGRATION ===");
		CreateteleportersMod.LOGGER.info("QUANTUM_PORTAL_BLOCK: {}", CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
		PortalTrackProvider.REGISTRY.register(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get(), CreateTrainPortalIntegration::findExit);
		registered = true;
		CreateteleportersMod.LOGGER.info("Registered Create train portal provider for quantum portal blocks.");
		

	}

	
	public static boolean connectQuantumTrack(ServerLevel level, BlockPos pos, BlockState state) {
		TrackShape shape = state.getValue(TrackBlock.SHAPE);
		if (shape != TrackShape.XO && shape != TrackShape.ZO) return false;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (direction.getAxis() != (shape == TrackShape.XO ? Direction.Axis.X : Direction.Axis.Z)
				|| !level.getBlockState(pos.relative(direction)).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())) continue;
			if (!isCompatibleTrack(level, pos, direction)) return true;
			PortalTrackProvider.Exit exit = resolveExit(level, new BlockFace(pos, direction), false);
			if (exit == null) return true;
			ServerLevel target = exit.level();
			BlockPos targetPos = exit.face().getPos();
			if (target == level && targetPos.equals(pos)) return true;
			BlockState existing = target.getBlockState(targetPos);
			if (!(existing.getBlock() instanceof TrackBlock)) {
				
				
				BlockFace reverseEntry = new BlockFace(exit.face().getConnectedPos().relative(exit.face().getFace()), exit.face().getFace().getOpposite());
				if (isCompatibleTrack(target, reverseEntry.getPos(), reverseEntry.getFace())) {
					PortalTrackProvider.Exit reverseExit = PortalTrackProvider.getOtherSide(target, reverseEntry);
					if (reverseExit != null && reverseExit.level() == level && reverseExit.face().getPos().equals(pos)) {
						return connectQuantumTrack(target, reverseEntry.getPos(), target.getBlockState(reverseEntry.getPos()));
					}
				}
			}
			if (!isCompatibleTrack(target, targetPos, exit.face().getFace())) return true;
			BlockState targetState = existing;
			level.setBlock(pos, state.setValue(TrackBlock.SHAPE, TrackShape.asPortal(direction)).setValue(TrackBlock.HAS_BE, true), 3);
			target.setBlock(targetPos, targetState.setValue(TrackBlock.SHAPE, TrackShape.asPortal(exit.face().getFace())).setValue(TrackBlock.HAS_BE, true), 3);
			((TrackBlockEntity) level.getBlockEntity(pos)).bind(target.dimension(), targetPos);
			((TrackBlockEntity) target.getBlockEntity(targetPos)).bind(level.dimension(), pos);
			net.createteleporters.integration.train.QuantumTrainPortals.registerConnection(level, pos, activeBaseForTrack(level, pos),
				target, targetPos, activeBaseForTrack(target, targetPos));
			level.scheduleTick(pos, state.getBlock(), 1);
			target.scheduleTick(targetPos, targetState.getBlock(), 1);
			return true;
		}
		return false;
	}

	private static PortalTrackProvider.Exit findExit(ServerLevel level, BlockFace entryFace) {
		return resolveExit(level, entryFace, true);
	}

	private static PortalTrackProvider.Exit resolveExit(ServerLevel level, BlockFace entryFace, boolean requireTrack) {
		CreateteleportersMod.LOGGER.debug("=== TRAIN PORTAL TELEPORT ATTEMPT ===");
		CreateteleportersMod.LOGGER.debug("Entry face: {} at {}", entryFace.getFace(), entryFace.getPos());
		CreateteleportersMod.LOGGER.debug("Entry track block: {}", level.getBlockState(entryFace.getPos()).getBlock());
		
		BlockPos sourcePortalPos = entryFace.getConnectedPos();
		CreateteleportersMod.LOGGER.debug("Source portal position: {}", sourcePortalPos);
		
		PortalBaseData sourceBase = findLinkedActivePortalBaseForPortalBlock(level, sourcePortalPos);
		if (sourceBase == null) {
			CreateteleportersMod.LOGGER.debug("FAILED: No active portal base found for portal block at {}", sourcePortalPos);
			return null;
		}
		CreateteleportersMod.LOGGER.debug("Found source portal base at {}", sourceBase.basePos);
		if (CustomPortalTeleportMode.isCoordinateMode(level, sourceBase.basePos)) {
			CreateteleportersMod.LOGGER.debug("Train portal at {} requires Portal to portal mode", sourceBase.basePos);
			return null;
		}

		PortalTargetData targetData = resolveLinkedPortalTarget(sourceBase);
		if (targetData == null) {
			CreateteleportersMod.LOGGER.debug("FAILED: Train portal tracks require an active portal-to-portal link at {}", sourceBase.basePos);
			return null;
		}

		ResourceLocation targetDimLoc = targetData.dimension();
		CreateteleportersMod.LOGGER.debug("Target resolved from {} - dimension: {}, base position: {}",
			targetData.source(), targetDimLoc, targetData.basePos());
		if (targetDimLoc == null) {
			CreateteleportersMod.LOGGER.debug("FAILED: Invalid target dimension for portal at {}", sourcePortalPos);
			return null;
		}

		ResourceKey<net.minecraft.world.level.Level> targetDim = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
		ServerLevel targetLevel = level.getServer().getLevel(targetDim);
		CreateteleportersMod.LOGGER.debug("Target dimension key: {}, level exists: {}", targetDim, targetLevel != null);
		if (targetLevel == null) {
			CreateteleportersMod.LOGGER.debug("FAILED: Target dimension {} not found", targetDimLoc);
			return null;
		}

		BlockPos targetBasePos = targetData.basePos();
		if (targetLevel == level && targetBasePos.equals(sourceBase.basePos)) {
			CreateteleportersMod.LOGGER.debug("FAILED: Portal at {} links back to itself", sourceBase.basePos);
			return null;
		}
		CreateteleportersMod.LOGGER.debug("Target base position: {}", targetBasePos);
		
		BlockEntity targetBE = targetLevel.getBlockEntity(targetBasePos);
		CreateteleportersMod.LOGGER.debug("Target block entity exists: {}, type: {}",
			targetBE != null, 
			targetBE != null ? targetBE.getClass().getSimpleName() : "null");
		if (targetBE == null) {
			CreateteleportersMod.LOGGER.debug("FAILED: No block entity at target base position {}", targetBasePos);
			return null;
		}

		CompoundTag targetNbt = targetBE.getPersistentData();
		if (CustomPortalTeleportMode.isCoordinateMode(targetLevel, targetBasePos)) {
			CreateteleportersMod.LOGGER.debug("Destination train portal at {} requires Portal to portal mode", targetBasePos);
			return null;
		}
		boolean targetActive = targetNbt.getBoolean("portalActive");
		CreateteleportersMod.LOGGER.debug("Target portal active: {}", targetActive);
		if (!targetActive) {
			CreateteleportersMod.LOGGER.debug("FAILED: Target portal at {} is not active", targetBasePos);
			return null;
		}
		String sourceRotation = sourceBase.nbt.getString("rotation");
		String targetRotation = targetNbt.getString("rotation");
		CreateteleportersMod.LOGGER.debug("Source rotation: '{}', Target rotation: '{}'", sourceRotation, targetRotation);

		int localHorizontalOffset = getLocalHorizontalOffset(sourceBase.basePos, sourcePortalPos, sourceRotation);
		int localY = sourcePortalPos.getY() - sourceBase.basePos.getY();
		CreateteleportersMod.LOGGER.debug("Local offset - horizontal: {}, y: {}", localHorizontalOffset, localY);
		
		BlockPos targetPortalPos = toPortalPos(targetBasePos, targetRotation, localHorizontalOffset, localY);
		CreateteleportersMod.LOGGER.debug("Calculated target portal position: {}", targetPortalPos);
		
		
		BlockState targetPortalState = targetLevel.getBlockState(targetPortalPos);
		boolean isPortalBlock = targetPortalState.is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
		CreateteleportersMod.LOGGER.debug("Target portal block check - position: {}, is portal: {}, block: {}",
			targetPortalPos, isPortalBlock, targetPortalState.getBlock());
		
		if (!isPortalBlock || !isPortalInteriorBlock(targetBasePos, targetPortalPos, targetNbt)) return null;

		Direction exitDirection = getCreateStyleExitDirection(entryFace.getFace(), targetRotation);
		CreateteleportersMod.LOGGER.debug("Create-style exit direction from entry face {} and target rotation {}: {}",
			entryFace.getFace(), targetRotation, exitDirection);

		BlockFace exitTrackFace = toExitTrackFace(targetPortalPos, exitDirection);
		CreateteleportersMod.LOGGER.debug("Resolved exit track face: {}", exitTrackFace);
		if (!isCompatibleTrack(targetLevel, exitTrackFace.getPos(), exitTrackFace.getFace())
			&& (requireTrack || !targetLevel.getBlockState(exitTrackFace.getPos()).canBeReplaced())) {
			CreateteleportersMod.LOGGER.debug("FAILED: No valid exit position found for portal at {}", targetPortalPos);
			return null;
		}

		CreateteleportersMod.LOGGER.debug("SUCCESS: Train teleporting from {} to {} (track at {}, face {})",
			sourcePortalPos, exitTrackFace.getConnectedPos(), exitTrackFace.getPos(), exitTrackFace.getFace());
		CreateteleportersMod.LOGGER.debug("=== END TELEPORT ATTEMPT ===");
		
		return new PortalTrackProvider.Exit(targetLevel, exitTrackFace);
	}

	private static PortalTargetData resolveLinkedPortalTarget(PortalBaseData sourceBase) {
		if (!sourceBase.nbt.getBoolean("isLinked")) {
			return null;
		}

		String targetDimString = sourceBase.nbt.getString("linkedDim").trim();
		ResourceLocation targetDimLoc = ResourceLocation.tryParse(targetDimString);
		CreateteleportersMod.LOGGER.debug("Linked portal target dimension string: '{}', parsed: {}", targetDimString, targetDimLoc);
		if (targetDimLoc == null) {
			return null;
		}

		BlockPos targetBasePos = BlockPos.containing(
			sourceBase.nbt.getDouble("linkedX"),
			sourceBase.nbt.getDouble("linkedY"),
			sourceBase.nbt.getDouble("linkedZ")
		);
		return new PortalTargetData(targetDimLoc, targetBasePos, "linked portal metadata");
	}

	private static BlockFace toExitTrackFace(BlockPos portalPos, Direction trackSide) {
		return new BlockFace(portalPos.relative(trackSide), trackSide.getOpposite());
	}

	private static Direction getCreateStyleExitDirection(Direction entryDirection, String targetRotation) {
		Direction exitDirection = entryDirection;
		if (exitDirection.getAxis() == getPortalPlaneAxis(targetRotation)) {
			exitDirection = exitDirection.getClockWise();
		}
		return exitDirection;
	}

	private static Direction.Axis getPortalPlaneAxis(String rotation) {
		return "east".equals(rotation) || "west".equals(rotation) ? Direction.Axis.Z : Direction.Axis.X;
	}

	private static boolean isCompatibleTrack(ServerLevel level, BlockPos pos, Direction direction) {
		BlockState state = level.getBlockState(pos);
		TrackShape expected = direction.getAxis() == Direction.Axis.X ? TrackShape.XO : TrackShape.ZO;
		if (!(state.getBlock() instanceof TrackBlock) || state.getValue(TrackBlock.SHAPE) != expected) return false;
		return !(level.getBlockEntity(pos) instanceof TrackBlockEntity track)
			|| (track.boundLocation == null && track.getConnections().isEmpty() && !track.isTilted());
	}

	private static PortalBaseData findLinkedActivePortalBaseForPortalBlock(ServerLevel level, BlockPos portalPos) {
		PortalBaseData best = null;
		int bestDistance = Integer.MAX_VALUE;
		// Only the two planes below the interior can contain its controller.
		for (Iterable<BlockPos> plane : List.of(
				BlockPos.betweenClosed(portalPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, 0), portalPos.offset(SEARCH_RADIUS, -1, 0)),
				BlockPos.betweenClosed(portalPos.offset(0, -SEARCH_RADIUS, -SEARCH_RADIUS), portalPos.offset(0, -1, SEARCH_RADIUS)))) {
			for (BlockPos cursor : plane) {
				if (!level.getBlockState(cursor).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) continue;
				BlockEntity be = level.getBlockEntity(cursor);
				if (be == null) continue;
				CompoundTag nbt = be.getPersistentData();
				if (!nbt.getBoolean("isLinked") || !nbt.getBoolean("portalActive") || !isPortalInteriorBlock(cursor, portalPos, nbt)) continue;
				int distance = cursor.distManhattan(portalPos);
				// Preserve the old cube's Y/Z/X ordering when two controllers are equally close.
				if (distance < bestDistance || (distance == bestDistance && cursor.compareTo(best.basePos) < 0)) {
					bestDistance = distance;
					best = new PortalBaseData(cursor.immutable(), nbt);
				}
			}
		}
		return best;
	}

	public static BlockPos activeBaseForTrack(ServerLevel level, BlockPos trackPos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos portal = trackPos.relative(direction);
			if (!level.getBlockState(portal).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())) continue;
			PortalBaseData base = findLinkedActivePortalBaseForPortalBlock(level, portal);
			if (base != null) return base.basePos;
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
		BlockPos horizontalDirection = horizontalDirection(rotation);
		int dx = portalPos.getX() - basePos.getX();
		int dz = portalPos.getZ() - basePos.getZ();
		return dx * horizontalDirection.getX() + dz * horizontalDirection.getZ();
	}

	private static BlockPos toPortalPos(BlockPos basePos, String rotation, int horizontalOffset, int localY) {
		BlockPos horizontalDirection = horizontalDirection(rotation);
		return basePos.offset(horizontalDirection.getX() * horizontalOffset, localY, horizontalDirection.getZ() * horizontalOffset);
	}

	private static BlockPos horizontalDirection(String rotation) {
		return switch (rotation) {
			case "south" -> new BlockPos(-1, 0, 0);
			case "east" -> new BlockPos(0, 0, 1);
			case "west" -> new BlockPos(0, 0, -1);
			default -> new BlockPos(1, 0, 0);
		};
	}

	private record PortalBaseData(BlockPos basePos, CompoundTag nbt) {
	}

	private record PortalTargetData(ResourceLocation dimension, BlockPos basePos, String source) {
	}

}
