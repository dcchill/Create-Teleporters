package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.CarriageContraptionVisual;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CarriageContraptionVisual.class, remap = false)
public abstract class PortalCarriageVisualMixin {
	@Redirect(method = "checkCarriage", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction$AxisDirection;getStep()I"))
	private static int ctp$movementAxisSign(Direction.AxisDirection direction) {
		// Portal cutoffs use movement-relative coordinates: the rear bogey is always at -spacing.
		return -1;
	}
}
