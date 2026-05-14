package net.createteleporters.mixin;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackShape;

import net.createmod.catnip.data.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.createteleporters.init.CreateteleportersModBlocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TrackBlock.class, remap = false)
public abstract class TrackBlockMixin {
	@Inject(method = "connectToPortal", at = @At("HEAD"), cancellable = true, remap = false)
	private void createteleporters$skipAlreadyBoundPortalTrack(ServerLevel level, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (createteleporters$isBoundPortalTrack(level, pos, state)) {
			ci.cancel();
		}
	}

	private static boolean createteleporters$isBoundPortalTrack(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.hasProperty(TrackBlock.HAS_BE) || !state.hasProperty(TrackBlock.SHAPE)) {
			return false;
		}
		if (!state.getValue(TrackBlock.HAS_BE)) {
			return false;
		}

		TrackShape shape = state.getValue(TrackBlock.SHAPE);
		if (!shape.isPortal()) {
			return false;
		}
		if (!(level.getBlockEntity(pos) instanceof TrackBlockEntity trackBlockEntity)) {
			return false;
		}

		Pair<ResourceKey<Level>, BlockPos> boundLocation = trackBlockEntity.boundLocation;
		if (boundLocation == null) {
			return false;
		}

		return level.dimension().equals(boundLocation.getFirst()) || createteleporters$hasAdjacentQuantumPortal(level, pos);
	}

	private static boolean createteleporters$hasAdjacentQuantumPortal(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(direction)).is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get())) {
				return true;
			}
		}

		return false;
	}
}
