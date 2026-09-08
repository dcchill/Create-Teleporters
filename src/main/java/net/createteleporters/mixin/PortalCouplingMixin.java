package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageCouplingRenderer;
import net.createteleporters.integration.train.PortalCarriageState;
import net.createteleporters.integration.train.PortalPath;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CarriageCouplingRenderer.class, remap = false)
public abstract class PortalCouplingMixin {
	@Redirect(method = "renderAll", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Carriage;getDimensional(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"))
	private static Carriage.DimensionalCarriageEntity ctp$localCouplings(Carriage carriage, Level level) {
		var train = carriage.train;
		int index = train.carriages.indexOf(carriage);
		if (index >= 0 && index + 1 < train.carriages.size()) {
			Carriage next = train.carriages.get(index + 1);
			PortalPath.Result gap = PortalPath.between(train.graph, next.getLeadingPoint(), carriage.getTrailingPoint(), train.carriageSpacing.get(index) + 20);
			if (PortalCarriageState.of(carriage).active() || PortalCarriageState.of(next).active() || gap != null && gap.crossing() != null) {
				return PortalCarriageState.of(carriage).hiddenCoupling();
			}
		}
		return carriage.getDimensional(level);
	}
}
