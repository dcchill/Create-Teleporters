package net.createteleporters.integration.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.minecraft.core.Direction;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

public class CreateTeleportersPonderScenes {
	public static void customPortalPage1(SceneBuilder scene, SceneBuildingUtil util) {
		renderWholeSchematic(scene, util, "c_portal_1", "Portal Controller Setup");
	}

	public static void customPortalPage2(SceneBuilder scene, SceneBuildingUtil util) {
		renderWholeSchematic(scene, util, "c_portal_2", "Building a Square Portal Frame");
	}

	public static void customPortalPage3(SceneBuilder scene, SceneBuildingUtil util) {
		renderWholeSchematic(scene, util, "c_portal_3", "Activating and Linking the Portal");
	}

	private static void renderWholeSchematic(SceneBuilder scene, SceneBuildingUtil util, String sceneId, String title) {
		CreateSceneBuilder builder = new CreateSceneBuilder(scene);
		builder.title(sceneId, title);
		builder.configureBasePlate(0, 0, 5);
		builder.world().showSection(util.select().layer(0), Direction.UP);
		builder.idle(10);
		builder.world().showSection(util.select().layersFrom(1), Direction.DOWN);
		builder.idle(40);
	}
}
