package net.createteleporters.integration.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.world.phys.Vec3;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

public class CreateTeleportersPonderScenes {
	public static void customPortalPage1(SceneBuilder scene, SceneBuildingUtil util) {
		renderPortalControllerSetup(scene, util);
	}

	public static void customPortalPage2(SceneBuilder scene, SceneBuildingUtil util) {
		renderPortalFrameBuilding(scene, util);
	}

	public static void customPortalPage3(SceneBuilder scene, SceneBuildingUtil util) {
		renderPortalActivationAndLinking(scene, util);
	}

	private static void renderPortalControllerSetup(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("c_portal_1", "Portal Controller Setup");
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.idle(30);
		
		
		builder.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 2), Direction.UP);
		builder.idle(30);
		
		builder.overlay().showText(100)
			.text("The Custom Portal Controller is the heart of your teleportation system.\n" +
			      "Place it in the bottom row, leaving one free block on each side.\n" +
			      "Placing it automatically adds the two side blocks.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(120);
		
		builder.overlay().showText(100)
			.text("Set both controllers to Portal to Portal mode.\n" +
			      "Right-click each controller with the same Advanced TP Link\n" +
			      "to pair them.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(100);
		
		builder.world().showSection(util.select().fromTo(0, 1, 2, 0, 5, 2), Direction.DOWN);
		builder.world().showSection(util.select().fromTo(4, 1, 2, 4, 5, 2), Direction.DOWN);
		builder.world().showSection(util.select().fromTo(1, 5, 2, 3, 5, 2), Direction.DOWN);
		builder.idle(80);
	}

	private static void renderPortalFrameBuilding(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("c_portal_2", "Building a Portal Frame");
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.idle(20);
		
		
		builder.world().showSection(util.select().fromTo(0, 1, 3, 4, 1, 3), Direction.UP);
		builder.idle(20);
		builder.world().showSection(util.select().fromTo(0, 2, 3, 0, 4, 3), Direction.DOWN);
		builder.world().showSection(util.select().fromTo(4, 2, 3, 4, 4, 3), Direction.DOWN);
		builder.idle(20);
		builder.world().showSection(util.select().fromTo(0, 5, 3, 4, 5, 3), Direction.DOWN);
		builder.idle(20);
		
		builder.overlay().showText(100)
			.text("Portal frames must be built in a valid square or rectangular shape.\n" +
			      "The opening can be 3 to 21 blocks wide and 3 to 21 blocks tall.\n" +
			      "Use Quantum Casings and place the base anywhere along the bottom,\n" +
			      "with room for a dummy block on each side.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 3.5))
			.placeNearTarget();
		builder.idle(100);
		
		builder.overlay().showOutline(PonderPalette.GREEN, "opening", util.select().fromTo(1, 2, 3, 3, 4, 3), 100);
		builder.overlay().showText(100)
			.text("Ensure the interior of the frame is empty of blocks and fluids.\n" +
			      "The portal will fill this space automatically\n" +
			      "when activated. Keep the entire frame, including its corners, intact.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 3.5))
			.placeNearTarget();
		builder.idle(100);
	}

	private static void renderPortalActivationAndLinking(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("c_portal_3", "Activating and Linking the Portal");
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.idle(20);
		
		
		builder.world().setBlocks(util.select().fromTo(1, 2, 3, 3, 4, 3), Blocks.AIR.defaultBlockState(), false);
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), CustomPortalBaseBlockEntity.class,
			base -> base.getPersistentData().putBoolean("portalVisualActive", false));
		builder.world().showSection(util.select().fromTo(0, 1, 3, 4, 5, 3), Direction.DOWN);
		builder.idle(20);
		builder.world().showSection(util.select().fromTo(0, 1, 0, 2, 3, 2), Direction.UP);
		builder.world().setKineticSpeed(util.select().fromTo(0, 1, 0, 1, 1, 1), 32);
		builder.idle(40);
		builder.overlay().showOutline(PonderPalette.BLUE, "fuel", util.select().fromTo(0, 1, 1, 2, 3, 2), 100);
		
		builder.overlay().showText(100)
			.text("Pump at least 256 mB of Telejuice into each portal base.\n" +
			      "Place one Advanced TP Link in either portal: one supplies the pair.\n" +
			      "Both frames must be valid for the pair to activate.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 3.5))
			.placeNearTarget();
		builder.idle(100);
		
		builder.overlay().showControls(new Vec3(2.5, 2, 3), Pointing.DOWN, 60)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
		builder.overlay().showText(120)
			.text("To link two portals together:\n" +
			      "1. Set both controllers to Portal to Portal mode\n" +
			      "2. Right-click both with the same Advanced TP Link\n" +
			      "3. Insert one link in either portal and fuel both bases\n\n" +
			      "Normal travel consumes Telejuice at the departure portal.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 3.5))
			.placeNearTarget();
		builder.idle(120);
		builder.world().restoreBlocks(util.select().fromTo(1, 2, 3, 3, 4, 3));
		builder.world().modifyBlockEntity(util.grid().at(2, 1, 3), CustomPortalBaseBlockEntity.class, base -> {
			var data = base.getPersistentData();
			data.putBoolean("portalVisualActive", true);
			data.putString("rotation", "north");
			data.putInt("portalMinExtent", -2);
			data.putInt("portalMaxExtent", 2);
			data.putInt("portalHeight", 4);
		});
		builder.overlay().showText(80)
			.text("The portal activates automatically when both frames, fuel and the link are ready.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 3.5, 3.5)).placeNearTarget();
		builder.idle(100);
	}

	public static void customPortalCoordinates(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("c_portal_coordinates", "Teleporting to Coordinates");
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.idle(30);
		builder.overlay().showControls(new Vec3(2.5, 2, 3), Pointing.DOWN, 60).rightClick();
		builder.overlay().showText(100)
			.text("Open the controller and select Coordinates mode to travel to a saved location without a second portal.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(120);
		builder.overlay().showControls(new Vec3(4.5, 1, 0.5), Pointing.DOWN, 80)
			.rightClick().withItem(new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
		builder.overlay().showText(120)
			.text("At your destination, aim at a loaded block and right-click with an Advanced TP Link to save its position and dimension. Choose a safe arrival spot.")
			.attachKeyFrame().pointAt(new Vec3(4.5, 0.5, 0.5)).placeNearTarget();
		builder.idle(140);
		builder.overlay().showControls(new Vec3(2.5, 2, 3), Pointing.DOWN, 60)
			.withItem(new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
		builder.overlay().showText(120)
			.text("Insert the saved link into the controller's slot and supply at least 256 mB of Telejuice. Step through to reach the saved destination; travel consumes fuel here.")
			.attachKeyFrame().pointAt(new Vec3(2.5, 1.5, 3.5)).placeNearTarget();
		builder.idle(140);
	}
}
