package net.createteleporters.block;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.BlockPos;

import net.createteleporters.configuration.CTPConfigConfiguration;

public class PocketdblockBlock extends Block {
	public PocketdblockBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(1.5f, Float.MAX_VALUE).lightLevel(s -> 3).noLootTable());
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, world, pos, oldState, moving);
		if (!world.isClientSide()) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be != null) {
				be.getPersistentData().putBoolean("dimTaken", true);
			}
		}
	}

	@Override
	public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return CTPConfigConfiguration.ALLOW_POCKETDBLOCK_BREAKING.get() ? super.getDestroyProgress(state, player, level, pos) : 0.0f;
	}
}