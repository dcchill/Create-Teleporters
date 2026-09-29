package net.createteleporters.client;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomPortalClient {
	private CustomPortalClient() {
	}

	@SubscribeEvent
	@SuppressWarnings("unchecked")
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		BlockEntityType<CustomPortalBaseBlockEntity> type = (BlockEntityType<CustomPortalBaseBlockEntity>)
			(BlockEntityType<?>) CreateteleportersModBlockEntities.CUSTOM_PORTAL_BASE.get();
		event.registerBlockEntityRenderer(type, CustomPortalBaseRenderer::new);
	}
}
