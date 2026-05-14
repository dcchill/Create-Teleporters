package net.createteleporters.mixin;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.createteleporters.integration.SameDimensionPortalTrackHelper;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = TrackEdge.class, remap = false)
public abstract class TrackEdgeMixin {
	@Shadow
	public TrackNode node1;

	@Shadow
	public TrackNode node2;

	@Shadow
	boolean interDimensional;

	@Inject(method = "isInterDimensional", at = @At("HEAD"), cancellable = true, remap = false)
	private void createteleporters$treatSameDimensionPortalEdgeAsInterDimensional(CallbackInfoReturnable<Boolean> cir) {
		if (interDimensional || node1 == null || node2 == null) {
			return;
		}

		TrackNodeLocation first = node1.getLocation();
		TrackNodeLocation second = node2.getLocation();
		if (first == null || second == null || !Objects.equals(first.getDimension(), second.getDimension())) {
			return;
		}

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (SameDimensionPortalTrackHelper.isSameDimensionPortalEdge(server, first, second)) {
			interDimensional = true;
			SameDimensionPortalTrackHelper.logPortalEdgeMarked("TrackEdge", first, second);
			cir.setReturnValue(true);
		}
	}
}
