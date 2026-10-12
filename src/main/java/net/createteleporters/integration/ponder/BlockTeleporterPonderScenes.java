package net.createteleporters.integration.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createteleporters.block.BlockTeleporterBlock;
import net.createteleporters.block.entity.BlockTeleporterBlockEntity;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class BlockTeleporterPonderScenes {
	private static void buildWorkshop(CreateSceneBuilder builder, SceneBuildingUtil util) {
		builder.world().setBlock(util.grid().at(0, 1, 2), AllBlocks.CREATIVE_MOTOR.getDefaultState()
			.setValue(BlockStateProperties.FACING, Direction.EAST), false);
		builder.world().setBlock(util.grid().at(1, 1, 2), AllBlocks.COGWHEEL.getDefaultState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
		builder.world().setBlocks(util.select().fromTo(1, 0, 2, 3, 0, 4), Blocks.POLISHED_ANDESITE.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(3, 0, 3), Blocks.YELLOW_CONCRETE.defaultBlockState(), false);
		builder.world().setBlocks(util.select().fromTo(4, 0, 2, 6, 0, 4), Blocks.LIME_CONCRETE.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(5, 0, 3), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(3, 1, 5), Blocks.CRAFTING_TABLE.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(4, 1, 5), Blocks.BARREL.defaultBlockState(), false);
		builder.world().showSection(util.select().fromTo(3, 1, 5, 4, 1, 5), Direction.NORTH);
	}

	public static void setup(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("block_teleporter_setup", "Setting Up a Block Teleporter");
		builder.configureBasePlate(0, 0, 7);
		builder.scaleSceneView(0.85f);
		buildWorkshop(builder, util);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().fromTo(2, 1, 3, 3, 1, 3), Direction.DOWN);
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.GREEN, "source", util.select().position(3, 1, 3), 100);
		builder.overlay().showLine(PonderPalette.BLUE, new Vec3(2.5, 1.5, 3.5), new Vec3(3.5, 1.5, 3.5), 100);
		builder.overlay().showText(100)
			.text("The Block Teleporter moves the single block directly in front of its output face. Orient it toward the block you want to move.")
			.attachKeyFrame().pointAt(new Vec3(3.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showOutline(PonderPalette.GREEN, "arrival_space", util.select().position(5, 1, 3), 100);
		builder.overlay().showControls(new Vec3(5.5, 1, 3.5), Pointing.DOWN, 80)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
		builder.overlay().showOutline(PonderPalette.GREEN, "destination", util.select().position(5, 0, 3), 100);
		builder.overlay().showText(100)
			.text("Right-click with an Advanced TP Link while aiming at a loaded destination block. The moved block will appear in the space above that block, which must be air. Choose a destination in the same dimension.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(100);
		builder.overlay().showLine(PonderPalette.BLUE, new Vec3(5.5, 1.2, 3.5), new Vec3(2.5, 1.5, 3.5), 30);
		builder.idle(12);
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 60).rightClick();
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class,
			base -> base.setItem(0, new ItemStack(CreateteleportersModItems.ADV_TPLINK.get())));
		builder.overlay().showText(100)
			.text("Open the teleporter and insert the saved Advanced TP Link into its slot. Protected blocks such as bedrock and portal controllers cannot be moved.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
	}

	public static void transfer(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("block_teleporter_transfer", "Moving Blocks with Redstone");
		builder.configureBasePlate(0, 0, 7);
		builder.scaleSceneView(0.85f);
		buildWorkshop(builder, util);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().fromTo(2, 1, 3, 3, 1, 3), Direction.DOWN);
		builder.idle(12);
		builder.world().showSection(util.select().fromTo(0, 1, 2, 1, 2, 3), Direction.WEST);
		builder.idle(12);
		builder.world().showSection(util.select().position(2, 1, 2), Direction.NORTH);
		builder.world().showSection(util.select().position(5, 1, 3), Direction.DOWN);
		builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
			tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000)));
		builder.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 1, 1, 2), -32);
		builder.world().setKineticSpeed(util.select().position(1, 1, 3), 32);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class, base -> {
			base.setItem(0, new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
			base.getFluidTank().setFluid(FluidStack.EMPTY);
		});
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.BLUE, "fuel", util.select().fromTo(0, 1, 3, 2, 1, 3), 100);
		builder.overlay().showText(100)
			.text("Supply Telejuice with a powered Mechanical Pump. Moving one block consumes 1,000 mB, or one bucket.")
			.attachKeyFrame().pointAt(new Vec3(1.5, 1.5, 3.5)).placeNearTarget();
		for (int step = 1; step <= 4; step++) {
			int amount = step * 500;
			builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
				tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000 - amount)));
			builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class,
				base -> base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), amount)));
			builder.idle(10);
		}
		builder.idle(80);
		builder.world().setBlock(util.grid().at(5, 1, 3), Blocks.COBBLESTONE.defaultBlockState(), true);
		builder.overlay().showOutline(PonderPalette.RED, "blocked", util.select().position(5, 1, 3), 70);
		builder.overlay().showText(70)
			.text("An occupied destination stops the transfer. Clear the space above the saved block first.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(80);
		builder.world().incrementBlockBreakingProgress(util.grid().at(5, 1, 3));
		builder.idle(10);
		builder.world().destroyBlock(util.grid().at(5, 1, 3));
		builder.overlay().showOutline(PonderPalette.GREEN, "clear", util.select().position(5, 1, 3), 40);
		builder.idle(20);
		builder.overlay().showControls(new Vec3(2.5, 1.5, 2.5), Pointing.DOWN, 60).rightClick();
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.world().modifyBlock(util.grid().at(2, 1, 3), state -> state.setValue(BlockTeleporterBlock.BLOCKSTATE, 2), false);
		builder.effects().indicateRedstone(util.grid().at(2, 1, 2));
		builder.overlay().showText(100)
			.text("Apply redstone power to move the block in front. The destination must be empty and the teleporter must contain its link and enough fuel.")
			.attachKeyFrame().pointAt(new Vec3(3.5, 1.5, 3.5)).placeNearTarget();
		PonderTeleportEffects.blockTransfer(builder, new Vec3(3, 1, 3), new Vec3(5.5, 0.9, 3.5));
		builder.world().setBlock(util.grid().at(3, 1, 3), Blocks.AIR.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(5, 1, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class,
			base -> base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 1000)));
		builder.idle(10);
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.world().modifyBlock(util.grid().at(2, 1, 3), state -> state.setValue(BlockTeleporterBlock.BLOCKSTATE, 1), false);
		builder.effects().indicateSuccess(util.grid().at(5, 1, 3));
		builder.overlay().showOutline(PonderPalette.GREEN, "arrival", util.select().position(5, 1, 3), 80);
		builder.idle(80);
		builder.overlay().showText(100)
			.text("The source block has moved to the destination. Clear the destination and place another source block before the next transfer. While powered, the teleporter keeps checking for another valid move.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(40);
		builder.world().destroyBlock(util.grid().at(5, 1, 3));
		builder.idle(15);
		builder.overlay().showControls(new Vec3(3.5, 2, 3.5), Pointing.DOWN, 40)
			.rightClick().withItem(new ItemStack(Blocks.OAK_LOG));
		builder.world().setBlock(util.grid().at(3, 1, 3), Blocks.OAK_LOG.defaultBlockState(), true);
		builder.idle(20);
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.world().modifyBlock(util.grid().at(2, 1, 3), state -> state.setValue(BlockTeleporterBlock.BLOCKSTATE, 2), false);
		builder.effects().indicateRedstone(util.grid().at(2, 1, 2));
		PonderTeleportEffects.blockTransfer(builder, new Vec3(3, 1, 3), new Vec3(5.5, 0.9, 3.5));
		builder.world().setBlock(util.grid().at(3, 1, 3), Blocks.AIR.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(5, 1, 3), Blocks.OAK_LOG.defaultBlockState(), false);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class,
			base -> base.getFluidTank().setFluid(FluidStack.EMPTY));
		builder.idle(10);
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.world().modifyBlock(util.grid().at(2, 1, 3), state -> state.setValue(BlockTeleporterBlock.BLOCKSTATE, 1), false);
		builder.idle(40);
	}
}
