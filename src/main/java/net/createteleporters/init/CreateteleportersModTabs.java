/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.createteleporters.CreateteleportersMod;

public class CreateteleportersModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateteleportersMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_TELEPORTERS = REGISTRY.register("create_teleporters",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.createteleporters.create_teleporters")).icon(() -> new ItemStack(CreateteleportersModItems.REDSTONE_PEARL.get())).displayItems((parameters, tabData) -> {
				tabData.accept(CreateteleportersModItems.REDSTONE_PEARL.get());
				tabData.accept(CreateteleportersModItems.QUANTUM_FLUID_BUCKET.get());
				tabData.accept(CreateteleportersModItems.ADVANCED_PART.get());
				tabData.accept(CreateteleportersModItems.QUANTUM_MECHANISM.get());
				tabData.accept(CreateteleportersModBlocks.TELEPORTER.get().asItem());
				tabData.accept(CreateteleportersModBlocks.ENTITY_TELEPORTER_SLAB.get().asItem());
				tabData.accept(CreateteleportersModBlocks.ENTITY_TELEPORTER_QUARTER.get().asItem());
				tabData.accept(CreateteleportersModBlocks.ITEM_TP.get().asItem());
				tabData.accept(CreateteleportersModItems.TP_LINK.get());
				tabData.accept(CreateteleportersModBlocks.QUANTUM_CASING.get().asItem());
				tabData.accept(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get().asItem());
				tabData.accept(CreateteleportersModBlocks.BLOCK_TELEPORTER.get().asItem());
				tabData.accept(CreateteleportersModItems.ADV_TPLINK.get());
				tabData.accept(CreateteleportersModItems.POCKET_DIMENSION_REMOTE.get());
			}).build());
}