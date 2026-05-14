package net.createteleporters.mixin;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.BezierConnection;

import net.createteleporters.integration.SameDimensionPortalTrackHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;

@Mixin(value = TrackGraph.class, remap = false)
public abstract class TrackGraphMixin {
	@Shadow
	public abstract TrackNode locateNode(TrackNodeLocation location);

	@Shadow
	public abstract Map<TrackNode, TrackEdge> getConnectionsFrom(TrackNode node);

	@Inject(method = "connectNodes", at = @At("RETURN"), remap = false)
	private void createteleporters$markSameDimensionPortalEdge(LevelAccessor level, TrackNodeLocation.DiscoveredLocation first,
			TrackNodeLocation.DiscoveredLocation second, BezierConnection turn, CallbackInfo ci) {
		if (turn != null || !(level instanceof ServerLevel sourceLevel)) {
			return;
		}
		if (first == null || second == null) {
			return;
		}
		if (!Objects.equals(first.getDimension(), second.getDimension())) {
			return;
		}
		if (!SameDimensionPortalTrackHelper.isSameDimensionPortalEdge(sourceLevel.getServer(), first, second)) {
			return;
		}

		TrackNode firstNode = locateNode(first);
		TrackNode secondNode = locateNode(second);
		if (firstNode == null || secondNode == null) {
			return;
		}

		createteleporters$markEdgeInterDimensional(firstNode, secondNode);
		createteleporters$markEdgeInterDimensional(secondNode, firstNode);
		SameDimensionPortalTrackHelper.logPortalEdgeMarked("TrackGraph", first, second);
	}

	private void createteleporters$markEdgeInterDimensional(TrackNode from, TrackNode to) {
		Map<TrackNode, TrackEdge> connections = getConnectionsFrom(from);
		if (connections == null) {
			return;
		}

		TrackEdge edge = connections.get(to);
		if (edge instanceof TrackEdgeAccessor accessor) {
			accessor.createteleporters$setInterDimensional(true);
		}
	}
}
