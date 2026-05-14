package net.createteleporters.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModItems;
import net.createteleporters.init.CreateteleportersModTabs;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {
	private CreativeTabEvents() {
	}

	@SubscribeEvent
	public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
		if (event.getTab() != CreateteleportersModTabs.CREATE_TELEPORTERS.get()) {
			return;
		}

		event.accept(new ItemStack(CreateteleportersModItems.SAME_DIMENSION_PORTAL_TRACK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}
}
