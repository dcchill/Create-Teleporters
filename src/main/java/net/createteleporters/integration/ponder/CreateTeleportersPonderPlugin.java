package net.createteleporters.integration.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;

import net.minecraft.resources.ResourceLocation;

import net.createteleporters.init.CreateteleportersModBlocks;

public class CreateTeleportersPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return "createteleporters";
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		helper.forComponents(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.getId()).addStoryBoard("c_portal_1", CreateTeleportersPonderScenes::customPortalPage1)
				.addStoryBoard("c_portal_2", CreateTeleportersPonderScenes::customPortalPage2)
				.addStoryBoard("c_portal_3", CreateTeleportersPonderScenes::customPortalPage3);
	}
}
