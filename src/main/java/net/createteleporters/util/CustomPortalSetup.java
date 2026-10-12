package net.createteleporters.util;

import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Setup checks that never load destination chunks. */
public final class CustomPortalSetup {
	private CustomPortalSetup() {}

	public enum Status {
		READY, LOCAL_FRAME, LOCAL_FUEL, UNLINKED, DESTINATION_UNAVAILABLE, DESTINATION_MISSING,
		MODE_MISMATCH, REMOTE_FRAME, REMOTE_FUEL, MISSING_LINK;

		public Component message() {
			return Component.translatable("gui.createteleporters.custom_teleporter_gui.status_" + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	public static boolean hasLink(CustomPortalBaseBlockEntity base) {
		return base.getItem(0).is(CreateteleportersModItems.ADV_TPLINK.get());
	}

	private static boolean portalMode(BlockEntity base) {
		return CustomPortalTeleportMode.PORTAL_TO_PORTAL.equals(CustomPortalTeleportMode.getOrMigrate(base));
	}

	public static CustomPortalBaseBlockEntity remote(CustomPortalBaseBlockEntity base) {
		if (!(base.getLevel() instanceof ServerLevel level) || !base.getPersistentData().getBoolean("isLinked")) return null;
		var data = base.getPersistentData();
		var dim = ResourceLocation.tryParse(data.getString("linkedDim"));
		var target = dim == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
		BlockPos pos = BlockPos.containing(data.getDouble("linkedX"), data.getDouble("linkedY"), data.getDouble("linkedZ"));
		var chunk = target == null ? null : target.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
		return chunk != null && chunk.getBlockEntity(pos) instanceof CustomPortalBaseBlockEntity other ? other : null;
	}

	public static boolean reciprocal(CustomPortalBaseBlockEntity base, CustomPortalBaseBlockEntity other) {
		return other != null && other != base && remote(other) == base;
	}

	public static boolean canInsertLink(CustomPortalBaseBlockEntity base) {
		var other = remote(base);
		return !portalMode(base) || !reciprocal(base, other) || !portalMode(other) || !hasLink(other);
	}

	/** 0 = missing, 1 = here, 2 = other end, 3 = destination unknown. */
	public static int linkLocation(CustomPortalBaseBlockEntity base) {
		if (hasLink(base)) return 1;
		var other = remote(base);
		if (reciprocal(base, other) && portalMode(other)) return hasLink(other) ? 2 : 0;
		return base.getPersistentData().getBoolean("isLinked") ? 3 : 0;
	}

	public static Status status(CustomPortalBaseBlockEntity base) {
		if (base.getFluidTank().getFluidAmount() < 256) return Status.LOCAL_FUEL;
		if (!base.getPersistentData().getBoolean("portalActive")) return Status.LOCAL_FRAME;
		if (!portalMode(base)) return hasLink(base) ? Status.READY : Status.MISSING_LINK;
		if (!base.getPersistentData().getBoolean("isLinked")) return Status.UNLINKED;
		var other = remote(base);
		if (other == null) {
			var dim = ResourceLocation.tryParse(base.getPersistentData().getString("linkedDim"));
			var level = (ServerLevel) base.getLevel();
			var target = dim == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
			var data = base.getPersistentData();
			var pos = BlockPos.containing(data.getDouble("linkedX"), data.getDouble("linkedY"), data.getDouble("linkedZ"));
			return target != null && target.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null ? Status.DESTINATION_UNAVAILABLE : Status.DESTINATION_MISSING;
		}
		if (!reciprocal(base, other)) return Status.DESTINATION_MISSING;
		if (!portalMode(other)) return Status.MODE_MISMATCH;
		if (other.getFluidTank().getFluidAmount() < 256) return Status.REMOTE_FUEL;
		if (!other.getPersistentData().getBoolean("portalActive")) return Status.REMOTE_FRAME;
		return hasLink(base) || hasLink(other) ? Status.READY : Status.MISSING_LINK;
	}
}
