package net.createteleporters.block;

import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LiquidBlock;

import net.createteleporters.init.CreateteleportersModFluids;

public class QuantumFluidBlock extends LiquidBlock {
	public QuantumFluidBlock() {
		super(CreateteleportersModFluids.QUANTUM_FLUID.get(), BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(100f).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY).replaceable());
	}
}