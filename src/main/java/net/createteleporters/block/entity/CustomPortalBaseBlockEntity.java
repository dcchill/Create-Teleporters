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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.createteleporters.world.inventory.CustomTeleporterGuiMenu;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModBlockEntities;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.createteleporters.client.CustomPortalSurfaceClient;
import net.createteleporters.integration.SableAeronauticsIntegration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.Unpooled;

public class CustomPortalBaseBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
	private Set<UUID> portalOccupants = new HashSet<>();

	public void triggerTrainRipple(double horizontal, double y) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		CompoundTag data = getPersistentData();
		if (!data.getBoolean("portalVisualActive") || data.getBoolean("immersivePortalCreated")) return;
		int min = data.getInt("portalMinExtent") + 1, max = data.getInt("portalMaxExtent"), top = data.getInt("portalHeight");
		if (max <= min || top <= 1) return;
		float h = (float) Math.max(min, Math.min(max, horizontal));
		float height = (float) Math.max(1, Math.min(top, y));
		long now = serverLevel.getGameTime();
		ListTag ripples = data.getList("portalTrainRipples", Tag.TAG_COMPOUND);
		ripples.removeIf(tag -> now - ((CompoundTag) tag).getLong("Time") >= 48);
		if (!ripples.isEmpty()) {
			CompoundTag last = ripples.getCompound(ripples.size() - 1);
			if (last.getLong("Time") == now && last.getFloat("Horizontal") == h && last.getFloat("Y") == height) return;
		}
		while (ripples.size() >= 8) ripples.remove(0);
		CompoundTag ripple = new CompoundTag();
		ripple.putLong("Time", now);
		ripple.putFloat("Horizontal", h);
		ripple.putFloat("Y", height);
		ripples.add(ripple);
		data.put("portalTrainRipples", ripples);
		setChanged();
		serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}

	public void updateEntryRipple(List<Entity> entities, AABB opening, boolean northSouth) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		Set<UUID> occupants = new HashSet<>();
		boolean changed = false;
		for (Entity entity : entities) {
			AABB bounds = SableAeronauticsIntegration.getEntityBounds(level, opening, entity);
			if (!bounds.intersects(opening)) continue;
			occupants.add(entity.getUUID());
			if (portalOccupants.contains(entity.getUUID())) continue;
			var center = bounds.getCenter();
			CompoundTag data = getPersistentData();
			data.putLong("portalRippleTime", serverLevel.getGameTime());
			data.putFloat("portalRippleHorizontal", (float) (northSouth ? center.x - worldPosition.getX() : center.z - worldPosition.getZ()));
			data.putFloat("portalRippleY", (float) (center.y - worldPosition.getY()));
			changed = true;
		}
		portalOccupants = occupants;
		if (changed) {
			setChanged();
			serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	}

	public CustomPortalBaseBlockEntity(BlockPos position, BlockState state) {
		super(CreateteleportersModBlockEntities.CUSTOM_PORTAL_BASE.get(), position, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && level.isClientSide()) CustomPortalSurfaceClient.track(this);
	}

	@Override
	public void setRemoved() {
		if (level != null && level.isClientSide()) CustomPortalSurfaceClient.remove(this);
		super.setRemoved();
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		if (compound.get("fluidTank") instanceof CompoundTag compoundTag)
			fluidTank.readFromNBT(lookupProvider, compoundTag);
		CustomPortalTeleportMode.getOrMigrate(this);
		if (level != null && level.isClientSide()) CustomPortalSurfaceClient.track(this);
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
