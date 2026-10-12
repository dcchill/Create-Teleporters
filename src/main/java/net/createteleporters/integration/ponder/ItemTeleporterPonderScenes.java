package net.createteleporters.integration.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createteleporters.block.entity.ItemTPBlockEntity;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class ItemTeleporterPonderScenes {
	public static void setup(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("item_teleporter_setup", "Setting Up an Item Teleporter");
		builder.configureBasePlate(0, 0, 7);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
		builder.idle(20);
		builder.overlay().showControls(new Vec3(5.5, 1, 3.5), Pointing.DOWN, 80)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.TP_LINK.get()));
		builder.overlay().showText(100)
			.text("Aim at a loaded destination block and right-click with a TP Link to save its position. Keep the destination in the same dimension and within the configured Item Teleporter range.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 0.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 60).rightClick();
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
			base -> base.setItem(0, new ItemStack(CreateteleportersModItems.TP_LINK.get())));
		builder.overlay().showText(100)
			.text("Open the Item Teleporter. Put the saved TP Link in the link slot and the items to send in the other slot.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class,
			base -> base.setItem(1, new ItemStack(Items.IRON_INGOT, 64)));
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 60).withItem(new ItemStack(Items.IRON_INGOT, 64));
		builder.overlay().showText(100)
			.text("The contents of the item slot are sent as a stack. They arrive as dropped items, so prepare a collection point at the destination.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1, 3.5)).placeNearTarget();
		builder.idle(120);
	}

	public static void transfer(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("item_teleporter_transfer", "Sending Items with Redstone");
		builder.configureBasePlate(0, 0, 7);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
			tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000)));
		builder.world().setKineticSpeed(util.select().position(1, 1, 3), 32);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class, base -> {
			base.setItem(0, new ItemStack(CreateteleportersModItems.TP_LINK.get()));
			base.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
			base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 1000));
		});
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.BLUE, "fuel", util.select().fromTo(0, 1, 3, 2, 1, 3), 100);
		builder.overlay().showText(100)
			.text("Use a powered Mechanical Pump to supply Telejuice. The fuel cost scales with the item count: a stack of 64 costs 100 mB.")
			.attachKeyFrame().pointAt(new Vec3(1.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showControls(new Vec3(2.5, 1.5, 2.5), Pointing.DOWN, 60).rightClick();
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.overlay().showText(100)
			.text("Apply a redstone pulse to send the loaded stack. The teleporter needs enough fuel and an in-range destination.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 2.5)).placeNearTarget();
		builder.idle(20);
		PonderTeleportEffects.charge(builder, new Vec3(2.5, 1.5, 3.5));
		builder.idle(20);
		PonderTeleportEffects.flash(builder, new Vec3(2.5, 1.5, 3.5));
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), ItemTPBlockEntity.class, base -> {
			base.setItem(1, ItemStack.EMPTY);
			base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 900));
		});
		builder.idle(8);
		PonderTeleportEffects.flash(builder, new Vec3(5.5, 1.2, 3.5));
		builder.world().createItemEntity(new Vec3(5.5, 1.2, 3.5), Vec3.ZERO, new ItemStack(Items.IRON_INGOT, 64));
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.idle(80);
		builder.overlay().showText(100)
			.text("The item slot is now empty and the stack has arrived. Reload the item slot and pulse again for another delivery; no receiving teleporter is needed.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1, 3.5)).placeNearTarget();
		builder.idle(120);
	}
}
