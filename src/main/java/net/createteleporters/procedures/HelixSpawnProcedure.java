package net.createteleporters.procedures;

import org.joml.Vector3f;


import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.createteleporters.block.BlockTeleporterBlock;

public class HelixSpawnProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {

        // === CONFIG ===
double height = 2.0;
double radius = 0.75;
int turns = 4;
double speed = 0.015;

// Redstone particle needs a color (R, G, B) and scale
float r = 0.9f;
float g = 0.1f;
float b = 0.1f;
float scale = 1.0f;

// Construct the dust particle options
ParticleOptions particle = new DustParticleOptions(new Vector3f(r, g, b), scale);

        // Only run on server
        if (world instanceof ServerLevel level) {
            long gameTime = level.getGameTime();
            double time = (gameTime % (int)(height / speed)) * speed;

            // Block center
            double centerX = x + 0.5;
            double centerY = y + 0.5;
            double centerZ = z + 0.5;

            // Get block facing direction
            Direction dir = blockstate.getValue(BlockTeleporterBlock.FACING);
            double dx = dir.getStepX();
            double dy = dir.getStepY();
            double dz = dir.getStepZ();

            // First helix strand
            double angle1 = (time / height) * turns * 2 * Math.PI;
            double px1 = centerX + dx * time + Math.cos(angle1) * radius;
            double py1 = centerY + dy * time + Math.sin(angle1) * radius;
            double pz1 = centerZ + dz * time;

            // Second helix strand (180° offset)
            double angle2 = angle1 + Math.PI;
            double px2 = centerX + dx * time + Math.cos(angle2) * radius;
            double py2 = centerY + dy * time + Math.sin(angle2) * radius;
            double pz2 = centerZ + dz * time;

            // Spawn both helices
            level.sendParticles(particle, px1, py1, pz1, 1, 0, 0, 0, 0);
            level.sendParticles(particle, px2, py2, pz2, 1, 0, 0, 0, 0);
        }
    }
}