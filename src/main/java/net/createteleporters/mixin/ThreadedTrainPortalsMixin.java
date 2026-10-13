package net.createteleporters.mixin;

import net.createteleporters.integration.train.QuantumTrainPortals;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.mrjulsen.ctt.CreateThreadedTrains", remap = false)
public abstract class ThreadedTrainPortalsMixin {
	@Inject(method = "preTick", at = @At("HEAD"))
	private static void ctp$preparePortalChecks(MinecraftServer server, CallbackInfo ci) {
		QuantumTrainPortals.prepareThreadedEntrances(server);
	}
}
