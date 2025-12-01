/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import net.createteleporters.world.inventory.ItemTeleporterGuiMenu;
import net.createteleporters.world.inventory.EntityTeleporterGuiMenu;
import net.createteleporters.world.inventory.CustomTeleporterGuiMenu;
import net.createteleporters.world.inventory.BlockTeleporterGuiMenu;
import net.createteleporters.network.MenuStateUpdateMessage;
import net.createteleporters.CreateteleportersMod;

import java.util.Map;

public class CreateteleportersModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, CreateteleportersMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<EntityTeleporterGuiMenu>> ENTITY_TELEPORTER_GUI = REGISTRY.register("entity_teleporter_gui", () -> IMenuTypeExtension.create(EntityTeleporterGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ItemTeleporterGuiMenu>> ITEM_TELEPORTER_GUI = REGISTRY.register("item_teleporter_gui", () -> IMenuTypeExtension.create(ItemTeleporterGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<CustomTeleporterGuiMenu>> CUSTOM_TELEPORTER_GUI = REGISTRY.register("custom_teleporter_gui", () -> IMenuTypeExtension.create(CustomTeleporterGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BlockTeleporterGuiMenu>> BLOCK_TELEPORTER_GUI = REGISTRY.register("block_teleporter_gui", () -> IMenuTypeExtension.create(BlockTeleporterGuiMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof CreateteleportersModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}