package net.createteleporters.block.entity;

import com.simibubi.create.content.trains.track.TrackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.createteleporters.init.CreateteleportersModBlockEntities;

public class SameDimensionPortalTrackBlockEntity extends TrackBlockEntity {
	public SameDimensionPortalTrackBlockEntity(BlockPos pos, BlockState state) {
		super(CreateteleportersModBlockEntities.SAME_DIMENSION_PORTAL_TRACK.get(), pos, state);
	}
}
