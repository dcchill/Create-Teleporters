package net.createteleporters.integration.train;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackPropagator;
import com.simibubi.create.content.trains.track.TrackShape;
import java.util.Collection;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.integration.CreateTrainPortalIntegration;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class QuantumTrainPortals {
	private static final String LEGACY = "createteleportersSameDimensionPortalTrack";
	private static final String COUNTERPART = "createteleportersPortalCounterpart";
	private static final String BASE = "CTPTrainBase";
	private static final String TARGET_BASE = "CTPTrainTargetBase";
	private QuantumTrainPortals() { }

	public static void registerConnection(ServerLevel source, BlockPos sourceTrack, BlockPos sourceBase,
		ServerLevel target, BlockPos targetTrack, BlockPos targetBase) {
		CreateteleportersMod.queueServerWork(1, () -> {
			if (!(source.getBlockEntity(sourceTrack) instanceof TrackBlockEntity first) || first.boundLocation == null
				|| !first.boundLocation.getFirst().equals(target.dimension()) || !first.boundLocation.getSecond().equals(targetTrack)
				|| !(target.getBlockEntity(targetTrack) instanceof TrackBlockEntity second) || second.boundLocation == null
				|| !second.boundLocation.getFirst().equals(source.dimension()) || !second.boundLocation.getSecond().equals(sourceTrack)) return;
			stamp(source, sourceTrack, sourceBase, targetBase);
			stamp(target, targetTrack, targetBase, sourceBase);
		});
	}
	private static void stamp(ServerLevel level, BlockPos track, BlockPos base, BlockPos targetBase) {
		if (level.getBlockEntity(track) instanceof TrackBlockEntity be) {
			be.getPersistentData().putLong(BASE, base.asLong());
			be.getPersistentData().putLong(TARGET_BASE, targetBase.asLong());
			be.setChanged();
		}
	}
	public static TrackNodeLocation endpoint(ServerLevel level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof TrackBlock block) || !state.getValue(TrackBlock.SHAPE).isPortal()) return null;
		Vec3 plane = Vec3.atBottomCenterOf(pos).add(0, block.getElevationAtCenter(level, pos, state), 0)
			.add(block.getTrackAxes(level, pos, state).getFirst().scale(0.5));
		return new TrackNodeLocation(plane).in(level);
	}
	public static boolean quantumTrack(ServerLevel level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof TrackBlock) || !state.getValue(TrackBlock.SHAPE).isPortal()) return false;
		Vec3 axis = state.getValue(TrackBlock.SHAPE).getAxes().getFirst();
		return level.getBlockState(pos.offset(BlockPos.containing(axis))).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
	}
	public static void markNodes(ServerLevel level, BlockPos pos, BlockState state, TrackNodeLocation from,
		Collection<TrackNodeLocation.DiscoveredLocation> nodes) {
		if (!(level.getBlockEntity(pos) instanceof TrackBlockEntity track) || track.boundLocation == null) return;
		if (!track.getPersistentData().contains(BASE) && !quantumTrack(level, pos, state)) return;
		ServerLevel other = level.getServer().getLevel(track.boundLocation.getFirst());
		BlockPos otherPos = track.boundLocation.getSecond();
		if (other == null || !other.hasChunkAt(otherPos)) return;
		TrackNodeLocation first = endpoint(level, pos, state);
		TrackNodeLocation second = endpoint(other, otherPos, other.getBlockState(otherPos));
		if (first == null || second == null || first.equals(second)) return;
		for (TrackNodeLocation.DiscoveredLocation node : nodes) {
			if (node.equals(first) || node.equals(second)) {
				node.forceNode();
				((PortalNode) node).ctp$setCounterpart(node.equals(first) ? second : first);
			}
		}
		if (from != null && (from.equals(first) || from.equals(second))) ((PortalNode) from).ctp$setCounterpart(from.equals(first) ? second : first);
	}

	public static void migrate(TrackBlockEntity track) {
		if (!(track.getLevel() instanceof ServerLevel level)) return;
		CompoundTag tag = track.getPersistentData();
		if (!tag.getBoolean(LEGACY) && !tag.contains(BASE) && track.boundLocation != null
			&& quantumTrack(level, track.getBlockPos(), track.getBlockState())) {
			ServerLevel otherLevel = level.getServer().getLevel(track.boundLocation.getFirst());
			BlockPos otherPos = track.boundLocation.getSecond();
			if (otherLevel == null || !otherLevel.hasChunkAt(otherPos)
				|| !(otherLevel.getBlockEntity(otherPos) instanceof TrackBlockEntity other) || other.boundLocation == null
				|| !other.boundLocation.getFirst().equals(level.dimension()) || !other.boundLocation.getSecond().equals(track.getBlockPos())) return;
			BlockPos base = CreateTrainPortalIntegration.activeBaseForTrack(level, track.getBlockPos());
			BlockPos otherBase = CreateTrainPortalIntegration.activeBaseForTrack(otherLevel, otherPos);
			if (base != null && otherBase != null) {
				stamp(level, track.getBlockPos(), base, otherBase);
				stamp(otherLevel, otherPos, otherBase, base);
			}
			return;
		}
		if (!tag.getBoolean(LEGACY) || !tag.contains(COUNTERPART)) return;
		BlockPos pos = track.getBlockPos();
		BlockPos otherPos = BlockPos.of(tag.getLong(COUNTERPART));
		if (pos.equals(otherPos) || !level.hasChunkAt(otherPos) || !(level.getBlockEntity(otherPos) instanceof TrackBlockEntity other)) return;
		CompoundTag otherTag = other.getPersistentData();
		if (!otherTag.getBoolean(LEGACY) || otherTag.getLong(COUNTERPART) != pos.asLong()) return;
		if (!quantumTrack(level, pos, track.getBlockState()) || !quantumTrack(level, otherPos, other.getBlockState())) return;
		AABB entrance = new AABB(pos).inflate(4);
		AABB exit = new AABB(otherPos).inflate(4);
		boolean[] occupied = { false };
		for (Train train : Create.RAILWAYS.trains.values()) for (Carriage carriage : train.carriages) carriage.forEachPresentEntity(entity -> {
			if (entity.level() == level && (entity.getBoundingBox().intersects(entrance) || entity.getBoundingBox().intersects(exit))) occupied[0] = true;
		});
		if (occupied[0]) return;
		BlockPos base = CreateTrainPortalIntegration.activeBaseForTrack(level, pos);
		BlockPos otherBase = CreateTrainPortalIntegration.activeBaseForTrack(level, otherPos);
		if (base == null || otherBase == null) return;
		stamp(level, pos, base, otherBase);
		stamp(level, otherPos, otherBase, base);
		TrackPropagator.onRailRemoved(level, pos, track.getBlockState());
		TrackPropagator.onRailRemoved(level, otherPos, other.getBlockState());
		track.bind(level.dimension(), otherPos);
		other.bind(level.dimension(), pos);
		TrackPropagator.onRailAdded(level, pos, track.getBlockState());
		TrackPropagator.onRailAdded(level, otherPos, other.getBlockState());
		TrackNodeLocation first = endpoint(level, pos, track.getBlockState());
		TrackNodeLocation second = endpoint(level, otherPos, other.getBlockState());
		boolean connected = Create.RAILWAYS.trackNetworks.values().stream().anyMatch(graph -> {
			var a = graph.locateNode(first); var b = graph.locateNode(second);
			return a != null && b != null && graph.getConnectionsFrom(a).get(b) != null && graph.getConnectionsFrom(a).get(b).isInterDimensional();
		});
		if (!connected) return; // Retain the legacy metadata so the upgrade can retry.
		tag.remove(LEGACY); tag.remove(COUNTERPART);
		otherTag.remove(LEGACY); otherTag.remove(COUNTERPART);
		track.setChanged(); other.setChanged();
	}

	public static void maintain(TrackBlockEntity track) {
		if (!(track.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
		if (!track.getPersistentData().contains(BASE) || track.boundLocation == null) return;
		ServerLevel other = level.getServer().getLevel(track.boundLocation.getFirst());
		if (other == null || !other.hasChunkAt(track.boundLocation.getSecond())) return;
		if (active(track) || occupied(level, track.getBlockPos(), track.getBlockState())) return;
		BlockPos pos = track.getBlockPos();
		BlockState state = track.getBlockState();
		BlockPos basePos = BlockPos.of(track.getPersistentData().getLong(BASE));
		var base = level.getBlockEntity(basePos);
		if (base != null && base.getPersistentData().getBoolean("portalActive") && matchesLink(track, base.getPersistentData())) {
			// Changing only destination mode closes entry without tearing up the rails.
			return;
		}
		boolean relinked = base != null && base.getPersistentData().getBoolean("portalActive")
			&& base.getPersistentData().getBoolean("isLinked") && quantumTrack(level, pos, state);
		if (relinked) {
			TrackShape normal = state.getValue(TrackBlock.SHAPE).getAxes().getFirst().x == 0 ? TrackShape.ZO : TrackShape.XO;
			level.setBlock(pos, state.setValue(TrackBlock.SHAPE, normal).setValue(TrackBlock.HAS_BE, false), 3);
			level.scheduleTick(pos, state.getBlock(), 1);
		} else {
			level.destroyBlock(pos, false);
		}
	}

	public static boolean using(Train train, TrackNodeLocation endpoint) {
		Carriage previous = null;
		for (Carriage carriage : train.carriages) {
			for (var entry : ((PortalCarriage) carriage).ctp$entities().entrySet()) {
				TrackNodeLocation pivot = entry.getValue().pivot;
				if (pivot != null && pivot.equalsIgnoreDim(endpoint) && PortalSide.dimension(entry.getKey()).equals(endpoint.dimension)) return true;
			}
			if (previous != null) {
				int i = train.carriages.indexOf(carriage) - 1;
				PortalPath.Result gap = PortalPath.between(train.graph, carriage.getLeadingPoint(), previous.getTrailingPoint(), train.carriageSpacing.get(i) + 20);
				if (gap != null && gap.crossing() != null && (gap.crossing().entrance().equals(endpoint) || gap.crossing().exit().equals(endpoint))) return true;
			}
			previous = carriage;
		}
		return false;
	}
	public static boolean occupied(ServerLevel level, BlockPos pos, BlockState state) {
		TrackNodeLocation node = endpoint(level, pos, state);
		return node != null && Create.RAILWAYS.trains.values().stream().anyMatch(train -> using(train, node));
	}
	public static boolean canEnter(Train train, TrackNodeLocation node) {
		if (!(node instanceof PortalNode portal) || portal.ctp$getCounterpart() == null) return true;
		if (using(train, node)) return true;
		var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
		if (server == null) return true;
		ServerLevel level = server.getLevel(node.dimension);
		if (level == null) return false;
		for (BlockPos pos : node.allAdjacent()) {
			if (level.getBlockEntity(pos) instanceof TrackBlockEntity track && track.getPersistentData().contains(BASE)) return active(track);
		}
		return false;
	}
	public static boolean active(TrackBlockEntity track) {
		if (!(track.getLevel() instanceof ServerLevel level) || track.boundLocation == null) return false;
		CompoundTag tag = track.getPersistentData();
		if (!tag.contains(BASE) || !tag.contains(TARGET_BASE)) return false;
		var base = level.getBlockEntity(BlockPos.of(tag.getLong(BASE)));
		ServerLevel other = level.getServer().getLevel(track.boundLocation.getFirst());
		if (base == null || other == null || !other.hasChunkAt(BlockPos.of(tag.getLong(TARGET_BASE)))) return false;
		var target = other.getBlockEntity(BlockPos.of(tag.getLong(TARGET_BASE)));
		if (target == null) return false;
		CompoundTag data = base.getPersistentData();
		return data.getBoolean("portalActive") && target.getPersistentData().getBoolean("portalActive")
			&& !CustomPortalTeleportMode.isCoordinateMode(level, base.getBlockPos())
			&& !CustomPortalTeleportMode.isCoordinateMode(other, target.getBlockPos()) && matchesLink(track, data);
	}
	private static boolean matchesLink(TrackBlockEntity track, CompoundTag data) {
		return track.boundLocation != null && data.getBoolean("isLinked")
			&& data.getString("linkedDim").equals(track.boundLocation.getFirst().location().toString())
			&& BlockPos.containing(data.getDouble("linkedX"), data.getDouble("linkedY"), data.getDouble("linkedZ")).asLong() == track.getPersistentData().getLong(TARGET_BASE);
	}
}
