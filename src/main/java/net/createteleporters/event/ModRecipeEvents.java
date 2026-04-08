package net.createteleporters.event;

import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Event handler that modifies the Custom Portal Base recipe output
 * based on the Portal Binding configuration setting.
 */
public class ModRecipeEvents {
	
	@SubscribeEvent
	public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
		ItemStack crafted = event.getCrafting();
		
		// Check if the crafted item is the Custom Portal Base
		if (crafted.is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get().asItem())) {
			// Check if Portal Binding is disabled
			boolean forcePortalBinding = CTPConfigConfiguration.isCoordinateBindingEnabled();
			
			// When Portal Binding is disabled (forcePortalBinding = false), output 2
			// When Portal Binding is enabled (forcePortalBinding = true), output 1
			if (!forcePortalBinding) {
				crafted.setCount(2);
			}
		}
	}
}
