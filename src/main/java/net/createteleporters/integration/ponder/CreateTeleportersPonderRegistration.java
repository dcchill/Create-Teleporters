package net.createteleporters.integration.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.createteleporters.CreateteleportersMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateTeleportersPonderRegistration {
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		if (!ModList.get().isLoaded("ponder")) {
			return;
		}
		event.enqueueWork(() -> PonderIndex.addPlugin(new CreateTeleportersPonderPlugin()));
	}
}
