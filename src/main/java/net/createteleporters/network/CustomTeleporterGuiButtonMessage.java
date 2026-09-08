package net.createteleporters.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.createteleporters.procedures.CloseGuiProcedure;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.world.inventory.CustomTeleporterGuiMenu;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.createteleporters.init.CreateteleportersModBlocks;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record CustomTeleporterGuiButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {

	public static final Type<CustomTeleporterGuiButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateteleportersMod.MODID, "custom_teleporter_gui_buttons"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CustomTeleporterGuiButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, CustomTeleporterGuiButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new CustomTeleporterGuiButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
	@Override
	public Type<CustomTeleporterGuiButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final CustomTeleporterGuiButtonMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> handleButtonAction(context.player(), message.buttonID, message.x, message.y, message.z)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		if (world.isClientSide || !(entity.containerMenu instanceof CustomTeleporterGuiMenu menu)
			|| menu.x != x || menu.y != y || menu.z != z || !menu.stillValid(entity)) return;
		BlockPos pos = new BlockPos(x, y, z);
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			CloseGuiProcedure.execute(entity);
		}
		if (buttonID == 1 && entity.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= 64
			&& world.getBlockState(pos).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) {
			CustomPortalTeleportMode.toggle(world, pos);
			menu.broadcastChanges();
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CreateteleportersMod.addNetworkMessage(CustomTeleporterGuiButtonMessage.TYPE, CustomTeleporterGuiButtonMessage.STREAM_CODEC, CustomTeleporterGuiButtonMessage::handleData);
	}
}
