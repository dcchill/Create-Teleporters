package net.createteleporters.integration;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;

import net.createmod.catnip.math.BlockFace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.integration.ImmersivePortalsIntegration;
import net.createmod.catnip.data.Pair;

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
	
	static {
		CreateteleportersMod.LOGGER.info("CreateTrainPortalIntegration class loaded!");
	}

	private CreateTrainPortalIntegration() {
	}

	public static void register() {
		CreateteleportersMod.LOGGER.info("=== REGISTERING TRAIN PORTAL INTEGRATION ===");
		CreateteleportersMod.LOGGER.info("QUANTUM_PORTAL_BLOCK: {}", CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
		PortalTrackProvider.REGISTRY.register(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get(), CreateTrainPortalIntegration::findExit);
		CreateteleportersMod.LOGGER.info("Registered Create train portal provider for quantum portal blocks.");
		
		// Log Immersive Portals compatibility status
		boolean useImmersivePortals = CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.get();
		boolean ipLoaded = ImmersivePortalsIntegration.isImmersivePortalsLoaded();
		CreateteleportersMod.LOGGER.info("Immersive Portals Compatibility: enabled={}, loaded={}", useImmersivePortals, ipLoaded);
		if (useImmersivePortals && ipLoaded) {
			CreateteleportersMod.LOGGER.info("Trains will use Immersive Portals for seamless teleportation");
		} else {
			CreateteleportersMod.LOGGER.info("Trains will use vanilla quantum portal blocks");
		}
		
		CreateteleportersMod.LOGGER.info("=== END REGISTRATION ===");
	}

	private static PortalTrackProvider.Exit findExit(ServerLevel level, BlockFace entryFace) {
		CreateteleportersMod.LOGGER.info("=== TRAIN PORTAL TELEPORT ATTEMPT ===");
		CreateteleportersMod.LOGGER.info("Entry face: {} at {}", entryFace.getFace(), entryFace.getPos());
		
		BlockPos sourcePortalPos = entryFace.getConnectedPos();
		CreateteleportersMod.LOGGER.info("Source portal position: {}", sourcePortalPos);
		
		PortalBaseData sourceBase = findLinkedActivePortalBaseForPortalBlock(level, sourcePortalPos);
		if (sourceBase == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: No active portal base found for portal block at {}", sourcePortalPos);
			return null;
		}
		CreateteleportersMod.LOGGER.info("Found source portal base at {}", sourceBase.basePos);

		ResourceLocation targetDimLoc = ResourceLocation.tryParse(sourceBase.nbt.getString("linkedDim"));
		CreateteleportersMod.LOGGER.info("Target dimension string: '{}', parsed: {}", sourceBase.nbt.getString("linkedDim"), targetDimLoc);
		if (targetDimLoc == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: Invalid target dimension for portal at {}", sourcePortalPos);
			return null;
		}

		ResourceKey<net.minecraft.world.level.Level> targetDim = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
		ServerLevel targetLevel = level.getServer().getLevel(targetDim);
		CreateteleportersMod.LOGGER.info("Target dimension key: {}, level exists: {}", targetDim, targetLevel != null);
		if (targetLevel == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: Target dimension {} not found", targetDimLoc);
			return null;
		}

		BlockPos targetBasePos = BlockPos.containing(
			sourceBase.nbt.getDouble("linkedX"),
			sourceBase.nbt.getDouble("linkedY"),
			sourceBase.nbt.getDouble("linkedZ")
		);
		CreateteleportersMod.LOGGER.info("Target base position: {} (x={}, y={}, z={})", 
			targetBasePos, 
			sourceBase.nbt.getDouble("linkedX"),
			sourceBase.nbt.getDouble("linkedY"),
			sourceBase.nbt.getDouble("linkedZ"));
		
		BlockEntity targetBE = targetLevel.getBlockEntity(targetBasePos);
		CreateteleportersMod.LOGGER.info("Target block entity exists: {}, type: {}", 
			targetBE != null, 
			targetBE != null ? targetBE.getClass().getSimpleName() : "null");
		if (targetBE == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: No block entity at target base position {}", targetBasePos);
			return null;
		}

		CompoundTag targetNbt = targetBE.getPersistentData();
		boolean targetActive = targetNbt.getBoolean("portalActive");
		CreateteleportersMod.LOGGER.info("Target portal active: {}", targetActive);
		if (!targetActive) {
			CreateteleportersMod.LOGGER.warn("FAILED: Target portal at {} is not active", targetBasePos);
			return null;
		}
		String sourceRotation = sourceBase.nbt.getString("rotation");
		String targetRotation = targetNbt.getString("rotation");
		CreateteleportersMod.LOGGER.info("Source rotation: '{}', Target rotation: '{}'", sourceRotation, targetRotation);

		int localHorizontalOffset = getLocalHorizontalOffset(sourceBase.basePos, sourcePortalPos, sourceRotation);
		int localY = sourcePortalPos.getY() - sourceBase.basePos.getY();
		CreateteleportersMod.LOGGER.info("Local offset - horizontal: {}, y: {}", localHorizontalOffset, localY);
		
		BlockPos targetPortalPos = toPortalPos(targetBasePos, targetRotation, localHorizontalOffset, localY);
		CreateteleportersMod.LOGGER.info("Calculated target portal position: {}", targetPortalPos);
		
		// Verify the target portal block exists
		BlockState targetPortalState = targetLevel.getBlockState(targetPortalPos);
		boolean isPortalBlock = targetPortalState.is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
		CreateteleportersMod.LOGGER.info("Target portal block check - position: {}, is portal: {}, block: {}", 
			targetPortalPos, isPortalBlock, targetPortalState.getBlock());
		
		if (!isPortalBlock) {
			BlockPos mirroredTargetPos = toPortalPos(targetBasePos, targetRotation, -localHorizontalOffset, localY);
			CreateteleportersMod.LOGGER.info("Trying mirrored position: {}", mirroredTargetPos);
			BlockState mirroredState = targetLevel.getBlockState(mirroredTargetPos);
			boolean isMirroredPortal = mirroredState.is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
			CreateteleportersMod.LOGGER.info("Mirrored portal block check - is portal: {}, block: {}", 
				isMirroredPortal, mirroredState.getBlock());
			
			if (!isMirroredPortal) {
				CreateteleportersMod.LOGGER.warn("FAILED: No portal block found at calculated target position {} or mirrored position {}", 
					targetPortalPos, mirroredTargetPos);
				return null;
			}
			targetPortalPos = mirroredTargetPos;
			CreateteleportersMod.LOGGER.info("Using mirrored portal position: {}", targetPortalPos);
		}

		Direction sourceNormal = rotationToNormal(sourceRotation);
		Direction targetNormal = rotationToNormal(targetRotation);
		
		// Determine which side the train entered from
		boolean enteredFromBackSide = entryFace.getFace() == sourceNormal;
		CreateteleportersMod.LOGGER.info("Entry analysis - face: {}, sourceNormal: {}, enteredFromBackSide: {}", 
			entryFace.getFace(), sourceNormal, enteredFromBackSide);
		
		// Exit from the corresponding side of the target portal
		Direction exitDirection = enteredFromBackSide ? targetNormal.getOpposite() : targetNormal;
		CreateteleportersMod.LOGGER.info("Target normal: {}, exit direction: {}", targetNormal, exitDirection);

		// Find a valid track on the exit side
		BlockPos exitTrackPos = resolveTrackSide(targetLevel, targetPortalPos, exitDirection);
		CreateteleportersMod.LOGGER.info("Resolved exit track position: {}", exitTrackPos);
		if (exitTrackPos == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: No valid exit track found for portal at {}", targetPortalPos);
			return null;
		}
		
		// The face should point FROM the track TOWARD the portal
		Direction trackFace = exitDirection.getOpposite();
		CreateteleportersMod.LOGGER.info("SUCCESS: Train teleporting from {} to {} (track at {}, face {})", 
			sourcePortalPos, targetPortalPos, exitTrackPos, trackFace);
		CreateteleportersMod.LOGGER.info("=== END TELEPORT ATTEMPT ===");
		
		return new PortalTrackProvider.Exit(targetLevel, new BlockFace(exitTrackPos, trackFace));
	}

	private static BlockPos resolveTrackSide(ServerLevel level, BlockPos portalPos, Direction preferredDirection) {
		CreateteleportersMod.LOGGER.info("Resolving track side - portal: {}, preferred direction: {}", portalPos, preferredDirection);
		
		// Try the preferred side first
		BlockPos preferredTrack = portalPos.relative(preferredDirection);
		CreateteleportersMod.LOGGER.info("Checking preferred track at: {}", preferredTrack);
		if (isUsableExitTrack(level, preferredTrack, preferredDirection.getOpposite())) {
			CreateteleportersMod.LOGGER.info("Found usable track on preferred side");
			return preferredTrack;
		}
		
		// Try the opposite side
		Direction oppositeDirection = preferredDirection.getOpposite();
		BlockPos oppositeTrack = portalPos.relative(oppositeDirection);
		CreateteleportersMod.LOGGER.info("Checking opposite track at: {}", oppositeTrack);
		if (isUsableExitTrack(level, oppositeTrack, oppositeDirection.getOpposite())) {
			CreateteleportersMod.LOGGER.info("Found usable track on opposite side");
			return oppositeTrack;
		}
		
		// No valid track found
		CreateteleportersMod.LOGGER.warn("No usable track found on either side");
		return null;
	}

	private static boolean isUsableExitTrack(ServerLevel level, BlockPos trackPos, Direction travelDirection) {
		CreateteleportersMod.LOGGER.info("  Checking track usability at {} with travel direction {}", trackPos, travelDirection);
		
		BlockState blockState = level.getBlockState(trackPos);
		CreateteleportersMod.LOGGER.info("  Block at position: {}, is ITrackBlock: {}", 
			blockState.getBlock(), blockState.getBlock() instanceof ITrackBlock);
		
		if (!(blockState.getBlock() instanceof ITrackBlock track)) {
			CreateteleportersMod.LOGGER.info("  Not a track block, rejecting");
			return false;
		}

		// Get the direction vector the train will be traveling
		Vec3 lookAngle = Vec3.atLowerCornerOf(travelDirection.getNormal());
		CreateteleportersMod.LOGGER.info("  Look angle for track axis: {}", lookAngle);
		
		// Find the nearest track axis
		Pair<Vec3, AxisDirection> nearestTrackAxis = track.getNearestTrackAxis(level, trackPos, blockState, lookAngle);
		CreateteleportersMod.LOGGER.info("  Nearest track axis result: {}", nearestTrackAxis);
		
		if (nearestTrackAxis == null) {
			CreateteleportersMod.LOGGER.info("  No track axis found, rejecting");
			return false;
		}
		
		CreateteleportersMod.LOGGER.info("  Track axis - position: {}, direction: {}", 
			nearestTrackAxis.getFirst(), nearestTrackAxis.getSecond());
		
		// Verify the track has valid graph connectivity
		TrackGraphLocation graphLocation = TrackGraphHelper.getGraphLocationAt(
			level, 
			trackPos, 
			nearestTrackAxis.getSecond(), 
			nearestTrackAxis.getFirst()
		);
		
		boolean hasGraph = graphLocation != null;
		CreateteleportersMod.LOGGER.info("  Track graph location exists: {}", hasGraph);
		if (hasGraph) {
			CreateteleportersMod.LOGGER.info("  Graph location details: {}", graphLocation);
		}
		
		return hasGraph;
	}

	private static PortalBaseData findLinkedActivePortalBaseForPortalBlock(ServerLevel level, BlockPos portalPos) {
		CreateteleportersMod.LOGGER.info("Searching for portal base near {}", portalPos);
		BlockPos min = portalPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
		BlockPos max = portalPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
		PortalBaseData best = null;
		int bestDistance = Integer.MAX_VALUE;
		int basesFound = 0;
		int linkedActiveBases = 0;
		
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			if (!level.getBlockState(cursor).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) {
				continue;
			}
			basesFound++;

			BlockEntity be = level.getBlockEntity(cursor);
			if (be == null) {
				continue;
			}

			CompoundTag nbt = be.getPersistentData();
			boolean isInterior = isPortalInteriorBlock(cursor, portalPos, nbt);
			boolean isLinked = nbt.getBoolean("isLinked");
			boolean isActive = nbt.getBoolean("portalActive");
			
			CreateteleportersMod.LOGGER.info("  Found base at {} - isInterior: {}, isLinked: {}, isActive: {}", 
				cursor, isInterior, isLinked, isActive);
			
			if (!isInterior) {
				continue;
			}
			if (!isLinked || !isActive) {
				continue;
			}
			
			linkedActiveBases++;
			int distance = cursor.distManhattan(portalPos);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = new PortalBaseData(cursor.immutable(), nbt);
			}
		}
		
		CreateteleportersMod.LOGGER.info("Search complete - bases found: {}, linked+active: {}, best distance: {}", 
			basesFound, linkedActiveBases, bestDistance == Integer.MAX_VALUE ? "none" : bestDistance);
		return best;
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