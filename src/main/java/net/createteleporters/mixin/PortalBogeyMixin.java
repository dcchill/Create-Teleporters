package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createteleporters.integration.train.PortalPath;
import net.createteleporters.integration.train.PortalCarriageState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageBogey.class, remap = false)
public abstract class PortalBogeyMixin {
	@Shadow public Carriage carriage;
	@Unique private long ctp$lastAnglesTick = Long.MIN_VALUE;
	@Inject(method = "updateAngles", at = @At("HEAD"), cancellable = true)
	private void ctp$oneWheelRotation(CarriageContraptionEntity entity, double distance, CallbackInfo ci) {
		if (!PortalCarriageState.of(carriage).active()) return;
		long tick = entity.level().getGameTime();
		if (ctp$lastAnglesTick == tick) ci.cancel();
		else ctp$lastAnglesTick = tick;
	}
	@Inject(method = "getDimension", at = @At("HEAD"), cancellable = true)
	private void ctp$splitBogey(CallbackInfoReturnable<ResourceKey<Level>> cir) {
		if (carriage == null || carriage.train == null) return;
		CarriageBogey bogey = (CarriageBogey) (Object) this;
		PortalPath.Result path = PortalPath.between(carriage.train.graph, bogey.trailing(), bogey.leading(), 20);
		if (path != null && path.crossing() != null) cir.setReturnValue(null);
	}
}
