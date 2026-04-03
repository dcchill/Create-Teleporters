package net.createteleporters.util;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;

public class PocketDimensionTracker extends SavedData {
	public static final String DATA_NAME = "createteleporters_pocketdim_tracker";
	public ListTag boundPositions = new ListTag();

	public static PocketDimensionTracker load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		PocketDimensionTracker data = new PocketDimensionTracker();
		data.read(tag, lookupProvider);
		return data;
	}

	public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
		if (nbt.contains("boundPositions")) {
			boundPositions = nbt.getList("boundPositions", Tag.TAG_STRING);
		}
	}

	@Override
	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
		if (!boundPositions.isEmpty()) {
			nbt.put("boundPositions", boundPositions);
		}
		return nbt;
	}

	public boolean isPositionBound(long x, long y, long z) {
		String posKey = x + "," + y + "," + z;
		for (int i = 0; i < boundPositions.size(); i++) {
			if (boundPositions.getString(i).equals(posKey)) {
				return true;
			}
		}
		return false;
	}

	public void bindPosition(long x, long y, long z) {
		String posKey = x + "," + y + "," + z;
		// Remove existing binding if present (allows rebinding)
		boundPositions.removeIf(tag -> tag.getAsString().equals(posKey));
		boundPositions.add(StringTag.valueOf(posKey));
	}

	public static PocketDimensionTracker get(LevelAccessor world) {
		if (world instanceof ServerLevelAccessor serverLevelAcc) {
			return serverLevelAcc.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage()
					.computeIfAbsent(new SavedData.Factory<>(PocketDimensionTracker::new, PocketDimensionTracker::load), DATA_NAME);
		}
		return new PocketDimensionTracker();
	}
}
