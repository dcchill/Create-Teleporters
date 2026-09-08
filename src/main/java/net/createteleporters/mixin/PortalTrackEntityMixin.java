package net.createteleporters.mixin;

import com.simibubi.create.content.trains.track.TrackBlockEntity;
import net.createteleporters.integration.train.QuantumTrainPortals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TrackBlockEntity.class, remap = false)
public abstract class PortalTrackEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void ctp$maintain(CallbackInfo ci) { QuantumTrainPortals.maintain((TrackBlockEntity) (Object) this); }
	@Inject(method = "lazyTick", at = @At("TAIL"))
	private void ctp$migrate(CallbackInfo ci) { QuantumTrainPortals.migrate((TrackBlockEntity) (Object) this); }
}
