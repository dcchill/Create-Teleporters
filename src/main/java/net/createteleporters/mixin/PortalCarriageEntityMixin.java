package net.createteleporters.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.CubeParticleData;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createteleporters.block.QuantumPortalBlockBlock;
import net.createteleporters.integration.train.PortalCarriageEntity;
import net.createteleporters.integration.train.PortalCarriageState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public abstract class PortalCarriageEntityMixin implements PortalCarriageEntity {
	@Redirect(method = "spawnPortalParticles", at = @At(value = "NEW", target = "com/simibubi/create/content/trains/CubeParticleData"))
	private CubeParticleData ctp$portalParticleColor(float red, float green, float blue, float scale, int age, boolean hot,
		Carriage.DimensionalCarriageEntity portion) {
		Level level = ((CarriageContraptionEntity) (Object) this).level();
		// The pivot borders the track and the portal on this carriage portion's side.
		for (BlockPos pos : portion.pivot.allAdjacent()) {
			var state = level.getBlockState(pos);
			if (state.getBlock() instanceof QuantumPortalBlockBlock) {
				int color = state.getValue(QuantumPortalBlockBlock.COLOR).getTextureDiffuseColor();
				return new CubeParticleData((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f,
					(color & 255) / 255f, scale, age, hot);
			}
		}
		return new CubeParticleData(red, green, blue, scale, age, hot);
	}
	@Inject(method = "startControlling", at = @At("RETURN"))
	private void ctp$rememberControls(BlockPos pos, Player player,
		CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) ctp$setControls(Optional.of(pos));
	}
	@Unique private static final EntityDataAccessor<Optional<BlockPos>> CTP_CONTROLS = SynchedEntityData.defineId(CarriageContraptionEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	public Optional<BlockPos> ctp$getControls() { return ((CarriageContraptionEntity) (Object) this).getEntityData().get(CTP_CONTROLS); }
	public void ctp$setControls(Optional<BlockPos> controls) { ((CarriageContraptionEntity) (Object) this).getEntityData().set(CTP_CONTROLS, controls); }
	@Unique private static final EntityDataAccessor<String> CTP_SIDE = SynchedEntityData.defineId(CarriageContraptionEntity.class, EntityDataSerializers.STRING);
	public String ctp$getSide() { return ((CarriageContraptionEntity) (Object) this).getEntityData().get(CTP_SIDE); }
	public void ctp$setSide(String side) { ((CarriageContraptionEntity) (Object) this).getEntityData().set(CTP_SIDE, side); }
	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void ctp$define(SynchedEntityData.Builder builder, CallbackInfo ci) { builder.define(CTP_SIDE, ""); builder.define(CTP_CONTROLS, Optional.empty()); }
	@Inject(method = "writeAdditional", at = @At("TAIL"))
	private void ctp$write(CompoundTag tag, HolderLookup.Provider registries, boolean spawnPacket, CallbackInfo ci) { tag.putString("CTPPortalSide", ctp$getSide()); }
	@Inject(method = "readAdditional", at = @At("TAIL"))
	private void ctp$read(CompoundTag tag, boolean spawnPacket, CallbackInfo ci) { ctp$setSide(tag.getString("CTPPortalSide")); }
	@Inject(method = "tickContraption", at = @At("TAIL"))
	private void ctp$clearStaleController(CallbackInfo ci) {
		CarriageContraptionEntity entity = (CarriageContraptionEntity) (Object) this;
		if (!(entity.level() instanceof ServerLevel level) || entity.getControllingPlayer().isEmpty()) return;
		var playerId = entity.getControllingPlayer().get();
		var player = level.getServer().getPlayerList().getPlayer(playerId);
		if (player != null && player.getVehicle() == entity) return;
		ControlsServerHandler.receivedInputs.get(level).remove(playerId);
		entity.setControllingPlayer(null);
		ctp$setControls(Optional.empty());
	}
	@Redirect(method = {"tickContraption", "bindCarriage", "setCarriage", "onClientRemoval", "tickArrivalSound"},
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Carriage;getDimensional(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"))
	private Carriage.DimensionalCarriageEntity ctp$portion(Carriage carriage, Level level) {
		return PortalCarriageState.forEntity(carriage, (CarriageContraptionEntity) (Object) this);
	}
}
