package net.createteleporters.block.entity;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.createteleporters.world.inventory.CustomTeleporterGuiMenu;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModBlockEntities;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

public class CustomPortalBaseBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);

	public CustomPortalBaseBlockEntity(BlockPos position, BlockState state) {
		super(CreateteleportersModBlockEntities.CUSTOM_PORTAL_BASE.get(), position, state);
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		if (compound.get("fluidTank") instanceof CompoundTag compoundTag)
			fluidTank.readFromNBT(lookupProvider, compoundTag);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		compound.put("fluidTank", fluidTank.writeToNBT(lookupProvider, new CompoundTag()));
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("custom_portal_base");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new CustomTeleporterGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Custom Portal Base");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		if (index == 0)
			return false;
		return true;
	}

	private final FluidTank fluidTank = new FluidTank(32000, fs -> {
		if (fs.getFluid() == CreateteleportersModFluids.QUANTUM_FLUID.get())
			return true;
		if (fs.getFluid() == CreateteleportersModFluids.FLOWING_QUANTUM_FLUID.get())
			return true;
		return false;
	}) {
		@Override
		protected void onContentsChanged() {
			super.onContentsChanged();
			setChanged();
			if (level != null) {
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
		}
	};

	public FluidTank getFluidTank() {
		return fluidTank;
	}
}