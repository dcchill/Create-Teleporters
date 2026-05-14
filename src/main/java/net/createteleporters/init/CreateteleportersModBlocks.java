/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import net.createteleporters.block.QuantumPortalBlockBlock;
import net.createteleporters.block.QuantumFluidBlock;
import net.createteleporters.block.QuantumCasingBlock;
import net.createteleporters.block.PocketdblockBlock;
import net.createteleporters.block.ItemTPBlock;
import net.createteleporters.block.EntityTeleporterSlabBlock;
import net.createteleporters.block.EntityTeleporterQuarterBlock;
import net.createteleporters.block.EntityTeleporterBlock;
import net.createteleporters.block.CustomPortalOnBlock;
import net.createteleporters.block.CustomPortalBlock;
import net.createteleporters.block.CustomPortalBaseDummyBlockBlock;
import net.createteleporters.block.CustomPortalBaseBlock;
import net.createteleporters.block.BlockTeleporterBlock;
import net.createteleporters.CreateteleportersMod;

public class CreateteleportersModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(CreateteleportersMod.MODID);
	public static final DeferredBlock<Block> TELEPORTER = REGISTRY.register("teleporter", EntityTeleporterBlock::new);
	public static final DeferredBlock<Block> QUANTUM_FLUID = REGISTRY.register("quantum_fluid", QuantumFluidBlock::new);
	public static final DeferredBlock<Block> ITEM_TP = REGISTRY.register("item_tp", ItemTPBlock::new);
	public static final DeferredBlock<Block> QUANTUM_CASING = REGISTRY.register("quantum_casing", QuantumCasingBlock::new);
	public static final DeferredBlock<Block> CUSTOM_PORTAL_BASE = REGISTRY.register("custom_portal_base", CustomPortalBaseBlock::new);
	public static final DeferredBlock<Block> QUANTUM_PORTAL_BLOCK = REGISTRY.register("quantum_portal_block", QuantumPortalBlockBlock::new);
	public static final DeferredBlock<Block> POCKET_D_BLOCK = REGISTRY.register("pocket_d_block", PocketdblockBlock::new);
	public static final DeferredBlock<Block> CUSTOM_PORTAL_BASE_DUMMY_BLOCK = REGISTRY.register("custom_portal_base_dummy_block", CustomPortalBaseDummyBlockBlock::new);
	public static final DeferredBlock<Block> ENTITY_TELEPORTER_SLAB = REGISTRY.register("entity_teleporter_slab", EntityTeleporterSlabBlock::new);
	public static final DeferredBlock<Block> ENTITY_TELEPORTER_QUARTER = REGISTRY.register("entity_teleporter_quarter", EntityTeleporterQuarterBlock::new);
	public static final DeferredBlock<Block> CUSTOM_PORTAL = REGISTRY.register("custom_portal", CustomPortalBlock::new);
	public static final DeferredBlock<Block> CUSTOM_PORTAL_ON = REGISTRY.register("custom_portal_on", CustomPortalOnBlock::new);
	public static final DeferredBlock<Block> BLOCK_TELEPORTER = REGISTRY.register("block_teleporter", BlockTeleporterBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}