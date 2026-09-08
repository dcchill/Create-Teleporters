package net.createteleporters.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createteleporters.integration.train.PortalCarriageEntity;
import net.createteleporters.integration.train.PortalCarriageState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public abstract class PortalCarriageEntityMixin implements PortalCarriageEntity {
	@Unique private static final EntityDataAccessor<String> CTP_SIDE = SynchedEntityData.defineId(CarriageContraptionEntity.class, EntityDataSerializers.STRING);
	public String ctp$getSide() { return ((CarriageContraptionEntity) (Object) this).getEntityData().get(CTP_SIDE); }
	public void ctp$setSide(String side) { ((CarriageContraptionEntity) (Object) this).getEntityData().set(CTP_SIDE, side); }
	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void ctp$define(SynchedEntityData.Builder builder, CallbackInfo ci) { builder.define(CTP_SIDE, ""); }
	@Inject(method = "writeAdditional", at = @At("TAIL"))
	private void ctp$write(CompoundTag tag, HolderLookup.Provider registries, boolean spawnPacket, CallbackInfo ci) { tag.putString("CTPPortalSide", ctp$getSide()); }
	@Inject(method = "readAdditional", at = @At("TAIL"))
	private void ctp$read(CompoundTag tag, boolean spawnPacket, CallbackInfo ci) { ctp$setSide(tag.getString("CTPPortalSide")); }
	@Redirect(method = {"tickContraption", "bindCarriage", "setCarriage", "onClientRemoval", "tickArrivalSound"},
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Carriage;getDimensional(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"))
	private Carriage.DimensionalCarriageEntity ctp$portion(Carriage carriage, Level level) {
		return PortalCarriageState.forEntity(carriage, (CarriageContraptionEntity) (Object) this);
	}
}
