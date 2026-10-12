package net.createteleporters.integration.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createteleporters.block.EntityTeleporterBlock;
import net.createteleporters.block.entity.EntityTeleporterBlockEntity;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class EntityTeleporterPonderScenes {
	public static void setup(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("entity_teleporter_setup", "Setting an Entity Teleporter Destination");
		builder.configureBasePlate(0, 0, 7);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
		builder.idle(20);
		builder.overlay().showText(100)
			.text("Entity Teleporters send players and mobs to a saved location. The full, slab and quarter-height versions use the same setup.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showOutline(PonderPalette.GREEN, "destination", util.select().position(5, 0, 3), 100);
		builder.overlay().showControls(new Vec3(5.5, 1, 3.5), Pointing.DOWN, 80)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.TP_LINK.get()));
		builder.overlay().showText(100)
			.text("Aim at a loaded block at your destination and right-click with a regular TP Link to save the arrival position. Leave room above it for safe arrival.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 0.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showControls(new Vec3(2.5, 2, 3.5), Pointing.DOWN, 60).rightClick();
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), EntityTeleporterBlockEntity.class,
			base -> base.setItem(0, new ItemStack(CreateteleportersModItems.TP_LINK.get())));
		builder.overlay().showText(100)
			.text("Open the teleporter and insert the saved TP Link into its slot. No receiving teleporter is required.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showText(100)
			.text("Keep the destination in the same dimension and within the configured Entity Teleporter range. Check the distance status in the teleporter screen.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
	}

	public static void travel(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("entity_teleporter_travel", "Fueling and Using an Entity Teleporter");
		builder.configureBasePlate(0, 0, 7);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
		builder.idle(20);
		builder.world().showSection(util.select().fromTo(0, 1, 2, 1, 2, 3), Direction.UP);
		builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
			tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000)));
		builder.world().setKineticSpeed(util.select().position(1, 1, 3), 32);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), EntityTeleporterBlockEntity.class, base -> {
			base.setItem(0, new ItemStack(CreateteleportersModItems.TP_LINK.get()));
			base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 1000));
		});
		builder.overlay().showOutline(PonderPalette.BLUE, "fuel", util.select().fromTo(0, 1, 3, 2, 1, 3), 100);
		builder.overlay().showText(100)
			.text("With a saved TP Link inserted, pump Telejuice into the teleporter. Each activation needs and consumes 250 mB.")
			.attachKeyFrame().pointAt(new Vec3(1.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		var passenger = builder.world().createEntity(level -> {
			Pig pig = new Pig(EntityType.PIG, level);
			pig.setNoAi(true);
			pig.setNoGravity(true);
			pig.moveTo(2.5, 2, 3.5, 90, 0);
			return pig;
		});
		builder.world().modifyBlock(util.grid().at(2, 1, 3),
			state -> state.setValue(EntityTeleporterBlock.BLOCKSTATE, 2), false);
		builder.overlay().showText(100)
			.text("Stand on the teleporter, or guide a mob onto it, and wait for it to charge. Dropped items do not start charging.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 2.5, 3.5)).placeNearTarget();
		builder.idle(100);
		PonderTeleportEffects.charge(builder, new Vec3(2.5, 2.4, 3.5));
		builder.idle(20);
		PonderTeleportEffects.flash(builder, new Vec3(2.5, 2.4, 3.5));
		builder.world().modifyEntity(passenger, entity -> entity.setInvisible(true));
		builder.idle(8);
		PonderTeleportEffects.flash(builder, new Vec3(5.5, 1.4, 3.5));
		builder.world().modifyEntity(passenger, entity -> {
			entity.moveTo(5.5, 1, 3.5, 90, 0);
			entity.setInvisible(false);
		});
		builder.world().modifyBlock(util.grid().at(2, 1, 3),
			state -> state.setValue(EntityTeleporterBlock.BLOCKSTATE, 1), false);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), EntityTeleporterBlockEntity.class,
			base -> base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 750)));
		builder.overlay().showText(100)
			.text("When charging completes, nearby entities arrive at the saved location. Keep the teleporter supplied with Telejuice for the next trip.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
	}
}
