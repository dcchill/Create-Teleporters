package net.createteleporters.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.block.entity.CustomPortalBaseDummyBlockBlockEntity;

public class CustomPortalBaseDummyBlockBlock extends Block implements EntityBlock {
	public CustomPortalBaseDummyBlockBlock() {
		super(BlockBehaviour.Properties.of()
			.sound(SoundType.METAL)
			.strength(1.5f, 19f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((bs, br, bp) -> false));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get());
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CustomPortalBaseDummyBlockBlockEntity(pos, state);
	}
	
	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
    return true;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
    return true;
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}

	// Optional helper method (not called automatically)
	public static void destroyConnectedDummies(Level world, BlockPos basePos) {
		Direction facing = world.getBlockState(basePos).getValue(BlockStateProperties.HORIZONTAL_FACING);

		BlockPos pos1 = basePos.relative(facing.getClockWise());
		BlockPos pos2 = basePos.relative(facing.getCounterClockWise());

		if (world.getBlockState(pos1).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get()))
			world.destroyBlock(pos1, false);
		if (world.getBlockState(pos2).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get()))
			world.destroyBlock(pos2, false);
	}

	// This runs when the dummy is actually broken by a player
@Override
public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    if (!level.isClientSide) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            long mainX = be.getPersistentData().getLong("main_x");
            long mainY = be.getPersistentData().getLong("main_y");
            long mainZ = be.getPersistentData().getLong("main_z");

            BlockPos mainPos = new BlockPos((int) mainX, (int) mainY, (int) mainZ);
            BlockState mainState = level.getBlockState(mainPos);

            if (mainState.is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) {
    Direction facing = mainState.getValue(BlockStateProperties.HORIZONTAL_FACING);

    BlockPos sideA = mainPos.relative(facing.getClockWise());
    BlockPos sideB = mainPos.relative(facing.getCounterClockWise());

    if (level.getBlockState(sideA).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get()))
        level.destroyBlock(sideA, false);
    if (level.getBlockState(sideB).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get()))
        level.destroyBlock(sideB, false);

    Block.dropResources(mainState, level, mainPos, level.getBlockEntity(mainPos), player, player.getMainHandItem());
    level.removeBlock(mainPos, false);
}

        }
    }

    return super.playerWillDestroy(level, pos, state, player);
}

}
