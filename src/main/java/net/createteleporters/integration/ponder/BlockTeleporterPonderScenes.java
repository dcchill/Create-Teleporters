package net.createteleporters.integration.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createteleporters.block.entity.BlockTeleporterBlockEntity;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class BlockTeleporterPonderScenes {
	public static void setup(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("block_teleporter_setup", "Setting Up a Block Teleporter");
		builder.configureBasePlate(0, 0, 7);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().fromTo(2, 1, 3, 3, 1, 3), Direction.DOWN);
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.GREEN, "source", util.select().position(3, 1, 3), 100);
		builder.overlay().showText(100)
			.text("The Block Teleporter moves the single block directly in front of its output face. Orient it toward the block you want to move.")
			.attachKeyFrame().pointAt(new Vec3(3.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showControls(new Vec3(5.5, 1, 3.5), Pointing.DOWN, 80)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
		builder.overlay().showOutline(PonderPalette.GREEN, "destination", util.select().position(5, 1, 3), 100);
		builder.overlay().showText(100)
			.text("Right-click with an Advanced TP Link while aiming at a loaded destination block. The moved block will appear in the space above that block, which must be air. Choose a destination in the same dimension.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
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
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.world().modifyBlockEntity(util.grid().at(0, 1, 3), FluidTankBlockEntity.class,
			tank -> tank.getTankInventory().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 4000)));
		builder.world().setKineticSpeed(util.select().position(1, 1, 3), 32);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class, base -> {
			base.setItem(0, new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
			base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 2000));
		});
		builder.idle(20);
		builder.overlay().showOutline(PonderPalette.BLUE, "fuel", util.select().fromTo(0, 1, 3, 2, 1, 3), 100);
		builder.overlay().showText(100)
			.text("Supply Telejuice with a powered Mechanical Pump. Moving one block consumes 1,000 mB, or one bucket.")
			.attachKeyFrame().pointAt(new Vec3(1.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showControls(new Vec3(2.5, 1.5, 2.5), Pointing.DOWN, 60).rightClick();
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.overlay().showText(100)
			.text("Apply redstone power to move the block in front. The destination must be empty and the teleporter must contain its link and enough fuel.")
			.attachKeyFrame().pointAt(new Vec3(3.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(20);
		PonderTeleportEffects.charge(builder, new Vec3(3.5, 1.5, 3.5));
		builder.idle(20);
		PonderTeleportEffects.flash(builder, new Vec3(3.5, 1.5, 3.5));
		builder.world().setBlock(util.grid().at(3, 1, 3), Blocks.AIR.defaultBlockState(), false);
		builder.idle(8);
		PonderTeleportEffects.flash(builder, new Vec3(5.5, 1.5, 3.5));
		builder.world().setBlock(util.grid().at(5, 1, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), BlockTeleporterBlockEntity.class,
			base -> base.getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 1000)));
		builder.world().toggleRedstonePower(util.select().position(2, 1, 2));
		builder.idle(80);
		builder.overlay().showText(100)
			.text("The source block has moved to the destination. Clear the destination and place another source block before the next transfer. While powered, the teleporter keeps checking for another valid move.")
			.attachKeyFrame().pointAt(new Vec3(5.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
	}
}
