package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;

import net.createmod.catnip.data.Couple;
import net.createteleporters.integration.SameDimensionPortalTrackHelper;
import net.createteleporters.integration.SameDimensionTrainPortalSnap;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = Carriage.class, remap = false)
public abstract class CarriageMixin {
	@Shadow
	public Train train;

	@Unique
	private List<SameDimensionTrainPortalSnap.PointSnapshot> createteleporters$beforeTravel;

	@Inject(method = "travel", at = @At("HEAD"), remap = false)
	private void createteleporters$snapshotBeforeSameDimensionPortal(Level level, TrackGraph graph, double distance, TravellingPoint leading,
			TravellingPoint trailing, int type, CallbackInfoReturnable<Double> cir) {
		createteleporters$beforeTravel = SameDimensionTrainPortalSnap.snapshot(train);
	}

	@Inject(method = "travel", at = @At("RETURN"), remap = false)
	private void createteleporters$snapAfterSameDimensionPortal(Level level, TrackGraph graph, double distance, TravellingPoint leading,
			TravellingPoint trailing, int type, CallbackInfoReturnable<Double> cir) {
		SameDimensionTrainPortalSnap.snapIfSameDimensionPortalCrossed(level, train, graph, createteleporters$beforeTravel);
		createteleporters$beforeTravel = null;
	}

	@Inject(method = "lambda$travel$8", at = @At("HEAD"), cancellable = true, remap = false)
	private void createteleporters$allowSameDimensionPortal(Couple<TrackNodeLocation> portalNodes, CallbackInfoReturnable<Boolean> cir) {
		if (portalNodes == null) {
			return;
		}

		TrackNodeLocation first = portalNodes.getFirst();
		TrackNodeLocation second = portalNodes.getSecond();
		if (SameDimensionPortalTrackHelper.isSameDimensionPortalEdge(ServerLifecycleHooks.getCurrentServer(), first, second)) {
			SameDimensionPortalTrackHelper.logPortalEdgeMarked("CarriagePortal", first, second);
			cir.setReturnValue(false);
		}
	}
}
