package net.createteleporters.integration.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createteleporters.block.ItemTPBlock;
import net.createteleporters.block.entity.ItemTPBlockEntity;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class ItemTeleporterPonderScenes {
	private static void buildWorkshop(CreateSceneBuilder builder, SceneBuildingUtil util) {
		builder.world().setBlock(util.grid().at(0, 1, 2), AllBlocks.CREATIVE_MOTOR.getDefaultState()
			.setValue(BlockStateProperties.FACING, Direction.EAST), false);
		builder.world().setBlock(util.grid().at(1, 1, 2), AllBlocks.COGWHEEL.getDefaultState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
		builder.world().setBlocks(util.select().fromTo(1, 0, 2, 3, 0, 5), Blocks.POLISHED_ANDESITE.defaultBlockState(), false);
		builder.world().setBlocks(util.select().fromTo(4, 0, 2, 6, 0, 5), Blocks.LIME_CONCRETE.defaultBlockState(), false);
		builder.world().setBlock(util.grid().at(2, 1, 5), AllBlocks.DEPOT.getDefaultState(), false);
		builder.world().setBlock(util.grid().at(5, 1, 3), Blocks.HOPPER.defaultBlockState()
			.setValue(HopperBlock.FACING, Direction.SOUTH), false);
		builder.world().setBlock(util.grid().at(5, 1, 4), Blocks.CHEST.defaultBlockState(), false);
		builder.world().showSection(util.select().position(2, 1, 5), Direction.DOWN);
		builder.world().showSection(util.select().fromTo(5, 1, 3, 5, 1, 4), Direction.WEST);
	}

	private static void loadStack(CreateSceneBuilder builder, SceneBuildingUtil util) {
		builder.world().createItemOnBeltLike(util.grid().at(2, 1, 5), Direction.UP, new ItemStack(Items.IRON_INGOT, 64));
		builder.overlay().showControls(new Vec3(2.5, 2, 5.5), Pointing.DOWN, 35).rightClick();
		builder.idle(30);
		builder.world().removeItemsFromBelt(util.grid().at(2, 1, 5));
		var stack = builder.world().createItemEntity(new Vec3(2.5, 2, 5.5), Vec3.ZERO, new ItemStack(Items.IRON_INGOT, 64));
		builder.world().modifyEntity(stack, entity -> entity.setNoGravity(true));
		PonderTeleportEffects.walk(builder, stack, new Vec3(2.5, 2, 5.5), new Vec3(2.5, 2, 3.5));
		builder.world().modifyEntity(stack, entity -> entity.discard());
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 40)
			.rightClick().withItem(new ItemStack(Items.IRON_INGOT, 64));
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
			base -> base.setItem(1, new ItemStack(Items.IRON_INGOT, 64)));
	}

	public static void setup(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("item_teleporter_setup", "Setting Up an Item Teleporter");
		builder.configureBasePlate(0, 0, 7);
		builder.scaleSceneView(0.85f);
		buildWorkshop(builder, util);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.GREEN, "arrival_space", util.select().position(5, 2, 3), 100);
		builder.overlay().showControls(new Vec3(5.5, 2, 3.5), Pointing.DOWN, 80)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.TP_LINK.get()));
		builder.overlay().showText(100)
			.text("Aim at a loaded destination block and right-click with a TP Link to save its position. Keep the destination in the same dimension and within the configured Item Teleporter range.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(100);
		builder.overlay().showLine(PonderPalette.BLUE, new Vec3(5.5, 2.2, 3.5), new Vec3(2.5, 1.5, 3.5), 30);
		builder.idle(12);
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 60).rightClick();
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
			base -> base.setItem(0, new ItemStack(CreateteleportersModItems.TP_LINK.get())));
		builder.overlay().showText(100)
			.text("Open the Item Teleporter. Put the saved TP Link in the link slot and the items to send in the other slot.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		loadStack(builder, util);
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 60).withItem(new ItemStack(Items.IRON_INGOT, 64));
		builder.overlay().showText(100)
			.text("Take a stack from storage and put it in the item slot. Items arrive above the saved block: a hopper can collect them into a chest.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 2, 3.5)).placeNearTarget();
		builder.idle(120);
	}

	public static void transfer(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("item_teleporter_transfer", "Sending Items with Redstone");
		builder.configureBasePlate(0, 0, 7);
		builder.scaleSceneView(0.85f);
		buildWorkshop(builder, util);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().fromTo(2, 1, 3, 3, 1, 3), Direction.DOWN);
		builder.idle(12);
		builder.world().showSection(util.select().fromTo(0, 1, 2, 1, 2, 3), Direction.WEST);
		builder.idle(12);
		builder.world().showSection(util.select().position(2, 1, 2), Direction.NORTH);
		builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
			tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000)));
		builder.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 1, 1, 2), -32);
		builder.world().setKineticSpeed(util.select().position(1, 1, 3), 32);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class, base -> {
			base.setItem(0, new ItemStack(CreateteleportersModItems.TP_LINK.get()));
			base.getFluidTank().setFluid(FluidStack.EMPTY);
		});
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.BLUE, "fuel", util.select().fromTo(0, 1, 3, 2, 1, 3), 100);
		builder.overlay().showText(100)
			.text("Use a powered Mechanical Pump to supply Telejuice. The fuel cost scales with the item count: a stack of 64 costs 100 mB.")
			.attachKeyFrame().pointAt(new Vec3(1.5, 1.5, 3.5)).placeNearTarget();
		for (int step = 1; step <= 4; step++) {
			int amount = step * 250;
			builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
				tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000 - amount)));
			builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
				base -> base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), amount)));
			builder.idle(10);
		}
		builder.idle(80);
		loadStack(builder, util);
		builder.overlay().showControls(new Vec3(2.5, 1.5, 2.5), Pointing.DOWN, 60).rightClick();
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.world().modifyBlock(util.grid().at(2, 1, 3), state -> state.setValue(ItemTPBlock.BLOCKSTATE, 3), false);
		builder.effects().indicateRedstone(util.grid().at(2, 1, 2));
		builder.overlay().showText(100)
			.text("Apply a redstone pulse to send the loaded stack. The teleporter needs enough fuel and an in-range destination.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 2.5)).placeNearTarget();
		PonderTeleportEffects.itemTransfer(builder, new Vec3(2, 1, 3), new Vec3(5.5, 1.9, 3.5));
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
			base -> base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 900)));
		builder.idle(5);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
			base -> base.setItem(1, ItemStack.EMPTY));
		var delivered = builder.world().createItemEntity(new Vec3(5.5, 1.9, 3.5), Vec3.ZERO, new ItemStack(Items.IRON_INGOT, 64));
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.world().modifyBlock(util.grid().at(2, 1, 3), state -> state.setValue(ItemTPBlock.BLOCKSTATE, 1), false);
		builder.effects().indicateSuccess(util.grid().at(5, 1, 3));
		builder.overlay().showOutline(PonderPalette.GREEN, "arrival", util.select().position(5, 1, 3), 80);
		builder.idle(30);
		builder.world().modifyEntity(delivered, entity -> entity.discard());
		builder.world().modifyBlockEntity(util.grid().at(5, 1, 4), ChestBlockEntity.class,
			chest -> chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64)));
		builder.overlay().showLine(PonderPalette.GREEN, new Vec3(5.5, 1.5, 3.5), new Vec3(5.5, 1.5, 4.5), 50);
		builder.overlay().showControls(new Vec3(5.5, 2, 4.5), Pointing.DOWN, 50).withItem(new ItemStack(Items.IRON_INGOT, 64));
		builder.idle(50);
		builder.overlay().showText(100)
			.text("The hopper collects the delivered stack into the chest. Reload the item slot and pulse again; no receiving teleporter is needed.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 2, 3.5)).placeNearTarget();
		builder.idle(120);
	}
}
