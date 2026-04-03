package net.createteleporters.procedures;

import org.joml.Vector3f;

import org.checkerframework.checker.units.qual.g;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;

import net.createteleporters.init.CreateteleportersModItems;
import net.createteleporters.configuration.CTPConfigConfiguration;

import java.util.List;
import java.util.Comparator;

public class EntityTeleporterOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double dist = 0;
		double maxDist = 0;
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") == 0) {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}
		dist = Math.abs(x - (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo"))
				+ Math.abs(y - (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo"))
				+ Math.abs(z - (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo"));
		maxDist = (double) CTPConfigConfiguration.ENTITY_TP_RANGE.get();
		if (dist >= maxDist) {
			return "Out Of Range";
		}
		if (CreateteleportersModItems.TP_LINK.get() == (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem()) {
			{
				BlockPos pos = BlockPos.containing(x, y, z);
				boolean isCharging = false;
				// Check for any entity above the block (slightly expanded hitbox)
				AABB detectionBox = new AABB(x, y - 0.5, z, x + 1, y + 1.5, z + 1);
				List<Entity> entities = world.getEntitiesOfClass(Entity.class, detectionBox, e -> !e.getType().toString().equals("minecraft:item") && e.isAlive());
				if (!entities.isEmpty()) {
					isCharging = true;
				}
				BlockEntity blockEntity = world.getBlockEntity(pos);
				BlockState state = world.getBlockState(pos);
				if (blockEntity != null) {
					blockEntity.getPersistentData().putBoolean("charging", isCharging);
				}
				if (world instanceof Level level) {
					level.sendBlockUpdated(pos, state, state, 3);
				}
			}
			if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "charging") && getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) >= 250) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				} // === CONFIG ===
				double height = 2.0;
				double radius = 0.75;
				int turns = 4;
				double speed = 0.015;
				// Redstone particle needs a color (R, G, B) and scale
				float r = 0.9f;
				float g = 0.1f;
				float b = 0.1f;
				float scale = 1.0f;
				// Construct the dust particle options
				ParticleOptions particle = new DustParticleOptions(new Vector3f(r, g, b), scale);
				// === ONLY RUN ON SERVER ===
				if (world instanceof ServerLevel level) {
					long gameTime = level.getGameTime();
					double time = (gameTime % (int) (height / speed)) * speed;
					double centerX = x + 0.5;
					double centerY = y + 1;
					double centerZ = z + 0.5;
					// base angle for the first helix
					double angle1 = (time / height) * turns * 2 * Math.PI;
					double px1 = centerX + Math.cos(angle1) * radius;
					double py1 = centerY + time;
					double pz1 = centerZ + Math.sin(angle1) * radius;
					// second helix is 180° offset (Math.PI)
					double angle2 = angle1 + Math.PI;
					double px2 = centerX + Math.cos(angle2) * radius;
					double py2 = centerY + time;
					double pz2 = centerZ + Math.sin(angle2) * radius;
					// spawn both helices
					level.sendParticles(particle, px1, py1, pz1, 1, 0, 0, 0, 0);
					level.sendParticles(particle, px2, py2, pz2, 1, 0, 0, 0, 0);
				}
				if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= 50) {
					if (dist <= maxDist) {
						if (world instanceof ILevelExtension _ext) {
							IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
							if (_fluidHandler != null)
								_fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE);
						}
						{
							final Vec3 _center = new Vec3(x, (y + 1), z);
							for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
								// Save player's team before teleportation (Bug fix: preserve team assignment)
								String playerTeamName = null;
								if (entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									net.minecraft.world.scores.Scoreboard scoreboard = serverPlayer.getScoreboard();
									net.minecraft.world.scores.PlayerTeam playerTeam = scoreboard.getPlayerTeam(serverPlayer.getScoreboardName());
									if (playerTeam != null) {
										playerTeamName = playerTeam.getName();
									}
								}

								// Check if target dimension is specified and different from current
								String targetDimName = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
										.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("dimension");
								double targetX = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
										.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo");
								double targetY = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
										.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo") + 1;
								double targetZ = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
										.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo");
								float targetYaw = (float) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
										.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("yawpo");

								if (entityiterator instanceof ServerPlayer serverPlayer && !targetDimName.isEmpty()) {
									// Cross-dimension teleportation for players
									try {
										ResourceKey<Level> destinationKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(targetDimName));
										ServerLevel destinationLevel = serverPlayer.server.getLevel(destinationKey);
										if (destinationLevel != null) {
											serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
											serverPlayer.teleportTo(destinationLevel, targetX, targetY, targetZ, targetYaw, serverPlayer.getXRot());
											serverPlayer.connection.send(new ClientboundPlayerAbilitiesPacket(serverPlayer.getAbilities()));
											for (MobEffectInstance effect : serverPlayer.getActiveEffects())
												serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(serverPlayer.getId(), effect, false));
											serverPlayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
										} else {
											serverPlayer.teleportTo(targetX, targetY, targetZ);
										}
									} catch (Exception e) {
										serverPlayer.teleportTo(targetX, targetY, targetZ);
									}
								} else {
									// Same-dimension teleportation for non-players or when no dimension specified
									Entity _ent = entityiterator;
									_ent.teleportTo(targetX, targetY, targetZ);
									if (_ent instanceof ServerPlayer _serverPlayer)
										_serverPlayer.connection.teleport(targetX, targetY, targetZ, _ent.getYRot(), _ent.getXRot());
								}

								// Restore player's team after teleportation (Bug fix: preserve team assignment)
								if (playerTeamName != null && entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									net.minecraft.world.scores.Scoreboard scoreboard = serverPlayer.getScoreboard();
									net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(playerTeamName);
									if (team != null) {
										scoreboard.addPlayerToTeam(serverPlayer.getScoreboardName(), team);
									}
								}
								{
									Entity _ent = entityiterator;
									_ent.setYRot(targetYaw);
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
							}
						}
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20, 1.5, 1.5, 1.5, 0.1);
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.END_ROD, ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")),
									((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo")),
									((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")), 20, 1.5, 1.5, 1.5, 0.1);
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
							} else {
								_level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
							}
						}
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null,
										BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo"),
												(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo"),
												(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")),
										net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
							} else {
								_level.playLocalSound(((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")),
										((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo")),
										((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")),
										net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
							}
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					} else {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			} else {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", 0);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
		}
		return "Distance OK!";
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
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
}