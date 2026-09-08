package net.createteleporters.integration.train;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;

/** Metadata belongs to an endpoint, not to every edge touching that endpoint. */
public interface PortalNode {
	TrackNodeLocation ctp$getCounterpart();
	void ctp$setCounterpart(TrackNodeLocation counterpart);

	static boolean connects(TrackNodeLocation first, TrackNodeLocation second) {
		return first instanceof PortalNode a && second instanceof PortalNode b
			&& second.equals(a.ctp$getCounterpart()) && first.equals(b.ctp$getCounterpart());
	}
}
