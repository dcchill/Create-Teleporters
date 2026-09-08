package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import java.util.List;
import java.util.Map;
import net.createteleporters.integration.train.PortalCarriage;
import net.createteleporters.integration.train.PortalCarriageState;
import net.createteleporters.integration.train.PortalSide;
import net.createteleporters.integration.train.QuantumTrainPortals;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Carriage.class, remap = false)
public abstract class PortalCarriageMixin implements PortalCarriage {
	@Shadow private Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> entities;
	@Shadow public Train train;
	@Unique private PortalCarriageState ctp$state;
	public Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> ctp$entities() { return entities; }
	public PortalCarriageState ctp$state() {
		if (ctp$state == null) ctp$state = new PortalCarriageState((Carriage) (Object) this);
		return ctp$state;
	}
	@Inject(method = "updateContraptionAnchors", at = @At("HEAD"))
	private void ctp$prepare(CallbackInfo ci) { ctp$state().prepare(); }
	@Redirect(method = {"updateContraptionAnchors", "getAnchorDiff", "lambda$manageEntities$11"},
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph/TrackNode;getLocation()Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;"))
	private TrackNodeLocation ctp$location(TrackNode node) { return ctp$state().location(node); }
	@Redirect(method = "updateContraptionAnchors",
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/CarriageBogey;getDimension()Lnet/minecraft/resources/ResourceKey;"))
	private ResourceKey<Level> ctp$bogeySide(CarriageBogey bogey) { return ctp$state().bogeyDimension(bogey); }
	@Redirect(method = "manageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
	private ServerLevel ctp$realLevel(MinecraftServer server, ResourceKey<Level> key) { return server.getLevel(PortalSide.dimension(key)); }
	@Inject(method = "getPresentDimensions", at = @At("HEAD"), cancellable = true)
	private void ctp$dimensions(CallbackInfoReturnable<List<ResourceKey<Level>>> cir) {
		if (entities.keySet().stream().anyMatch(PortalSide::isSide)) cir.setReturnValue(entities.keySet().stream().map(PortalSide::dimension).distinct().toList());
	}
	@Inject(method = "getDimensional(Lnet/minecraft/resources/ResourceKey;)Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;", at = @At("HEAD"), cancellable = true)
	private void ctp$existingPortion(ResourceKey<Level> dimension, CallbackInfoReturnable<Carriage.DimensionalCarriageEntity> cir) {
		if (PortalSide.isSide(dimension) || entities.containsKey(dimension)) return;
		for (var entry : entities.entrySet()) {
			if (PortalSide.isSide(entry.getKey()) && PortalSide.dimension(entry.getKey()).equals(dimension)) {
				cir.setReturnValue(entry.getValue());
				return;
			}
		}
	}
	@Inject(method = "lambda$travel$8", at = @At("HEAD"), cancellable = true)
	private void ctp$entryGate(Couple<TrackNodeLocation> nodes, CallbackInfoReturnable<Boolean> cir) {
		if (!QuantumTrainPortals.canEnter(train, nodes.getFirst()) || !QuantumTrainPortals.canEnter(train, nodes.getSecond())) cir.setReturnValue(true);
	}
	@Inject(method = "pivoted", at = @At("HEAD"), cancellable = true)
	private void ctp$wheelOnPlane(Carriage.DimensionalCarriageEntity dce, ResourceKey<Level> dimension, TravellingPoint start,
		double offset, boolean leadingUpsideDown, boolean trailingUpsideDown, CallbackInfoReturnable<Vec3> cir) {
		if (!PortalSide.isSide(dimension) || train.graph == null || start.edge == null) return;
		Vec3 pivot = PortalSide.pivot(dimension).getLocation().add(0, leadingUpsideDown ? -1 : 1, 0);
		boolean leading = start == ((Carriage) (Object) this).getLeadingPoint();
		Vec3 point = start.getPosition(train.graph, !leading && leadingUpsideDown != trailingUpsideDown);
		if (point.distanceToSqr(pivot) < 1e-10) {
			dce.pivot = PortalSide.pivot(dimension);
			cir.setReturnValue(point.add(start.edge.getDirection(leading).scale(leading ? -offset : offset)));
		}
	}
}
