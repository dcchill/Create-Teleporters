package net.createteleporters.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.nbt.CompoundTag;

public class CustomPortalBaseOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double maxDist = 0;
		double dist = 0;
		boolean facingX = false;
		PortalBlockCheckerProcedure.execute(world, x, y, z);
		if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "portalActive")) {
			if (4 <= getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {
				if ((getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("east") || (getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("west")) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"fill ~ ~1 ~-1 ~ ~3 ~1 createteleporters:quantum_portal_block");
				} else if ((getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("north") || (getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("south")) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"fill ~-1 ~1 ~ ~1 ~3 ~ createteleporters:quantum_portal_block");
				}
				if (world instanceof ILevelExtension _ext) {
					IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
					if (_fluidHandler != null)
						_fluidHandler.drain(4, IFluidHandler.FluidAction.EXECUTE);
				}

				// ---------- unified, corrected AABB + feet check (keeps your NBT handling) ----------
				String rotation = getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation");

				// Build an AABB that matches the blocks placed by your fill commands:
				// east/west: portal column at x..x+1, spans z-1..z+1
				// north/south: portal column at z..z+1, spans x-1..x+1
				AABB portalArea;
				if ("east".equals(rotation) || "west".equals(rotation)) {
					portalArea = new AABB(x, y + 1, z - 1, x + 1, y + 3, z + 1);
				} else {
					portalArea = new AABB(x - 1, y + 1, z, x + 1, y + 3, z + 1);
				}

				// tiny epsilon to avoid strict boundary misses
				final double EPS = 1e-6;
				portalArea = new AABB(
					Math.min(portalArea.minX, portalArea.maxX) - EPS,
					Math.min(portalArea.minY, portalArea.maxY) - EPS,
					Math.min(portalArea.minZ, portalArea.maxZ) - EPS,
					Math.max(portalArea.minX, portalArea.maxX) + EPS,
					Math.max(portalArea.minY, portalArea.maxY) + EPS,
					Math.max(portalArea.minZ, portalArea.maxZ) + EPS
				);

				// Cache your inventory item's custom data tag (keeps your existing tag usage)
				ItemStack invStack = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy());
				CompoundTag cd = invStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
				String targetDim = cd.getString("dimension");
				double tx = cd.getDouble("xpo");
				double ty = cd.getDouble("ypo");
				double tz = cd.getDouble("zpo");
				double yaw = cd.getDouble("yawpo");

				for (Entity entityiterator : world.getEntities(null, portalArea)) {
					// use the entity's feet (minY of bounding box) to prevent premature triggers
					double feetY = entityiterator.getBoundingBox().minY;
					if (feetY + 1e-6 >= (y + 1) && feetY <= (y + 3) + 1e-6) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									("execute in " + targetDim
											+ ("run tp " + entityiterator.getStringUUID() + " "
													+ tx + " "
													+ (ty + 1) + " "
													+ tz)));
						{
							Entity _ent = entityiterator;
							_ent.setYRot((float) yaw);
							_ent.setXRot(0);
							_ent.setYBodyRot(_ent.getYRot());
							_ent.setYHeadRot(_ent.getYRot());
							_ent.yRotO = _ent.getYRot();
							_ent.xRotO = _ent.getXRot();
							if (_ent instanceof LivingEntity _entity) {
								_entity.yBodyRotO = _entity.getYRot();
								_entity.yHeadRotO = _entity.getYRot();
							}
						}
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.END_ROD, x, (y + 1), z, 20, 1.5, 1.5, 1.5, 0.1);
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 20, 1.5, 1.5, 1.5, 0.1);
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y + 1, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
							} else {
								_level.playLocalSound(x, (y + 1), z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
							}
						}
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(tx, ty, tz), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
							} else {
								_level.playLocalSound(tx, ty, tz, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.enderman.teleport")), SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
							}
						}
					}
				}
				// ------------------------------------------------------------------------------------

				return "Portal Ready";
			} else {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("portalActive", false);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
				return "No Telefluid";
			}
		} else {
			if ((getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("east") || (getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("west")) {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~ ~1 ~-1 ~ ~3 ~1 air replace createteleporters:quantum_portal_block");
			} else if ((getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("north") || (getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation")).equals("south")) {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~-1 ~1 ~ ~1 ~3 ~ air replace createteleporters:quantum_portal_block");
			}
		}
		return "Portal Frame Incorrect";
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getBoolean(tag);
		return false;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}

	private static String getBlockNBTString(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getString(tag);
		return "";
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
}
