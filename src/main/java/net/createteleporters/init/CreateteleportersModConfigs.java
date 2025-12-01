package net.createteleporters.init;

import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;

import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.CreateteleportersMod;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CreateteleportersModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() -> {
			ModList.get().getModContainerById("createteleporters").get().registerConfig(ModConfig.Type.COMMON, CTPConfigConfiguration.SPEC, "createteleportersconfig.toml");
		});
	}
}