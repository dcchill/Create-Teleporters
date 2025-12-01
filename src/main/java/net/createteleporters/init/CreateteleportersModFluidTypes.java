/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.createteleporters.init;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.FluidType;

import net.createteleporters.fluid.types.QuantumFluidFluidType;
import net.createteleporters.CreateteleportersMod;

public class CreateteleportersModFluidTypes {
	public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CreateteleportersMod.MODID);
	public static final DeferredHolder<FluidType, FluidType> QUANTUM_FLUID_TYPE = REGISTRY.register("quantum_fluid", () -> new QuantumFluidFluidType());
}