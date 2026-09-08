package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.createteleporters.integration.train.PortalCarriageState;
import net.createteleporters.integration.train.PortalPath;
import net.createteleporters.integration.train.PortalSide;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Train.class, remap = false)
public abstract class PortalTrainMixin {
	@Shadow public List<Carriage> carriages;
	@Shadow public List<Integer> carriageSpacing;
	@Shadow public TrackGraph graph;
	@Unique private final Map<TrackNode, ResourceKey<Level>> ctp$gapSides = new IdentityHashMap<>();
	@Unique private Carriage ctp$collisionCarriage;
	@Inject(method = "tick", at = @At("HEAD"))
	private void ctp$prepare(Level level, CallbackInfo ci) {
		ctp$gapSides.clear();
		for (Carriage carriage : carriages) PortalCarriageState.of(carriage).prepare();
		for (int i = 1; i < carriages.size(); i++) {
			TravellingPoint rear = carriages.get(i).getLeadingPoint();
			TravellingPoint front = carriages.get(i - 1).getTrailingPoint();
			PortalPath.Result gap = PortalPath.between(graph, rear, front, carriageSpacing.get(i - 1) + 20);
			if (gap != null && gap.crossing() != null) {
				ctp$gapSides.put(rear.node1, PortalSide.key(gap.crossing().entrance()));
				ctp$gapSides.put(front.node1, PortalSide.key(gap.crossing().exit()));
			}
		}
	}
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph/TrackNode;getLocation()Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;"))
	private TrackNodeLocation ctp$gapLocation(TrackNode node) {
		TrackNodeLocation actual = node.getLocation();
		ResourceKey<Level> side = ctp$gapSides.get(node);
		if (side == null) return actual;
		TrackNodeLocation location = new TrackNodeLocation(actual.getLocation()).in(side);
		location.yOffsetPixels = actual.yOffsetPixels;
		return location;
	}
	@Inject(method = "collideWithOtherTrains", at = @At("HEAD"), cancellable = true)
	private void ctp$noCrossPortalCollisionLine(Level level, Carriage carriage, CallbackInfo ci) {
		// Match Create's inter-dimension collision rule: never cast a line between portal ends.
		if (PortalCarriageState.of(carriage).active()) ci.cancel();
	}
	@Redirect(method = "findCollidingTrain", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Carriage;getLeadingPoint()Lcom/simibubi/create/content/trains/entity/TravellingPoint;"))
	private TravellingPoint ctp$collisionCarriage(Carriage carriage) {
		ctp$collisionCarriage = carriage;
		return carriage.getLeadingPoint();
	}
	@Redirect(method = "findCollidingTrain", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph/TrackNode;getLocation()Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;"))
	private TrackNodeLocation ctp$collisionLocation(TrackNode node) {
		return ctp$collisionCarriage == null ? node.getLocation() : PortalCarriageState.of(ctp$collisionCarriage).location(node);
	}
}
