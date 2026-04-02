package net.createteleporters.integration;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.createteleporters.CreateteleportersMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = "createteleporters")
public final class CreateTrainPortalBootstrap {

	private CreateTrainPortalBootstrap() {
	}

	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		CreateteleportersMod.LOGGER.info("=== CREATE TRAIN PORTAL BOOTSTRAP CALLED ===");
		CreateteleportersMod.LOGGER.info("Create mod loaded: {}", ModList.get().isLoaded("create"));
		if (!ModList.get().isLoaded("create")) {
			CreateteleportersMod.LOGGER.info("Create mod not loaded, skipping train portal integration");
			return;
		}

		CreateteleportersMod.LOGGER.info("Enqueueing Create train portal integration registration");
		event.enqueueWork(CreateTrainPortalIntegration::register);
	}
}
