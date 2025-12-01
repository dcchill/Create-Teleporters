package net.createteleporters.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;

import net.createteleporters.init.CreateteleportersModFluids;

public class QuantumFluidItem extends BucketItem {
	public QuantumFluidItem() {
		super(CreateteleportersModFluids.QUANTUM_FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)

		);
	}
}