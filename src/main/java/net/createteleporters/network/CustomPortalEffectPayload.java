package net.createteleporters.network;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.client.CustomPortalEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record CustomPortalEffectPayload(byte phase, int color, int duration) implements CustomPacketPayload {
	public static final byte CHARGE = 0;
	public static final byte CANCEL = 1;
	public static final byte ARRIVAL = 2;
	public static final Type<CustomPortalEffectPayload> TYPE = new Type<>(
		ResourceLocation.fromNamespaceAndPath(CreateteleportersMod.MODID, "custom_portal_effect"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CustomPortalEffectPayload> STREAM_CODEC = StreamCodec.of(
		(buffer, message) -> {
			buffer.writeByte(message.phase);
			buffer.writeInt(message.color);
			buffer.writeVarInt(message.duration);
		}, buffer -> new CustomPortalEffectPayload(buffer.readByte(), buffer.readInt(), buffer.readVarInt()));

	@Override
	public Type<CustomPortalEffectPayload> type() {
		return TYPE;
	}

	public static void handleData(CustomPortalEffectPayload message, IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND)
			context.enqueueWork(() -> CustomPortalEffects.accept(message));
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CreateteleportersMod.addNetworkMessage(TYPE, STREAM_CODEC, CustomPortalEffectPayload::handleData);
	}
}
