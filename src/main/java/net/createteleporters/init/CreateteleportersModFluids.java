/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import net.createteleporters.fluid.QuantumFluidFluid;
import net.createteleporters.CreateteleportersMod;

public class CreateteleportersModFluids {
	public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(BuiltInRegistries.FLUID, CreateteleportersMod.MODID);
	public static final DeferredHolder<Fluid, FlowingFluid> QUANTUM_FLUID = REGISTRY.register("quantum_fluid", () -> new QuantumFluidFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_QUANTUM_FLUID = REGISTRY.register("flowing_quantum_fluid", () -> new QuantumFluidFluid.Flowing());

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class FluidsClientSideHandler {
		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			ItemBlockRenderTypes.setRenderLayer(QUANTUM_FLUID.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_QUANTUM_FLUID.get(), RenderType.translucent());
		}
	}
}