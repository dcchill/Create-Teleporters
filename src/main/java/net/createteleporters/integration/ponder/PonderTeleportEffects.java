package net.createteleporters.integration.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class PonderTeleportEffects {
	// Match EntityTeleporterOnTickUpdateProcedure, including its saved-link coordinate offsets.
	static void entityCharge(SceneBuilder scene, Vec3 source, Vec3 destination) {
		for (int tick = 1; tick <= 50; tick++) {
			double progress = tick / 50.0;
			var dust = new DustParticleOptions(new Vector3f(1, (float) progress, 0.1f), 1.5f);
			scene.effects().emitParticles(source, (level, x, y, z) -> {
				int count = (int) (8 + progress * 16);
				double radius = 0.5 + progress * 0.5;
				for (int i = 0; i < count; i++) {
					double angle = i * 2 * Math.PI / count;
					level.addParticle(dust, x + 0.5 + Math.cos(angle) * radius, y + 1.2 + progress * 0.8,
						z + 0.5 + Math.sin(angle) * radius, 0, 0, 0);
				}
				for (int i = 0; i < (int) (2 + progress * 6); i++) {
					level.addParticle(dust, destination.x + 0.5 + (level.random.nextDouble() - 0.5) * 0.6,
						destination.y + 1 + level.random.nextDouble() * 0.5,
						destination.z + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
				}
			}, 1, 1);
			scene.idle(1);
		}
	}

	static void entityBurst(SceneBuilder scene, Vec3 origin) {
		Vec3 center = origin.add(0.5, 1, 0.5);
		for (int i = 0; i < 2; i++) {
			var dust = i == 0 ? new DustParticleOptions(new Vector3f(1, 0.9f, 0.3f), 2)
				: new DustParticleOptions(new Vector3f(1, 1, 0.8f), 1.5f);
			double spread = i == 0 ? 0.8 : 1.2;
			double speed = i == 0 ? 0.2 : 0.15;
			scene.effects().emitParticles(center, (level, x, y, z) -> level.addParticle(dust,
				x + (level.random.nextDouble() - 0.5) * spread, y + (level.random.nextDouble() - 0.5) * spread,
				z + (level.random.nextDouble() - 0.5) * spread, level.random.nextGaussian() * speed,
				level.random.nextGaussian() * speed, level.random.nextGaussian() * speed), i == 0 ? 40 : 20, 1);
		}
		burst(scene, ParticleTypes.POOF, center, 30, new Vec3(0.5, 0.5, 0.5), 0.15);
		burst(scene, ParticleTypes.GLOW, center, 25, new Vec3(1, 1, 1), 0.1);
		burst(scene, ParticleTypes.REVERSE_PORTAL, origin.add(0.5, 0.5, 0.5), 20, new Vec3(0.6, 0.3, 0.6), 0.1);
	}

	static void itemTransfer(SceneBuilder scene, Vec3 source, Vec3 destination) {
		burst(scene, ParticleTypes.END_ROD, source, 4, new Vec3(1, 1, 1), 0.01);
		burst(scene, ParticleTypes.END_ROD, destination.add(0, 0.25, 0), 4, new Vec3(0.1, 0.1, 0.1), 0);
	}

	static void blockTransfer(SceneBuilder scene, Vec3 source, Vec3 destination) {
		burst(scene, ParticleTypes.END_ROD, source, 5, new Vec3(0.1, 0.1, 0.1), 0.1);
		burst(scene, ParticleTypes.END_ROD, destination.add(0, 1, 0), 20, new Vec3(1.5, 1.5, 1.5), 0.1);
	}

	private static void burst(SceneBuilder scene, ParticleOptions particle, Vec3 position, int count, Vec3 spread, double speed) {
		// Server particle packets use Gaussian offsets and velocities on the client.
		scene.effects().emitParticles(position, (level, x, y, z) -> level.addParticle(particle,
			x + level.random.nextGaussian() * spread.x, y + level.random.nextGaussian() * spread.y,
			z + level.random.nextGaussian() * spread.z, level.random.nextGaussian() * speed,
			level.random.nextGaussian() * speed, level.random.nextGaussian() * speed), count, 1);
	}

	static void charge(SceneBuilder scene, Vec3 position) {
		for (int i = 0; i < 12; i++) {
			double angle = i * Math.PI / 6;
			Vec3 offset = new Vec3(Math.cos(angle) * 0.8, -0.25, Math.sin(angle) * 0.8);
			scene.effects().emitParticles(position.add(offset),
				scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD, offset.scale(-0.06).add(0, 0.04, 0)), 1, 20);
		}
		scene.effects().emitParticles(position,
			scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.REVERSE_PORTAL, Vec3.ZERO), 3, 20);
	}

	static void trail(SceneBuilder scene, Vec3 from, Vec3 to) {
		scene.overlay().showLine(PonderPalette.BLUE, from, to, 30);
		for (int step = 1; step <= 12; step++) {
			scene.effects().emitParticles(from.lerp(to, step / 12.0),
				scene.effects().simpleParticleEmitter(ParticleTypes.REVERSE_PORTAL, Vec3.ZERO), 2, 6);
			scene.idle(1);
		}
	}

	static void walk(SceneBuilder scene, ElementLink<EntityElement> passenger, Vec3 from, Vec3 to) {
		for (int step = 1; step <= 20; step++) {
			Vec3 position = from.lerp(to, step / 20.0);
			scene.world().modifyEntity(passenger, entity -> entity.setPos(position));
			scene.idle(1);
		}
	}

	static void flash(SceneBuilder scene, Vec3 position) {
		scene.effects().emitParticles(position,
			scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.END_ROD, new Vec3(0, 0.04, 0)), 6, 8);
		scene.effects().emitParticles(position,
			scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.POOF, Vec3.ZERO), 3, 4);
	}
}
