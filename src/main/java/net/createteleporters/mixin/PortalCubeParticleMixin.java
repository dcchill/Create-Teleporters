package net.createteleporters.mixin;

import com.simibubi.create.content.trains.CubeParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = CubeParticle.class, remap = false)
public abstract class PortalCubeParticleMixin {
	@Shadow protected boolean hot;

	@ModifyArg(method = "render", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 3)
	private float ctp$portalOpacity(float alpha) {
		// Hot cubes are locomotive smoke; only the portal effect becomes translucent.
		return hot ? alpha : alpha * 0.8f;
	}
}
