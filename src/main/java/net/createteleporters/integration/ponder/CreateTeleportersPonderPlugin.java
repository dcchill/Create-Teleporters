package net.createteleporters.integration.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.registries.BuiltInRegistries;

import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.init.CreateteleportersModItems;

public class CreateTeleportersPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return "createteleporters";
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		helper.forComponents(
				BuiltInRegistries.BLOCK.getKey(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get()),
				BuiltInRegistries.BLOCK.getKey(CreateteleportersModBlocks.QUANTUM_CASING.get()),
				BuiltInRegistries.ITEM.getKey(CreateteleportersModItems.ADV_TPLINK.get()))
				.addStoryBoard("c_portal_1", CreateTeleportersPonderScenes::customPortalPage1)
				.addStoryBoard("c_portal_2", CreateTeleportersPonderScenes::customPortalPage2)
				.addStoryBoard("c_portal_3", CreateTeleportersPonderScenes::customPortalPage3)
				.addStoryBoard("c_portal_3", CreateTeleportersPonderScenes::customPortalCoordinates);

		helper.forComponents(
				BuiltInRegistries.BLOCK.getKey(CreateteleportersModBlocks.TELEPORTER.get()),
				BuiltInRegistries.BLOCK.getKey(CreateteleportersModBlocks.ENTITY_TELEPORTER_SLAB.get()),
				BuiltInRegistries.BLOCK.getKey(CreateteleportersModBlocks.ENTITY_TELEPORTER_QUARTER.get()))
				.addStoryBoard("entity_teleporter", EntityTeleporterPonderScenes::setup)
				.addStoryBoard("entity_teleporter", EntityTeleporterPonderScenes::travel);
	}
}
