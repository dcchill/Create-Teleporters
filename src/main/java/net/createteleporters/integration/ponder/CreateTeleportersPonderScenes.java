package net.createteleporters.integration.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.minecraft.core.Direction;
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
		builder.idle(20);
		
		// Show the portal controller block
		builder.world().showSection(util.select().position(2, 1, 2), Direction.UP);
		builder.idle(20);
		
		builder.overlay().showText(80)
			.text("The Custom Portal Controller is the heart of your teleportation system.\n" +
			      "Place this block adjacent to where you want your portal frame to appear.\n" +
			      "It will manage the activation and linking of your custom portals.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(40);
		
		builder.overlay().showText(80)
			.text("Right-click the controller with a Portal Linking Card to configure\n" +
			      "the destination. Each controller must be uniquely placed - you cannot\n" +
			      "have two controllers at the same location.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(40);
		
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.idle(60);
	}

	private static void renderPortalFrameBuilding(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("c_portal_2", "Building a Square Portal Frame");
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.idle(20);
		
		builder.overlay().showText(80)
			.text("Portal frames must be built in a valid square or rectangular shape.\n" +
			      "The minimum size is 3x3 blocks, and the maximum is 21x21 blocks.\n" +
			      "The frame must be constructed from valid portal frame materials.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(40);
		
		// Show frame construction
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.idle(60);
		
		builder.overlay().showText(80)
			.text("Ensure the interior of the frame is completely empty - no blocks,\n" +
			      "fluids, or entities. The portal will fill this space automatically\n" +
			      "when activated. Corners can be left empty for decorative purposes.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(40);
	}

	private static void renderPortalActivationAndLinking(SceneBuilder scene, SceneBuildingUtil util) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title("c_portal_3", "Activating and Linking the Portal");
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.idle(20);
		
		builder.overlay().showText(80)
			.text("To activate the portal, use a Portal Activator item on the controller.\n" +
			      "The portal will ignite and display a swirling particle effect.\n" +
			      "A valid portal requires both a controller and a proper frame.")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(40);
		
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.idle(60);
		
		builder.overlay().showText(100)
			.text("To link two portals together:\n" +
			      "1. Right-click the source controller with a Linking Card\n" +
			      "2. Walk to the destination and right-click its controller\n" +
			      "3. Both portals will now be linked for bidirectional travel\n\n" +
			      "Stepping through a linked portal will teleport you instantly!")
			.attachKeyFrame()
			.pointAt(new Vec3(2.5, 1.5, 2.5))
			.placeNearTarget();
		builder.idle(40);
	}
}
