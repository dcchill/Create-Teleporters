package net.createteleporters.mixin;

import java.lang.ref.WeakReference;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.createteleporters.integration.train.PortalNode;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler;
import net.createteleporters.integration.train.PortalCarriageEntity;
import net.createteleporters.integration.SableAeronauticsIntegration;
import net.minecraft.core.BlockPos;
import java.util.Arrays;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.createteleporters.integration.train.PortalSide;
import net.createteleporters.integration.train.PortalCarriage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Carriage.DimensionalCarriageEntity.class, remap = false)
public abstract class PortalDimensionalCarriageMixin {
	@Shadow @Final private Carriage this$0;
	@Shadow public TrackNodeLocation pivot;
	@Shadow public WeakReference<CarriageContraptionEntity> entity;
	@Unique private CompoundTag ctp$driver;
	@Inject(method = "dismountPlayer", at = @At("HEAD"))
	private void ctp$captureDriver(ServerLevel level, ServerPlayer player, Integer seat, boolean capture,
		CallbackInfo ci) {
		ctp$driver = null;
		if (!capture || pivot == null || this$0.train.graph == null) return;
		var node = this$0.train.graph.locateNode(pivot);
		if (node == null || ((PortalNode) node.getLocation()).ctp$getCounterpart() == null) return;
		var source = entity.get();
		if (source == null || source.getControllingPlayer().filter(player.getUUID()::equals).isEmpty()) return;
		var inputs = ControlsServerHandler.receivedInputs.get(level);
		Object context = inputs.get(player.getUUID());
		var controls = context instanceof PortalControlsContextAccessor value && value.ctp$entity() == source ? value : null;
		var controlsPos = controls != null ? controls.ctp$controls() : ((PortalCarriageEntity) source).ctp$getControls().orElse(null);
		if (controlsPos == null) return;
		ctp$driver = new CompoundTag();
		ctp$driver.putLong("Position", controlsPos.asLong());
		ctp$driver.putIntArray("Keys", controls == null ? new int[0] : controls.ctp$keys().stream().filter(key -> key.getFirst() > 0).mapToInt(key -> key.getSecond()).toArray());
		inputs.remove(player.getUUID());
		source.setControllingPlayer(null);
		((PortalCarriageEntity) source).ctp$setControls(Optional.empty());
	}
	@Inject(method = "dismountPlayer", at = @At("TAIL"))
	private void ctp$storeDriver(ServerLevel level, ServerPlayer player, Integer seat, boolean capture,
		CallbackInfo ci) {
		if (ctp$driver != null) {
			var passenger = ((PortalCarriage) this$0).ctp$passengers().get(seat);
			if (passenger != null) passenger.put("CTPDriver", ctp$driver);
			ctp$driver = null;
		}
	}
	@Redirect(method = "updatePassengerLoadout", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;addSittingPassenger(Lnet/minecraft/world/entity/Entity;I)V"))
	private void ctp$restoreDriver(CarriageContraptionEntity target, Entity passenger, int seat) {
		target.addSittingPassenger(passenger, seat);
		var tag = ((PortalCarriage) this$0).ctp$passengers().get(seat);
		if (!(passenger instanceof ServerPlayer player) || player.getVehicle() != target
			|| tag == null || !tag.contains("CTPDriver") || this$0.train.derailed) return;
		var driver = tag.getCompound("CTPDriver");
		var controls = BlockPos.of(driver.getLong("Position"));
		target.setControllingPlayer(player.getUUID());
		((PortalCarriageEntity) target).ctp$setControls(Optional.of(controls));
		ControlsServerHandler.receivePressed(target.level(), target, controls,
			player.getUUID(), Arrays.stream(driver.getIntArray("Keys")).boxed().toList(), true);
	}
	@Inject(method = "findPivot", at = @At("HEAD"), cancellable = true)
	private void ctp$pivot(ResourceKey<Level> key, boolean leading, CallbackInfoReturnable<TrackNodeLocation> cir) {
		if (PortalSide.isSide(key)) { pivot = PortalSide.pivot(key); cir.setReturnValue(pivot); }
	}
	@Redirect(method = "dismountPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
	private ServerLevel ctp$passengerLevel(MinecraftServer server, ResourceKey<Level> key) { return server.getLevel(PortalSide.dimension(key)); }
	@Redirect(method = "dismountPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"))
	private void ctp$teleportPassenger(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
		player.setXRot(pitch);
		SableAeronauticsIntegration.teleportEntity(player, level, x, y, z, yaw);
	}
	@Redirect(method = "createEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
	private Optional<Entity> ctp$uniquePhysicalEntity(CompoundTag tag, Level level) {
		Optional<Entity> created = EntityType.create(tag, level);
		boolean portalPortion = ((PortalCarriage) this$0).ctp$entities().entrySet().stream()
			.anyMatch(entry -> entry.getValue() == (Object) this && PortalSide.isSide(entry.getKey()));
		if (portalPortion) created.ifPresent(entity -> entity.setUUID(UUID.randomUUID()));
		return created;
	}
}

