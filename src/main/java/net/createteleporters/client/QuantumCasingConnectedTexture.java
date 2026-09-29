package net.createteleporters.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class QuantumCasingConnectedTexture {
	private static final CTSpriteShiftEntry SHIFT = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
		ResourceLocation.fromNamespaceAndPath(CreateteleportersMod.MODID, "block/quantum_casing_base"),
		ResourceLocation.fromNamespaceAndPath(CreateteleportersMod.MODID, "block/quantum_casing_connect"));
	private static final ConnectedTextureBehaviour BEHAVIOUR = new SimpleCTBehaviour(SHIFT);

	private QuantumCasingConnectedTexture() {}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(CreateteleportersModBlocks.QUANTUM_CASING.getId(), model -> new CTModel(model, BEHAVIOUR)));
	}
}
