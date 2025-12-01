/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.createteleporters.client.model.Modelcustom_portal_ring;
import net.createteleporters.client.model.Modelcustom_portal;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class CreateteleportersModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelcustom_portal_ring.LAYER_LOCATION, Modelcustom_portal_ring::createBodyLayer);
		event.registerLayerDefinition(Modelcustom_portal.LAYER_LOCATION, Modelcustom_portal::createBodyLayer);
	}
}