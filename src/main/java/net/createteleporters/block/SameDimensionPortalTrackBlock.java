package net.createteleporters.block;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import net.createteleporters.block.entity.SameDimensionPortalTrackBlockEntity;
import net.createteleporters.init.CreateteleportersModBlockEntities;
import net.createteleporters.init.CreateteleportersTrackMaterials;

public class SameDimensionPortalTrackBlock extends TrackBlock {
	public SameDimensionPortalTrackBlock() {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_GRAY)
				.strength(0.8F)
				.sound(SoundType.WOOD)
				.noOcclusion(), CreateteleportersTrackMaterials.SAME_DIMENSION_PORTAL);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (!state.getValue(HAS_BE)) {
			return null;
		}

		return new SameDimensionPortalTrackBlockEntity(pos, state);
	}

	@SuppressWarnings("unchecked")
	@Override
	public BlockEntityType<? extends TrackBlockEntity> getBlockEntityType() {
		return (BlockEntityType<? extends TrackBlockEntity>) CreateteleportersModBlockEntities.SAME_DIMENSION_PORTAL_TRACK.get();
	}
}
