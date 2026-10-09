package net.createteleporters.client;

import com.simibubi.create.CreateClient;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.createteleporters.init.CreateteleportersModBlocks;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomPortalClient {
	private CustomPortalClient() {
	}

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.getId(), LiquidPortalModel::new));
	}

	@SubscribeEvent
	@SuppressWarnings("unchecked")
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		BlockEntityType<CustomPortalBaseBlockEntity> type = (BlockEntityType<CustomPortalBaseBlockEntity>)
			(BlockEntityType<?>) CreateteleportersModBlockEntities.CUSTOM_PORTAL_BASE.get();
		event.registerBlockEntityRenderer(type, CustomPortalBaseRenderer::new);
	}
}
