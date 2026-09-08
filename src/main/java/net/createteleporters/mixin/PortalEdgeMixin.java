package net.createteleporters.mixin;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import net.createteleporters.integration.train.PortalNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TrackEdge.class, remap = false)
public abstract class PortalEdgeMixin {
	@Shadow public TrackNode node1;
	@Shadow public TrackNode node2;
	@Inject(method = "isInterDimensional", at = @At("HEAD"), cancellable = true)
	private void ctp$portalEdge(CallbackInfoReturnable<Boolean> cir) {
		if (PortalNode.connects(node1.getLocation(), node2.getLocation())) cir.setReturnValue(true);
	}
}
