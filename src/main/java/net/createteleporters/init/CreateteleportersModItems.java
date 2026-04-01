/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.createteleporters.item.TpLinkItem;
import net.createteleporters.item.RedstonePearlItem;
import net.createteleporters.item.QuantumMechanismItem;
import net.createteleporters.item.QuantumFluidItem;
import net.createteleporters.item.PocketDimensionRemoteItem;
import net.createteleporters.item.IncompleteQMechanismItem;
import net.createteleporters.item.IncompleteAdvancedPartItem;
import net.createteleporters.item.AdvancedPartItem;
import net.createteleporters.item.ADVTplinkItem;
import net.createteleporters.CreateteleportersMod;

public class CreateteleportersModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(CreateteleportersMod.MODID);
	public static final DeferredItem<Item> REDSTONE_PEARL = REGISTRY.register("redstone_pearl", RedstonePearlItem::new);
	public static final DeferredItem<Item> TP_LINK = REGISTRY.register("tp_link", TpLinkItem::new);
	public static final DeferredItem<Item> TELEPORTER = block(CreateteleportersModBlocks.TELEPORTER);
	public static final DeferredItem<Item> QUANTUM_FLUID_BUCKET = REGISTRY.register("quantum_fluid_bucket", QuantumFluidItem::new);
	public static final DeferredItem<Item> INCOMPLETE_Q_MECHANISM = REGISTRY.register("incomplete_q_mechanism", IncompleteQMechanismItem::new);
	public static final DeferredItem<Item> QUANTUM_MECHANISM = REGISTRY.register("quantum_mechanism", QuantumMechanismItem::new);
	public static final DeferredItem<Item> ADVANCED_PART = REGISTRY.register("advanced_part", AdvancedPartItem::new);
	public static final DeferredItem<Item> INCOMPLETE_ADVANCED_PART = REGISTRY.register("incomplete_advanced_part", IncompleteAdvancedPartItem::new);
	public static final DeferredItem<Item> ITEM_TP = block(CreateteleportersModBlocks.ITEM_TP);
	public static final DeferredItem<Item> QUANTUM_CASING = block(CreateteleportersModBlocks.QUANTUM_CASING);
	public static final DeferredItem<Item> ADV_TPLINK = REGISTRY.register("adv_tplink", ADVTplinkItem::new);
	public static final DeferredItem<Item> CUSTOM_PORTAL_BASE = block(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE);
	public static final DeferredItem<Item> QUANTUM_PORTAL_BLOCK = block(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK);
	public static final DeferredItem<Item> POCKET_DIMENSION_REMOTE = REGISTRY.register("pocket_dimension_remote", PocketDimensionRemoteItem::new);
	public static final DeferredItem<Item> POCKET_D_BLOCK = block(CreateteleportersModBlocks.POCKET_D_BLOCK);
	public static final DeferredItem<Item> CUSTOM_PORTAL_BASE_DUMMY_BLOCK = block(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK);
	public static final DeferredItem<Item> ENTITY_TELEPORTER_SLAB = block(CreateteleportersModBlocks.ENTITY_TELEPORTER_SLAB);
	public static final DeferredItem<Item> ENTITY_TELEPORTER_QUARTER = block(CreateteleportersModBlocks.ENTITY_TELEPORTER_QUARTER);
	public static final DeferredItem<Item> CUSTOM_PORTAL = block(CreateteleportersModBlocks.CUSTOM_PORTAL);
	public static final DeferredItem<Item> CUSTOM_PORTAL_ON = block(CreateteleportersModBlocks.CUSTOM_PORTAL_ON);
	public static final DeferredItem<Item> BLOCK_TELEPORTER = block(CreateteleportersModBlocks.BLOCK_TELEPORTER);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}