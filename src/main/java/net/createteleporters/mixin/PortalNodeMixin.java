package net.createteleporters.mixin;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.createteleporters.integration.train.PortalNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TrackNodeLocation.class, remap = false)
public abstract class PortalNodeMixin implements PortalNode {
	@Unique private TrackNodeLocation ctp$counterpart;
	public TrackNodeLocation ctp$getCounterpart() { return ctp$counterpart; }
	public void ctp$setCounterpart(TrackNodeLocation value) { ctp$counterpart = value; }

	@Inject(method = "write", at = @At("RETURN"))
	private void ctp$write(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
		if (ctp$counterpart != null) {
			// Copy the position alone: writing the paired endpoint would recurse.
			TrackNodeLocation copy = new TrackNodeLocation(ctp$counterpart.getLocation()).in(ctp$counterpart.dimension);
			copy.yOffsetPixels = ctp$counterpart.yOffsetPixels;
			cir.getReturnValue().put("CTPPortal", copy.write(dimensions));
		}
	}
	@Inject(method = "read", at = @At("RETURN"))
	private static void ctp$read(CompoundTag tag, DimensionPalette dimensions, CallbackInfoReturnable<TrackNodeLocation> cir) {
		if (tag.contains("CTPPortal")) {
			TrackNodeLocation other = TrackNodeLocation.read(tag.getCompound("CTPPortal"), dimensions);
			if (other.dimension == null) other.dimension = cir.getReturnValue().dimension;
			((PortalNode) cir.getReturnValue()).ctp$setCounterpart(other);
		}
	}
	@Inject(method = "send", at = @At("TAIL"))
	private void ctp$send(FriendlyByteBuf buf, DimensionPalette dimensions, CallbackInfo ci) {
		buf.writeBoolean(ctp$counterpart != null);
		if (ctp$counterpart != null) {
			CompoundTag tag = ctp$counterpart.write(dimensions);
			tag.remove("CTPPortal");
			buf.writeNbt(tag);
		}
	}
	@Inject(method = "receive", at = @At("RETURN"))
	private static void ctp$receive(FriendlyByteBuf buf, DimensionPalette dimensions, CallbackInfoReturnable<TrackNodeLocation> cir) {
		if (buf.readBoolean()) ((PortalNode) cir.getReturnValue()).ctp$setCounterpart(TrackNodeLocation.read(buf.readNbt(), dimensions));
	}
}
