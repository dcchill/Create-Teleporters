/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.createteleporters.client.gui.ItemTeleporterGuiScreen;
import net.createteleporters.client.gui.EntityTeleporterGuiScreen;
import net.createteleporters.client.gui.CustomTeleporterGuiScreen;
import net.createteleporters.client.gui.BlockTeleporterGuiScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateteleportersModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(CreateteleportersModMenus.ENTITY_TELEPORTER_GUI.get(), EntityTeleporterGuiScreen::new);
		event.register(CreateteleportersModMenus.ITEM_TELEPORTER_GUI.get(), ItemTeleporterGuiScreen::new);
		event.register(CreateteleportersModMenus.CUSTOM_TELEPORTER_GUI.get(), CustomTeleporterGuiScreen::new);
		event.register(CreateteleportersModMenus.BLOCK_TELEPORTER_GUI.get(), BlockTeleporterGuiScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}