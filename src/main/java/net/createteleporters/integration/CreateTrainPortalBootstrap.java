package net.createteleporters.integration;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.configuration.CTPConfigConfiguration;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = "createteleporters")
public final class CreateTrainPortalBootstrap {

	private CreateTrainPortalBootstrap() {
	}

	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		if (!CTPConfigConfiguration.EXPERIMENTAL_TRAIN_TELEPORTATION.get()) {
			CreateteleportersMod.LOGGER.info("Experimental train teleportation is disabled");
			return;
		}
		if (!ModList.get().isLoaded("create")) {
			CreateteleportersMod.LOGGER.info("Create mod not loaded, skipping train portal integration");
			return;
		}

		CreateteleportersMod.LOGGER.warn("Experimental train teleportation is enabled");
		event.enqueueWork(CreateTrainPortalIntegration::register);
	}
}
