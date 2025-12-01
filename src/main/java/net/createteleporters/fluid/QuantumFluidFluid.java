package net.createteleporters.fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.LiquidBlock;

import net.createteleporters.init.CreateteleportersModItems;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModFluidTypes;
import net.createteleporters.init.CreateteleportersModBlocks;

public abstract class QuantumFluidFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> CreateteleportersModFluidTypes.QUANTUM_FLUID_TYPE.get(), () -> CreateteleportersModFluids.QUANTUM_FLUID.get(),
			() -> CreateteleportersModFluids.FLOWING_QUANTUM_FLUID.get()).explosionResistance(100f).tickRate(4).bucket(() -> CreateteleportersModItems.QUANTUM_FLUID_BUCKET.get())
			.block(() -> (LiquidBlock) CreateteleportersModBlocks.QUANTUM_FLUID.get());

	private QuantumFluidFluid() {
		super(PROPERTIES);
	}

	public static class Source extends QuantumFluidFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends QuantumFluidFluid {
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		public boolean isSource(FluidState state) {
			return false;
		}
	}
}