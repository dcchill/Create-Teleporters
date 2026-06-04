package net.createteleporters.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.QuantumPortalBlockBlock;
import net.createteleporters.init.CreateteleportersModBlocks;

import net.minecraft.world.item.DyeColor;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class QuantumPortalColorHandler {
	@SubscribeEvent
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		event.register((state, world, pos, tintIndex) -> state.getValue(QuantumPortalBlockBlock.COLOR).getTextureDiffuseColor(),
				CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> DyeColor.PURPLE.getTextureDiffuseColor(),
				CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
	}
}
