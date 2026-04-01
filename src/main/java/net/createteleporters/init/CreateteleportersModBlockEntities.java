/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.registries.BuiltInRegistries;

import net.createteleporters.block.entity.ItemTPBlockEntity;
import net.createteleporters.block.entity.EntityTeleporterSlabBlockEntity;
import net.createteleporters.block.entity.EntityTeleporterQuarterBlockEntity;
import net.createteleporters.block.entity.EntityTeleporterBlockEntity;
import net.createteleporters.block.entity.CustomPortalOnBlockEntity;
import net.createteleporters.block.entity.CustomPortalBlockEntity;
import net.createteleporters.block.entity.CustomPortalBaseDummyBlockBlockEntity;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.block.entity.BlockTeleporterBlockEntity;
import net.createteleporters.CreateteleportersMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CreateteleportersModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateteleportersMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> TELEPORTER = register("teleporter", CreateteleportersModBlocks.TELEPORTER, EntityTeleporterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ITEM_TP = register("item_tp", CreateteleportersModBlocks.ITEM_TP, ItemTPBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CUSTOM_PORTAL_BASE = register("custom_portal_base", CreateteleportersModBlocks.CUSTOM_PORTAL_BASE, CustomPortalBaseBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CUSTOM_PORTAL_BASE_DUMMY_BLOCK = register("custom_portal_base_dummy_block", CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK,
			CustomPortalBaseDummyBlockBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ENTITY_TELEPORTER_SLAB = register("entity_teleporter_slab", CreateteleportersModBlocks.ENTITY_TELEPORTER_SLAB, EntityTeleporterSlabBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> ENTITY_TELEPORTER_QUARTER = register("entity_teleporter_quarter", CreateteleportersModBlocks.ENTITY_TELEPORTER_QUARTER, EntityTeleporterQuarterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CUSTOM_PORTAL = register("custom_portal", CreateteleportersModBlocks.CUSTOM_PORTAL, CustomPortalBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CUSTOM_PORTAL_ON = register("custom_portal_on", CreateteleportersModBlocks.CUSTOM_PORTAL_ON, CustomPortalOnBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BLOCK_TELEPORTER = register("block_teleporter", CreateteleportersModBlocks.BLOCK_TELEPORTER, BlockTeleporterBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TELEPORTER.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TELEPORTER.get(), (blockEntity, side) -> ((EntityTeleporterBlockEntity) blockEntity).getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_TP.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ITEM_TP.get(), (blockEntity, side) -> ((ItemTPBlockEntity) blockEntity).getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CUSTOM_PORTAL_BASE.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CUSTOM_PORTAL_BASE.get(), (blockEntity, side) -> ((CustomPortalBaseBlockEntity) blockEntity).getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_TELEPORTER_SLAB.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY_TELEPORTER_SLAB.get(), (blockEntity, side) -> ((EntityTeleporterSlabBlockEntity) blockEntity).getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY_TELEPORTER_QUARTER.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY_TELEPORTER_QUARTER.get(), (blockEntity, side) -> ((EntityTeleporterQuarterBlockEntity) blockEntity).getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CUSTOM_PORTAL.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CUSTOM_PORTAL_ON.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLOCK_TELEPORTER.get(), (blockEntity, side) -> new SidedInvWrapper((WorldlyContainer) blockEntity, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BLOCK_TELEPORTER.get(), (blockEntity, side) -> ((BlockTeleporterBlockEntity) blockEntity).getFluidTank());
	}
}