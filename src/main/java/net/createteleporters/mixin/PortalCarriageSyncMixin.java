package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSyncData;
import net.createteleporters.integration.train.PortalCarriageState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageSyncData.class, remap = false)
public abstract class PortalCarriageSyncMixin {
	@Redirect(method = {"update", "apply"}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Carriage;getDimensional(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"))
	private Carriage.DimensionalCarriageEntity ctp$portion(Carriage target, Level level, CarriageContraptionEntity entity, Carriage carriage) {
		return PortalCarriageState.forEntity(target, entity);
	}
	@Redirect(method = "approach", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Carriage;getDimensional(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"))
	private Carriage.DimensionalCarriageEntity ctp$approachPortion(Carriage target, Level level, CarriageContraptionEntity entity, Carriage carriage, float partial) {
		return PortalCarriageState.forEntity(target, entity);
	}
	@Inject(method = "approach", at = @At("HEAD"), cancellable = true)
	private void ctp$oncePerTick(CarriageContraptionEntity entity, Carriage carriage, float partial, CallbackInfo ci) {
		PortalCarriageState state = PortalCarriageState.of(carriage);
		long tick = entity.level().getGameTime();
		if (state.lastClientTick == tick) ci.cancel();
		else state.lastClientTick = tick;
	}
}
