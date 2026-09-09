package net.createteleporters.mixin;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import java.util.Collection;
import net.createteleporters.integration.train.QuantumTrainPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TrackBlock.class, remap = false)
public abstract class PortalTrackMixin {
	@Inject(method = "getConnected", at = @At("RETURN"))
	private void ctp$nodes(BlockGetter reader, BlockPos pos, BlockState state, boolean linear, TrackNodeLocation from,
		CallbackInfoReturnable<Collection<TrackNodeLocation.DiscoveredLocation>> cir) {
		if (!linear && reader instanceof ServerLevel level) {
			ServerLevel actual = from == null ? level : level.getServer().getLevel(from.dimension);
			if (actual != null) QuantumTrainPortals.markNodes(actual, pos, state, from, cir.getReturnValue());
		}
	}
	@Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
	private void ctp$finishPassage(BlockState state, Direction direction, BlockState neighbor, LevelAccessor reader, BlockPos pos,
		BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
		if (cir.getReturnValue().isAir() && reader instanceof ServerLevel level
			&& level.getBlockEntity(pos) instanceof TrackBlockEntity track && track.getPersistentData().contains("CTPTrainBase")
			&& QuantumTrainPortals.occupied(level, pos, state)) {
			QuantumTrainPortals.recoverShutdown(level, pos, state);
			if (QuantumTrainPortals.occupied(level, pos, state)) cir.setReturnValue(state);
		}
	}
}
