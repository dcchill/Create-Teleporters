package net.createteleporters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.trains.entity.Carriage;
import net.createteleporters.integration.train.PortalCarriageState;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.george_vi.electroenergetics.simulation.infrastructure.CatenaryModule", remap = false)
public abstract class ElectroEnergeticsPortalMixin {
	@WrapOperation(method = "buildCircuit", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/trains/entity/Carriage;presentInMultipleDimensions()Z"))
	private boolean ctp$checkBothPortalSides(Carriage carriage, Operation<Boolean> original) {
		return original.call(carriage) && !PortalCarriageState.of(carriage).active();
	}

	@WrapOperation(method = "buildCircuit", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/phys/Vec3;atLowerCornerOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 ctp$pantographFrame(Vec3i pos, Operation<Vec3> original, @Local Carriage carriage,
		@Local(ordinal = 0) LocalFloatRef yaw, @Local(ordinal = 1) LocalFloatRef pitch,
		@Local(ordinal = 2) LocalRef<Vec3> pivot) {
		var state = PortalCarriageState.of(carriage);
		if (state.active()) {
			for (var portion : state.entities().values()) {
				// CEE rotates every pantograph into the same local X axis used by Create's portal cutoff.
				double seam = -carriage.bogeySpacing - 1 + (carriage.bogeySpacing + 2)
					* (portion.cutoff > 0 ? portion.cutoff : portion.cutoff + 1);
				if (portion.cutoff > 0 ? pos.getX() < seam : pos.getX() >= seam) continue;
				Vec3 front = portion.rotationAnchors.getFirst();
				Vec3 rear = portion.rotationAnchors.getSecond();
				if (front == null || rear == null) continue;
				Vec3 diff = front.subtract(rear);
				yaw.set((float) (Mth.atan2(diff.z, diff.x) * Mth.RAD_TO_DEG) + 180);
				pitch.set((float) - (Mth.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)) * Mth.RAD_TO_DEG));
				pivot.set(carriage.isOnTwoBogeys() ? front : VecHelper.lerp(0.5f, front, rear));
				break;
			}
		}
		return original.call(pos);
	}
}
