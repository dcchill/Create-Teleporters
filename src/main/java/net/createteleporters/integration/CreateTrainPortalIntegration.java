package net.createteleporters.integration;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackPropagator;
import net.createmod.catnip.math.BlockFace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.integration.ImmersivePortalsIntegration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector3f;

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
	private static final double TRAIN_ENDPOINT_SEARCH_RADIUS_SQR = 6.25;
	private static final double TRAIN_PORTAL_REARM_RADIUS = 3.0;
	private static final int TRAIN_TELEPORT_TRAIL_TICKS = 16;
	private static final String SAME_DIMENSION_PORTAL_TRACK_TAG = "createteleportersSameDimensionPortalTrack";
	private static final String PORTAL_COUNTERPART_TAG = "createteleportersPortalCounterpart";
	private static final Map<UUID, PortalRearmState> TRAINS_WAITING_TO_CLEAR_PORTALS = new HashMap<>();
	private static final Map<UUID, Integer> TRAIN_TELEPORT_TRAILS = new HashMap<>();
	private static boolean tickListenerRegistered;
	
	static {
		CreateteleportersMod.LOGGER.info("CreateTrainPortalIntegration class loaded!");
	}

	private CreateTrainPortalIntegration() {
	}

	public static void register() {
		CreateteleportersMod.LOGGER.info("=== REGISTERING TRAIN PORTAL INTEGRATION ===");
		CreateteleportersMod.LOGGER.info("QUANTUM_PORTAL_BLOCK: {}", CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
		PortalTrackProvider.REGISTRY.register(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get(), CreateTrainPortalIntegration::findExit);
		if (!tickListenerRegistered) {
			NeoForge.EVENT_BUS.addListener(CreateTrainPortalIntegration::onServerTick);
			tickListenerRegistered = true;
		}
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
		CreateteleportersMod.LOGGER.info("Entry track block: {}", level.getBlockState(entryFace.getPos()).getBlock());
		
		BlockPos sourcePortalPos = entryFace.getConnectedPos();
		CreateteleportersMod.LOGGER.info("Source portal position: {}", sourcePortalPos);
		
		PortalBaseData sourceBase = findLinkedActivePortalBaseForPortalBlock(level, sourcePortalPos);
		if (sourceBase == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: No active portal base found for portal block at {}", sourcePortalPos);
			return null;
		}
		CreateteleportersMod.LOGGER.info("Found source portal base at {}", sourceBase.basePos);

		PortalTargetData targetData = resolveLinkedPortalTarget(sourceBase);
		if (targetData == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: Train portal tracks require an active portal-to-portal link at {}", sourceBase.basePos);
			return null;
		}

		ResourceLocation targetDimLoc = targetData.dimension();
		CreateteleportersMod.LOGGER.info("Target resolved from {} - dimension: {}, base position: {}",
			targetData.source(), targetDimLoc, targetData.basePos());
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

		BlockPos targetBasePos = targetData.basePos();
		if (targetLevel == level && targetBasePos.equals(sourceBase.basePos)) {
			CreateteleportersMod.LOGGER.warn("FAILED: Portal at {} links back to itself", sourceBase.basePos);
			return null;
		}
		CreateteleportersMod.LOGGER.info("Target base position: {}", targetBasePos);
		
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
		
		// Prefer the matching portal block, but fall back to any usable interior block.
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
			
			if (isMirroredPortal) {
				targetPortalPos = mirroredTargetPos;
				CreateteleportersMod.LOGGER.info("Using mirrored portal position: {}", targetPortalPos);
			} else {
				CreateteleportersMod.LOGGER.warn("No portal block found at calculated target position {} or mirrored position {}; scanning the full target portal interior",
					targetPortalPos, mirroredTargetPos);
			}
		}

		Direction exitDirection = getCreateStyleExitDirection(entryFace.getFace(), targetRotation);
		CreateteleportersMod.LOGGER.info("Create-style exit direction from entry face {} and target rotation {}: {}",
			entryFace.getFace(), targetRotation, exitDirection);

		// Find a replaceable spot for Create to generate the linked portal track into.
		BlockFace exitTrackFace = resolveExitTrackFace(targetLevel, targetPortalPos, exitDirection, targetBasePos, targetNbt);
		CreateteleportersMod.LOGGER.info("Resolved exit track face: {}", exitTrackFace);
		if (exitTrackFace == null) {
			CreateteleportersMod.LOGGER.warn("FAILED: No valid exit position found for portal at {}", targetPortalPos);
			return null;
		}

		if (targetLevel == level) {
			deferSameDimensionTrackUnbind(level, entryFace.getPos(), exitTrackFace.getPos());
		}

		CreateteleportersMod.LOGGER.info("SUCCESS: Train teleporting from {} to {} (track at {}, face {})", 
			sourcePortalPos, exitTrackFace.getConnectedPos(), exitTrackFace.getPos(), exitTrackFace.getFace());
		CreateteleportersMod.LOGGER.info("=== END TELEPORT ATTEMPT ===");
		
		return new PortalTrackProvider.Exit(targetLevel, exitTrackFace);
	}

	private static void deferSameDimensionTrackUnbind(ServerLevel level, BlockPos sourceTrackPos, BlockPos targetTrackPos) {
		CreateteleportersMod.queueServerWork(1, () -> {
			removeBoundTrackEdge(level, sourceTrackPos);
			removeBoundTrackEdge(level, targetTrackPos);
			addLocalTrackEndpoint(level, sourceTrackPos, targetTrackPos);
			addLocalTrackEndpoint(level, targetTrackPos, sourceTrackPos);
			CreateteleportersMod.LOGGER.info("Converted same-dimension portal tracks at {} and {} into independent local endpoints",
				sourceTrackPos, targetTrackPos);
		});
	}

	private static void removeBoundTrackEdge(ServerLevel level, BlockPos trackPos) {
		BlockEntity blockEntity = level.getBlockEntity(trackPos);
		if (!(blockEntity instanceof TrackBlockEntity trackBlockEntity) || trackBlockEntity.boundLocation == null) {
			return;
		}

		BlockState trackState = level.getBlockState(trackPos);
		TrackPropagator.onRailRemoved(level, trackPos, trackState);
		trackBlockEntity.boundLocation = null;
		trackBlockEntity.setChanged();
	}

	private static void addLocalTrackEndpoint(ServerLevel level, BlockPos trackPos, BlockPos counterpartTrackPos) {
		BlockEntity blockEntity = level.getBlockEntity(trackPos);
		if (!(blockEntity instanceof TrackBlockEntity trackBlockEntity)) {
			CreateteleportersMod.LOGGER.warn("Could not finalize same-dimension portal track at {} because its track block entity is missing", trackPos);
			return;
		}

		trackBlockEntity.boundLocation = null;
		trackBlockEntity.getPersistentData().putBoolean(SAME_DIMENSION_PORTAL_TRACK_TAG, true);
		trackBlockEntity.getPersistentData().putLong(PORTAL_COUNTERPART_TAG, counterpartTrackPos.asLong());
		trackBlockEntity.setChanged();
		TrackPropagator.onRailAdded(level, trackPos, level.getBlockState(trackPos));
	}

	private static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		TRAIN_TELEPORT_TRAILS.entrySet().removeIf(entry -> {
			Train train = Create.RAILWAYS.trains.get(entry.getKey());
			if (train == null || entry.getValue() <= 0) {
				return true;
			}
			if (entry.getValue() % 2 == 0) {
				spawnTrainTrail(train);
			}
			entry.setValue(entry.getValue() - 1);
			return false;
		});

		TRAINS_WAITING_TO_CLEAR_PORTALS.entrySet().removeIf(entry -> {
			Train train = Create.RAILWAYS.trains.get(entry.getKey());
			return train == null || isTrainClearOfPortalAreas(server, train, entry.getValue());
		});

		for (Train train : List.copyOf(Create.RAILWAYS.trains.values())) {
			if (TRAINS_WAITING_TO_CLEAR_PORTALS.containsKey(train.id) || train.derailed || train.carriages.isEmpty()) {
				continue;
			}
			if (Math.abs(train.speed) < 0.01 && Math.abs(train.targetSpeed) < 0.01 && Math.abs(train.throttle) < 0.01) {
				continue;
			}

			Carriage firstCarriage = train.carriages.getFirst();
			Carriage lastCarriage = train.carriages.getLast();
			double motion = Math.abs(train.speed) >= 0.01 ? train.speed : train.targetSpeed;
			TravellingPoint approachingEndpoint = motion >= 0
				? firstCarriage.getLeadingPoint()
				: lastCarriage.getTrailingPoint();
			if (tryTeleportAtEndpoint(server, train, approachingEndpoint)) {
				continue;
			}
		}
	}

	private static boolean tryTeleportAtEndpoint(MinecraftServer server, Train train, TravellingPoint endpoint) {
		if (endpoint == null || endpoint.node1 == null || endpoint.node2 == null || train.graph == null) {
			return false;
		}

		ServerLevel level = server.getLevel(endpoint.node1.getLocation().dimension);
		if (level == null) {
			return false;
		}

		Vec3 endpointPosition = endpoint.getPosition(train.graph);
		BlockPos portalTrackPos = findNearbySameDimensionPortalTrack(level, endpointPosition);
		if (portalTrackPos == null) {
			return false;
		}

		TrackBlockEntity sourceTrack = getSameDimensionPortalTrack(level, portalTrackPos);
		if (sourceTrack == null || !sourceTrack.getPersistentData().contains(PORTAL_COUNTERPART_TAG)) {
			return false;
		}

		BlockPos counterpartPos = BlockPos.of(sourceTrack.getPersistentData().getLong(PORTAL_COUNTERPART_TAG));
		TrackBlockEntity counterpartTrack = getSameDimensionPortalTrack(level, counterpartPos);
		if (counterpartTrack == null) {
			return false;
		}

		Vec3 exitDirection = getPortalOutwardDirection(level, counterpartPos);
		if (exitDirection == null) {
			CreateteleportersMod.LOGGER.warn("Cannot teleport train {} because destination portal track {} is not adjacent to a portal", train.id, counterpartPos);
			return false;
		}

		double previousSpeed = train.speed;
		double previousTargetSpeed = train.targetSpeed;
		double previousThrottle = train.throttle;
		if (!TrainRelocator.relocate(train, level, counterpartPos, null, false, exitDirection, true)) {
			CreateteleportersMod.LOGGER.warn("Could not teleport train {} to {}. Ensure the destination track has enough clear rail for the entire train.",
				train.id, counterpartPos);
			return false;
		}

		spawnPortalBurst(level, portalTrackPos, getPortalOutwardDirection(level, portalTrackPos));
		spawnTrainFlash(train, level);
		playTeleportSounds(level, portalTrackPos);
		boolean relocated = TrainRelocator.relocate(train, level, counterpartPos, null, false, exitDirection, false);
		if (!relocated) {
			CreateteleportersMod.LOGGER.warn("Train {} passed relocation validation but failed to teleport to {}", train.id, counterpartPos);
			return false;
		}

		double motionSign = getOutwardMotionSign(train, exitDirection);
		double speedMagnitude = Math.max(0.05, Math.abs(previousSpeed));
		double targetSpeedMagnitude = Math.max(speedMagnitude, Math.abs(previousTargetSpeed));
		double throttleMagnitude = Math.max(0.05, Math.abs(previousThrottle));
		train.speed = motionSign * speedMagnitude;
		train.targetSpeed = motionSign * targetSpeedMagnitude;
		train.throttle = motionSign * throttleMagnitude;
		TRAINS_WAITING_TO_CLEAR_PORTALS.put(train.id, new PortalRearmState(level.dimension(), portalTrackPos, counterpartPos));
		TRAIN_TELEPORT_TRAILS.put(train.id, TRAIN_TELEPORT_TRAIL_TICKS);
		spawnPortalBurst(level, counterpartPos, exitDirection);
		spawnTrainFlash(train, level);
		playTeleportSounds(level, counterpartPos);
		CreateteleportersMod.LOGGER.info("Teleported train {} from {} to {}", train.id, portalTrackPos, counterpartPos);
		return true;
	}

	private static void spawnPortalBurst(ServerLevel level, BlockPos trackPos, Vec3 outwardDirection) {
		Vec3 center = Vec3.atCenterOf(trackPos).add(0, 1, 0);
		Vec3 direction = outwardDirection == null ? Vec3.ZERO : outwardDirection;
		DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.55f, 0.1f, 1.0f), 1.5f);
		DustParticleOptions violet = new DustParticleOptions(new Vector3f(0.65f, 0.25f, 1.0f), 1.25f);

		level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 70, 1.2, 1.5, 1.2, 0.35);
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 28, 0.8, 1.2, 0.8, 0.18);
		level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);

		for (int ring = 0; ring < 3; ring++) {
			double radius = 1.0 + ring * 0.55;
			for (int i = 0; i < 24; i++) {
				double angle = Math.PI * 2 * i / 24;
				double horizontal = Math.cos(angle) * radius;
				double vertical = Math.sin(angle) * radius;
				double x = center.x + (Math.abs(direction.x) > 0.5 ? 0 : horizontal);
				double z = center.z + (Math.abs(direction.z) > 0.5 ? 0 : horizontal);
				level.sendParticles(ring % 2 == 0 ? purple : violet, x, center.y + vertical, z, 1,
					direction.x * 0.15, 0, direction.z * 0.15, 0.05);
			}
		}
	}

	private static void spawnTrainFlash(Train train, ServerLevel level) {
		DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.55f, 0.1f, 1.0f), 1.4f);
		for (Carriage carriage : train.carriages) {
			carriage.forEachPresentEntity(entity -> {
				if (entity.level() != level) {
					return;
				}
				AABB bounds = entity.getBoundingBox();
				Vec3 center = bounds.getCenter();
				level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 18,
					Math.max(0.5, bounds.getXsize() / 3), Math.max(0.5, bounds.getYsize() / 3), Math.max(0.5, bounds.getZsize() / 3), 0.08);
				level.sendParticles(purple, center.x, center.y, center.z, 22,
					Math.max(0.5, bounds.getXsize() / 3), Math.max(0.5, bounds.getYsize() / 3), Math.max(0.5, bounds.getZsize() / 3), 0.03);
			});
		}
	}

	private static void spawnTrainTrail(Train train) {
		for (Carriage carriage : train.carriages) {
			carriage.forEachPresentEntity(entity -> {
				if (!(entity.level() instanceof ServerLevel level)) {
					return;
				}
				AABB bounds = entity.getBoundingBox();
				Vec3 center = bounds.getCenter();
				level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 8,
					Math.max(0.3, bounds.getXsize() / 4), Math.max(0.3, bounds.getYsize() / 4), Math.max(0.3, bounds.getZsize() / 4), 0.12);
				level.sendParticles(ParticleTypes.GLOW, center.x, center.y, center.z, 4,
					Math.max(0.3, bounds.getXsize() / 4), Math.max(0.3, bounds.getYsize() / 4), Math.max(0.3, bounds.getZsize() / 4), 0.04);
			});
		}
	}

	private static void playTeleportSounds(ServerLevel level, BlockPos trackPos) {
		level.playSound(null, trackPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.2f, 0.65f);
		level.playSound(null, trackPos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.8f, 1.35f);
	}

	private static double getOutwardMotionSign(Train train, Vec3 exitDirection) {
		TravellingPoint leadingPoint = train.carriages.getFirst().getLeadingPoint();
		if (leadingPoint == null || leadingPoint.edge == null) {
			return 1;
		}

		double edgeLength = leadingPoint.edge.getLength();
		double edgePosition = edgeLength == 0 ? 0 : leadingPoint.position / edgeLength;
		Vec3 positiveMotionDirection = leadingPoint.edge.getDirectionAt(edgePosition);
		return positiveMotionDirection.dot(exitDirection) >= 0 ? 1 : -1;
	}

	private static boolean isTrainClearOfPortalAreas(MinecraftServer server, Train train, PortalRearmState rearmState) {
		ServerLevel level = server.getLevel(rearmState.dimension());
		if (level == null) {
			return true;
		}

		AABB sourceArea = new AABB(rearmState.sourceTrack()).inflate(TRAIN_PORTAL_REARM_RADIUS);
		AABB targetArea = new AABB(rearmState.targetTrack()).inflate(TRAIN_PORTAL_REARM_RADIUS);
		AtomicBoolean intersectsPortal = new AtomicBoolean(false);
		for (Carriage carriage : train.carriages) {
			carriage.forEachPresentEntity(entity -> {
				if (entity.level() == level
					&& (entity.getBoundingBox().intersects(sourceArea) || entity.getBoundingBox().intersects(targetArea))) {
					intersectsPortal.set(true);
				}
			});
			if (intersectsPortal.get()) {
				return false;
			}
		}
		return true;
	}

	private static BlockPos findNearbySameDimensionPortalTrack(ServerLevel level, Vec3 endpointPosition) {
		BlockPos center = BlockPos.containing(endpointPosition);
		for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
			TrackBlockEntity track = getSameDimensionPortalTrack(level, cursor);
			if (track != null && Vec3.atCenterOf(cursor).distanceToSqr(endpointPosition) <= TRAIN_ENDPOINT_SEARCH_RADIUS_SQR) {
				return cursor.immutable();
			}
		}
		return null;
	}

	private static TrackBlockEntity getSameDimensionPortalTrack(ServerLevel level, BlockPos trackPos) {
		BlockEntity blockEntity = level.getBlockEntity(trackPos);
		if (blockEntity instanceof TrackBlockEntity trackBlockEntity
			&& trackBlockEntity.getPersistentData().getBoolean(SAME_DIMENSION_PORTAL_TRACK_TAG)) {
			return trackBlockEntity;
		}
		return null;
	}

	private static Vec3 getPortalOutwardDirection(ServerLevel level, BlockPos trackPos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(trackPos.relative(direction)).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())) {
				return Vec3.atLowerCornerOf(direction.getOpposite().getNormal());
			}
		}
		return null;
	}

	private static PortalTargetData resolveLinkedPortalTarget(PortalBaseData sourceBase) {
		if (!sourceBase.nbt.getBoolean("isLinked")) {
			return null;
		}

		String targetDimString = sourceBase.nbt.getString("linkedDim").trim();
		ResourceLocation targetDimLoc = ResourceLocation.tryParse(targetDimString);
		CreateteleportersMod.LOGGER.info("Linked portal target dimension string: '{}', parsed: {}", targetDimString, targetDimLoc);
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

	private static BlockFace resolveExitTrackFace(ServerLevel level, BlockPos portalPos, Direction preferredDirection, BlockPos targetBasePos, CompoundTag targetNbt) {
		CreateteleportersMod.LOGGER.info("Resolving track side - portal: {}, preferred direction: {}", portalPos, preferredDirection);

		List<BlockPos> candidatePortals = collectPortalCandidates(portalPos, targetBasePos, targetNbt);
		CreateteleportersMod.LOGGER.info("Checking {} candidate portal positions for an exit position", candidatePortals.size());

		for (BlockPos candidatePortalPos : candidatePortals) {
			if (!level.getBlockState(candidatePortalPos).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())) {
				CreateteleportersMod.LOGGER.info("Skipping candidate {} because it is not a quantum portal block", candidatePortalPos);
				continue;
			}

			BlockFace preferredFace = toExitTrackFace(candidatePortalPos, preferredDirection);
			CreateteleportersMod.LOGGER.info("Checking preferred exit position at: {} for portal {}", preferredFace.getPos(), candidatePortalPos);
			if (isUsableExitPosition(level, preferredFace.getPos())) {
				CreateteleportersMod.LOGGER.info("Found usable exit position on preferred side of {}", candidatePortalPos);
				return preferredFace;
			}
		}

		CreateteleportersMod.LOGGER.warn("No usable exit position found on the Create-matched side");
		return null;
	}

	private static BlockFace toExitTrackFace(BlockPos portalPos, Direction trackSide) {
		return new BlockFace(portalPos.relative(trackSide), trackSide.getOpposite());
	}

	private static List<BlockPos> collectPortalCandidates(BlockPos primaryPortalPos, BlockPos targetBasePos, CompoundTag targetNbt) {
		List<BlockPos> candidates = new ArrayList<>();
		candidates.add(primaryPortalPos);

		if (!targetNbt.contains("portalHeight") || !targetNbt.contains("portalMinExtent") || !targetNbt.contains("portalMaxExtent")) {
			return candidates;
		}

		int portalHeight = targetNbt.getInt("portalHeight");
		int interiorMin = targetNbt.getInt("portalMinExtent") + 1;
		int interiorMax = targetNbt.getInt("portalMaxExtent") - 1;
		String rotation = targetNbt.getString("rotation");
		BlockPos horizontalDirection = horizontalDirection(rotation);

		for (int yOffset = 1; yOffset <= portalHeight - 1; yOffset++) {
			for (int horizontalOffset = interiorMin; horizontalOffset <= interiorMax; horizontalOffset++) {
				BlockPos candidate = targetBasePos.offset(horizontalDirection.getX() * horizontalOffset, yOffset, horizontalDirection.getZ() * horizontalOffset);
				if (!candidates.contains(candidate)) {
					candidates.add(candidate);
				}
			}
		}

		candidates.sort(Comparator
			.comparingInt((BlockPos pos) -> Math.abs(pos.getY() - primaryPortalPos.getY()))
			.thenComparingInt(pos -> pos.distManhattan(primaryPortalPos)));
		return candidates;
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

	private static boolean isUsableExitPosition(ServerLevel level, BlockPos trackPos) {
		BlockState blockState = level.getBlockState(trackPos);
		boolean canReplace = blockState.canBeReplaced();
		CreateteleportersMod.LOGGER.info("  Exit block at {} is {} and canBeReplaced={}", trackPos, blockState.getBlock(), canReplace);
		return canReplace;
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

	private record PortalBaseData(BlockPos basePos, CompoundTag nbt) {
	}

	private record PortalTargetData(ResourceLocation dimension, BlockPos basePos, String source) {
	}

	private record PortalRearmState(ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos sourceTrack, BlockPos targetTrack) {
	}

}
