package net.createteleporters.integration;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class CreateTrainPortalBootstrap {

	private CreateTrainPortalBootstrap() {
	}

	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		if (!ModList.get().isLoaded("create")) {
			return;
		}

		event.enqueueWork(CreateTrainPortalIntegration::register);
	}
}
