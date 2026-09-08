package net.createteleporters.mixin;

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
	@Inject(method = "findPivot", at = @At("HEAD"), cancellable = true)
	private void ctp$pivot(ResourceKey<Level> key, boolean leading, CallbackInfoReturnable<TrackNodeLocation> cir) {
		if (PortalSide.isSide(key)) { pivot = PortalSide.pivot(key); cir.setReturnValue(pivot); }
	}
	@Redirect(method = "dismountPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
	private ServerLevel ctp$passengerLevel(MinecraftServer server, ResourceKey<Level> key) { return server.getLevel(PortalSide.dimension(key)); }
	@Redirect(method = "createEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
	private Optional<Entity> ctp$uniquePhysicalEntity(CompoundTag tag, Level level) {
		Optional<Entity> created = EntityType.create(tag, level);
		boolean portalPortion = ((PortalCarriage) this$0).ctp$entities().entrySet().stream()
			.anyMatch(entry -> entry.getValue() == (Object) this && PortalSide.isSide(entry.getKey()));
		if (portalPortion) created.ifPresent(entity -> entity.setUUID(UUID.randomUUID()));
		return created;
	}
}
