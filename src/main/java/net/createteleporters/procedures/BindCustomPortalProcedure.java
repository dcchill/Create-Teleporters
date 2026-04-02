package net.createteleporters.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.init.CreateteleportersModItems;
import net.createteleporters.block.CustomPortalBaseBlock;

/**
 * Procedure for binding custom portals together.
 * Players use an Advanced TP Link to link two custom portal bases.
 * Right-click first portal with ADVTplink to save coordinates.
 * Right-click second portal with linked ADVTplink to complete binding.
 * Shift + Right-click to clear the link data.
 */
public class BindCustomPortalProcedure {
	public static InteractionResult execute(LevelAccessor world, double x, double y, double z, Player entity, InteractionHand hand) {
		if (!(entity instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		ItemStack heldItem = entity.getItemInHand(hand);
		if (!(heldItem.getItem() instanceof net.createteleporters.item.ADVTplinkItem)) {
			return InteractionResult.PASS;
		}

		BlockPos pos = BlockPos.containing(x, y, z);
		if (!(world.getBlockState(pos).getBlock() instanceof CustomPortalBaseBlock)) {
			return InteractionResult.PASS;
		}

		// Check if player is sneaking - clear link data
		if (entity.isShiftKeyDown()) {
			CompoundTag linkData = heldItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			boolean hadData = linkData.contains("savedX") || linkData.contains("linkedX");
			
			linkData.remove("savedX");
			linkData.remove("savedY");
			linkData.remove("savedZ");
			linkData.remove("savedDim");
			linkData.remove("linkedX");
			linkData.remove("linkedY");
			linkData.remove("linkedZ");
			linkData.remove("linkedDim");
			linkData.remove("linkedYaw");
			heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(linkData));
			
			if (hadData) {
				serverPlayer.sendSystemMessage(Component.literal("Advanced TP Link data cleared!"));
			} else {
				serverPlayer.sendSystemMessage(Component.literal("Advanced TP Link has no data to clear."));
			}
			return InteractionResult.SUCCESS;
		}

		// Get the ADVTplink's custom data
		CompoundTag linkData = heldItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		
		// Check if the link already has saved coordinates
		if (linkData.contains("savedX") && linkData.contains("savedY") && linkData.contains("savedZ") && linkData.contains("savedDim")) {
			// Link has coordinates - try to bind to this portal
			double savedX = linkData.getDouble("savedX");
			double savedY = linkData.getDouble("savedY");
			double savedZ = linkData.getDouble("savedZ");
			String savedDim = linkData.getString("savedDim");
			
			BlockPos savedPos = BlockPos.containing(savedX, savedY, savedZ);
			
			// Verify the saved location is still a valid custom portal base
			if (!(world.getBlockState(savedPos).getBlock() instanceof CustomPortalBaseBlock)) {
				serverPlayer.sendSystemMessage(Component.literal("The linked portal is no longer valid!"));
				// Clear the invalid link data
				linkData.remove("savedX");
				linkData.remove("savedY");
				linkData.remove("savedZ");
				linkData.remove("savedDim");
				heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(linkData));
				return InteractionResult.SUCCESS;
			}
			
			// Bind this portal to the saved portal
			BlockEntity thisBE = world.getBlockEntity(pos);
			BlockEntity savedBE = world.getBlockEntity(savedPos);
			
			if (thisBE != null && savedBE != null) {
				// Get current dimension
				String currentDim = ((Level) world).dimension().location().toString();
				
				// Get rotation from both portals for proper yaw calculation
				String thisRotation = thisBE.getPersistentData().getString("rotation");
				String savedRotation = savedBE.getPersistentData().getString("rotation");
				
				// Calculate yaw for teleportation direction
				double thisYaw = getYawFromRotation(thisRotation);
				double savedYaw = getYawFromRotation(savedRotation);
				
				// Store binding data on both portals
				thisBE.getPersistentData().putDouble("linkedX", savedX);
				thisBE.getPersistentData().putDouble("linkedY", savedY);
				thisBE.getPersistentData().putDouble("linkedZ", savedZ);
				thisBE.getPersistentData().putString("linkedDim", savedDim);
				thisBE.getPersistentData().putDouble("linkedYaw", savedYaw);
				thisBE.getPersistentData().putBoolean("isLinked", true);
				
				savedBE.getPersistentData().putDouble("linkedX", x);
				savedBE.getPersistentData().putDouble("linkedY", y);
				savedBE.getPersistentData().putDouble("linkedZ", z);
				savedBE.getPersistentData().putString("linkedDim", currentDim);
				savedBE.getPersistentData().putDouble("linkedYaw", thisYaw);
				savedBE.getPersistentData().putBoolean("isLinked", true);
				
				serverPlayer.sendSystemMessage(Component.literal("Portals linked successfully!"));
				
				// Clear the link data from the item
				linkData.remove("savedX");
				linkData.remove("savedY");
				linkData.remove("savedZ");
				linkData.remove("savedDim");
				heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(linkData));
				
				// Send update to client
				if (world instanceof Level level) {
					level.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
					level.sendBlockUpdated(savedPos, world.getBlockState(savedPos), world.getBlockState(savedPos), 3);
				}
				
				return InteractionResult.SUCCESS;
			}
		} else {
			// Link doesn't have coordinates - save this portal's coordinates to the link
			String currentDim = ((Level) world).dimension().location().toString();
			
			linkData.putDouble("savedX", x);
			linkData.putDouble("savedY", y);
			linkData.putDouble("savedZ", z);
			linkData.putString("savedDim", currentDim);
			heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(linkData));
			
			serverPlayer.sendSystemMessage(Component.literal("Portal location saved! Right-click another portal to link them."));
			
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	/**
	 * Gets the linked portal coordinates for a portal.
	 * Returns null if the portal is not linked.
	 */
	public static LinkedPortalData getLinkedPortal(LevelAccessor world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be == null || !be.getPersistentData().getBoolean("isLinked")) {
			return null;
		}

		CompoundTag nbt = be.getPersistentData();
		return new LinkedPortalData(
			nbt.getDouble("linkedX"),
			nbt.getDouble("linkedY"),
			nbt.getDouble("linkedZ"),
			nbt.getString("linkedDim")
		);
	}

	/**
	 * Helper class to store linked portal data.
	 */
	public static class LinkedPortalData {
		public final double x;
		public final double y;
		public final double z;
		public final String dimension;

		public LinkedPortalData(double x, double y, double z, String dimension) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.dimension = dimension;
		}
	}
	
	/**
	 * Converts a rotation string to yaw value.
	 */
	private static double getYawFromRotation(String rotation) {
		return switch (rotation) {
			case "north" -> 180.0;
			case "south" -> 0.0;
			case "east" -> -90.0;
			case "west" -> 90.0;
			default -> 0.0;
		};
	}
}
