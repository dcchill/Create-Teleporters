package net.createteleporters.mixin;

import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createteleporters.integration.train.PortalCarriageEntity;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public abstract class PortalDriverClientMixin {
	@Unique private boolean ctp$resumedControls;
	@Inject(method = "tickContraption", at = @At("TAIL"))
	private void ctp$resumeDriver(CallbackInfo ci) {
		var entity = (CarriageContraptionEntity) (Object) this;
		if (!entity.level().isClientSide) return;
		var controls = ((PortalCarriageEntity) entity).ctp$getControls();
		if (controls.isEmpty() || entity.getControllingPlayer().isEmpty()) { ctp$resumedControls = false; return; }
		var player = Minecraft.getInstance().player;
		if (!ctp$resumedControls && player != null && player.getVehicle() == entity
			&& entity.getControllingPlayer().filter(player.getUUID()::equals).isPresent()) {
			if (ControlsHandler.getContraption() != entity) ControlsHandler.startControlling(entity, controls.get());
			ctp$resumedControls = true;
		}
	}
}
