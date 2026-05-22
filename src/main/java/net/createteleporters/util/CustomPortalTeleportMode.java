package net.createteleporters.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.createteleporters.configuration.CTPConfigConfiguration;

public final class CustomPortalTeleportMode {
	public static final String TAG = "portalTeleportMode";
	public static final String COORDINATE = "coordinate";
	public static final String PORTAL_TO_PORTAL = "portal_to_portal";

	private CustomPortalTeleportMode() {
	}

	public static String getOrMigrate(LevelAccessor world, BlockPos pos) {
		return getOrMigrate(world.getBlockEntity(pos));
	}

	public static String getOrMigrate(BlockEntity blockEntity) {
		if (blockEntity == null) {
			return defaultModeFromLegacyConfig();
		}

		CompoundTag nbt = blockEntity.getPersistentData();
		String mode = nbt.getString(TAG);
		if (isValid(mode)) {
			return mode;
		}

		mode = defaultModeFromLegacyConfig();
		nbt.putString(TAG, mode);
		blockEntity.setChanged();
		if (blockEntity.getLevel() instanceof Level level) {
			level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
		}
		return mode;
	}

	public static boolean isCoordinateMode(LevelAccessor world, BlockPos pos) {
		return COORDINATE.equals(getOrMigrate(world, pos));
	}

	public static String toggle(LevelAccessor world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			return defaultModeFromLegacyConfig();
		}

		String current = getOrMigrate(blockEntity);
		String next = COORDINATE.equals(current) ? PORTAL_TO_PORTAL : COORDINATE;
		blockEntity.getPersistentData().putString(TAG, next);
		blockEntity.setChanged();
		if (world instanceof Level level) {
			level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
		}
		return next;
	}

	public static Component displayName(String mode) {
		return Component.translatable(COORDINATE.equals(mode)
			? "gui.createteleporters.custom_teleporter_gui.mode_coordinates"
			: "gui.createteleporters.custom_teleporter_gui.mode_portal_to_portal");
	}

	public static String defaultModeFromLegacyConfig() {
		if (CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.get()) {
			return PORTAL_TO_PORTAL;
		}
		return CTPConfigConfiguration.LEGACY_CUSTOM_PORTAL_COORDINATE_BINDING_DEFAULT ? COORDINATE : PORTAL_TO_PORTAL;
	}

	private static boolean isValid(String mode) {
		return COORDINATE.equals(mode) || PORTAL_TO_PORTAL.equals(mode);
	}
}
