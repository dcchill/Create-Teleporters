package net.createteleporters.client;

import net.createteleporters.CreateteleportersMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, value = Dist.CLIENT)
public final class QuantumCasingResources {
	@SubscribeEvent
	public static void addFusionResources(AddPackFindersEvent event) {
		if (ModList.get().isLoaded("fusion")) {
			event.addPackFinders(
					ResourceLocation.fromNamespaceAndPath(CreateteleportersMod.MODID, "resourcepacks/fusion"),
					PackType.CLIENT_RESOURCES,
					Component.literal("Create Teleporters Connected Textures"),
					PackSource.BUILT_IN,
					true,
					Pack.Position.TOP);
		}
	}
}
