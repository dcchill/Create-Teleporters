package net.createteleporters.integration.train;

import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/** A bounded, directed track walk. World-space distance is meaningless across a portal. */
public final class PortalPath {
	private PortalPath() { }
	public record Crossing(TrackNodeLocation entrance, TrackNodeLocation exit) { }
	public record Result(double distance, Crossing crossing, int portals) { }
	private record Step(TrackNode from, TrackNode to, TrackEdge edge, double start, Crossing crossing, int portals) { }

	public static Result between(TrackGraph graph, TravellingPoint from, TravellingPoint to, double limit) {
		if (graph == null || from.edge == null || to.edge == null || from.node1 == null || to.node1 == null) return null;
		PriorityQueue<Step> queue = new PriorityQueue<>(Comparator.comparingDouble(Step::start));
		Set<TrackEdge> visited = new HashSet<>();
		queue.add(new Step(from.node1, from.node2, from.edge, -from.position, null, 0));
		while (!queue.isEmpty()) {
			Step step = queue.remove();
			if (step.start > limit || !visited.add(step.edge)) continue;
			if (step.from == to.node1 && step.to == to.node2) {
				double distance = step.start + to.position;
				if (distance >= -1e-5 && distance <= limit) return new Result(Math.max(0, distance), step.crossing, step.portals);
			}
			double end = step.start + step.edge.getLength();
			if (end > limit) continue;
			for (var entry : graph.getConnectionsFrom(step.to).entrySet()) {
				if (entry.getKey() == step.from || !step.edge.canTravelTo(entry.getValue())) continue;
				boolean portal = entry.getValue().isInterDimensional();
				Crossing crossing = step.crossing;
				if (portal && crossing == null && PortalNode.connects(step.to.getLocation(), entry.getKey().getLocation())) {
					crossing = new Crossing(step.to.getLocation(), entry.getKey().getLocation());
				}
				queue.add(new Step(step.to, entry.getKey(), entry.getValue(), end, crossing, step.portals + (portal ? 1 : 0)));
			}
		}
		return null;
	}
}
