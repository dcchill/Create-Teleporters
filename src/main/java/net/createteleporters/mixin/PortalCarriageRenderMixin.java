package net.createteleporters.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntityRenderer;
import net.createteleporters.integration.train.PortalCarriageState;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Shared by the ordinary renderer and Flywheel's carriage visual. */
@Mixin(value = CarriageContraptionEntityRenderer.class, remap = false)
public abstract class PortalCarriageRenderMixin {
	@Redirect(method = "translateBogey", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/animation/LerpedFloat;getValue(F)F", ordinal = 0))
	private static float ctp$localYaw(LerpedFloat angle, float partial, PoseStack poses, CarriageBogey bogey, int spacing, float yaw, float pitch, float frame) {
		return PortalCarriageState.of(bogey.carriage).active() && bogey.getDimension() == null ? 90 - yaw : angle.getValue(partial);
	}
	@Redirect(method = "translateBogey", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/animation/LerpedFloat;getValue(F)F", ordinal = 1))
	private static float ctp$localPitch(LerpedFloat angle, float partial, PoseStack poses, CarriageBogey bogey, int spacing, float yaw, float pitch, float frame) {
		return PortalCarriageState.of(bogey.carriage).active() && bogey.getDimension() == null ? 0 : angle.getValue(partial);
	}
	@Redirect(method = "getBogeyLightCoords", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/CarriageBogey;getAnchorPosition()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 ctp$localLighting(CarriageBogey bogey, CarriageContraptionEntity entity, CarriageBogey ignored, float partial) {
		return PortalCarriageState.of(bogey.carriage).active() && bogey.getDimension() == null ? entity.getLightProbePosition(partial) : bogey.getAnchorPosition();
	}
}
