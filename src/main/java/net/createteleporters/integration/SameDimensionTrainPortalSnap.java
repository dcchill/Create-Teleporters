package net.createteleporters.integration;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.graph.TrackEdge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.createteleporters.CreateteleportersMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SameDimensionTrainPortalSnap {
	private static final Map<UUID, Long> LAST_SNAP_TICK = new ConcurrentHashMap<>();
	private static final double MAX_REATTACH_DISTANCE_SQR = 16.0D;

	private SameDimensionTrainPortalSnap() {
	}

	public static List<PointSnapshot> snapshot(Train train) {
		List<PointSnapshot> snapshots = new ArrayList<>();
		if (train == null) {
			return snapshots;
		}

		train.forEachTravellingPoint(point -> snapshots.add(PointSnapshot.of(point)));
		return snapshots;
	}

	public static void snapIfSameDimensionPortalCrossed(Level level, Train train, TrackGraph graph, List<PointSnapshot> before) {
		if (level == null || train == null || graph == null || before == null || before.isEmpty()) {
			return;
		}
		MinecraftServer server = level.getServer();
		if (server == null) {
			return;
		}

		long gameTime = level.getGameTime();
		Long lastSnap = LAST_SNAP_TICK.get(train.id);
		if (lastSnap != null && gameTime - lastSnap < 5L) {
			return;
		}

		PortalCrossing crossing = findCrossing(server, before);
		if (crossing == null) {
			return;
		}

		Vec3 source = crossing.source().getLocation();
		Vec3 target = crossing.target().getLocation();
		Vec3 offset = target.subtract(source);
		int moved = 0;

		for (PointSnapshot snapshot : before) {
			TravellingPoint point = snapshot.point();
			if (!isValid(point) || point == crossing.point()) {
				continue;
			}

			Vec3 currentPos = safePosition(graph, point);
			if (currentPos == null || currentPos.distanceToSqr(source) >= currentPos.distanceToSqr(target)) {
				continue;
			}

			if (reattachToNearestEdge(graph, point, currentPos.add(offset))) {
				moved++;
			}
		}

		if (moved > 0) {
			LAST_SNAP_TICK.put(train.id, gameTime);
			CreateteleportersMod.LOGGER.info("Snapped {} train travelling points through same-dimension portal edge: {} -> {}", moved, crossing.source(), crossing.target());
		}
	}

	private static PortalCrossing findCrossing(MinecraftServer server, List<PointSnapshot> before) {
		for (PointSnapshot snapshot : before) {
			TravellingPoint point = snapshot.point();
			if (!isValid(point)) {
				continue;
			}

			TrackNodeLocation afterNode1 = point.node1.getLocation();
			TrackNodeLocation afterNode2 = point.node2.getLocation();
			if (snapshot.node2() != null && afterNode1 != null
					&& SameDimensionPortalTrackHelper.isSameDimensionPortalEdge(server, snapshot.node2(), afterNode1)) {
				return new PortalCrossing(point, snapshot.node2(), afterNode1);
			}
			if (snapshot.node1() != null && afterNode2 != null
					&& SameDimensionPortalTrackHelper.isSameDimensionPortalEdge(server, snapshot.node1(), afterNode2)) {
				return new PortalCrossing(point, snapshot.node1(), afterNode2);
			}
		}

		return null;
	}

	private static boolean reattachToNearestEdge(TrackGraph graph, TravellingPoint point, Vec3 targetPos) {
		DirectedLocation best = null;
		Vec3 oldDirection = directionOf(point);

		for (TrackNodeLocation fromLocation : graph.getNodes()) {
			TrackNode from = graph.locateNode(fromLocation);
			if (from == null) {
				continue;
			}

			Map<TrackNode, TrackEdge> connections = graph.getConnectionsFrom(from);
			if (connections == null) {
				continue;
			}

			for (Map.Entry<TrackNode, TrackEdge> entry : connections.entrySet()) {
				TrackNode to = entry.getKey();
				TrackEdge edge = entry.getValue();
				if (to == null || edge == null || edge.getLength() <= 0.0D) {
					continue;
				}

				DirectedLocation candidate = closestLocationOnEdge(graph, from, to, edge, targetPos, oldDirection);
				if (candidate != null && (best == null || candidate.score() < best.score())) {
					best = candidate;
				}
			}
		}

		if (best == null || best.distanceSqr() > MAX_REATTACH_DISTANCE_SQR) {
			CreateteleportersMod.LOGGER.warn("Could not reattach same-dimension portal train point near {}", targetPos);
			return false;
		}

		point.node1 = best.from();
		point.node2 = best.to();
		point.edge = best.edge();
		point.position = best.position();
		return true;
	}

	private static DirectedLocation closestLocationOnEdge(TrackGraph graph, TrackNode from, TrackNode to, TrackEdge edge, Vec3 targetPos, Vec3 oldDirection) {
		double length = edge.getLength();
		if (length <= 0.0D) {
			return null;
		}

		DirectedLocation best = null;
		for (int i = 0; i <= 32; i++) {
			double t = i / 32.0D;
			Vec3 pos = edge.getPosition(graph, t);
			double distanceSqr = pos.distanceToSqr(targetPos);
			Vec3 candidateDirection = to.getLocation().getLocation().subtract(from.getLocation().getLocation()).normalize();
			double directionPenalty = oldDirection == null ? 0.0D : Math.max(0.0D, 1.0D - oldDirection.dot(candidateDirection)) * 4.0D;
			double score = distanceSqr + directionPenalty;
			if (best == null || score < best.score()) {
				best = new DirectedLocation(from, to, edge, length * t, distanceSqr, score);
			}
		}

		return best;
	}

	private static Vec3 safePosition(TrackGraph graph, TravellingPoint point) {
		try {
			if (point.edge != null && point.edge.getLength() > 0.0D) {
				return point.getPosition(graph);
			}
		} catch (RuntimeException ignored) {
		}

		if (point.node1 != null) {
			return point.node1.getLocation().getLocation();
		}
		return point.node2 != null ? point.node2.getLocation().getLocation() : null;
	}

	private static Vec3 directionOf(TravellingPoint point) {
		if (point == null || point.node1 == null || point.node2 == null) {
			return null;
		}

		Vec3 direction = point.node2.getLocation().getLocation().subtract(point.node1.getLocation().getLocation());
		return direction.lengthSqr() > 1.0E-6D ? direction.normalize() : null;
	}

	private static boolean isValid(TravellingPoint point) {
		return point != null && point.node1 != null && point.node2 != null;
	}

	public record PointSnapshot(TravellingPoint point, TrackNodeLocation node1, TrackNodeLocation node2) {
		private static PointSnapshot of(TravellingPoint point) {
			return new PointSnapshot(point,
				point != null && point.node1 != null ? point.node1.getLocation() : null,
				point != null && point.node2 != null ? point.node2.getLocation() : null);
		}
	}

	private record PortalCrossing(TravellingPoint point, TrackNodeLocation source, TrackNodeLocation target) {
	}

	private record DirectedLocation(TrackNode from, TrackNode to, TrackEdge edge, double position, double distanceSqr, double score) {
	}
}
