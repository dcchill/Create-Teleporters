package net.createteleporters.integration.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

final class PonderTeleportEffects {
	static void charge(SceneBuilder scene, Vec3 position) {
		scene.effects().emitParticles(position,
			scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.REVERSE_PORTAL, Vec3.ZERO), 3, 20);
	}

	static void flash(SceneBuilder scene, Vec3 position) {
		scene.effects().emitParticles(position,
			scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.END_ROD, new Vec3(0, 0.04, 0)), 6, 8);
		scene.effects().emitParticles(position,
			scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.POOF, Vec3.ZERO), 3, 4);
	}
}
