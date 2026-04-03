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
				} // Enhanced charging effect with color progression
				if (world instanceof ServerLevel level) {
					double progress = getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress");
					double progressPercent = progress / 50.0; // 0 to 1
					// Spawn increasing ring of particles that changes color
					int ringParticles = (int) (8 + progressPercent * 16); // 8 to 24 particles
					double ringRadius = 0.5 + progressPercent * 0.5; // Expanding radius
					double ringY = y + 1.2 + progressPercent * 0.8; // Rising ring (starts higher)

					// Color transitions: red -> yellow
					float cr, cg, cb;
					cr = 1.0f; // Red stays full
					cg = (float) progressPercent; // Green increases from 0 to 1
					cb = 0.1f; // Blue stays minimal

					ParticleOptions ringParticle = new DustParticleOptions(new Vector3f(cr, cg, cb), 1.5f);
					for (int i = 0; i < ringParticles; i++) {
						double angle = (i * 2 * Math.PI) / ringParticles;
						double px = x + 0.5 + Math.cos(angle) * ringRadius;
						double pz = z + 0.5 + Math.sin(angle) * ringRadius;
						level.sendParticles(ringParticle, px, ringY, pz, 1, 0, 0, 0, 0);
					}
					
					// Sparks above destination coordinates
					double destX = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
							.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo");
					double destY = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
							.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo");
					double destZ = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy())
							.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo");
					
					// Spark intensity increases with progress
					int sparkCount = (int) (2 + progressPercent * 6); // 2 to 8 sparks per tick
					for (int i = 0; i < sparkCount; i++) {
						double sparkX = destX + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
						double sparkY = destY + 1.0 + level.random.nextDouble() * 0.5;
						double sparkZ = destZ + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
						// Sparks use the same color as the charging ring
						level.sendParticles(ringParticle, sparkX, sparkY, sparkZ, 1, 0, 0, 0, 0);
					}
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
											// Screen shake effect before teleport
											serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
											serverPlayer.teleportTo(destinationLevel, targetX, targetY, targetZ, targetYaw, serverPlayer.getXRot());
											serverPlayer.connection.send(new ClientboundPlayerAbilitiesPacket(serverPlayer.getAbilities()));
											for (MobEffectInstance effect : serverPlayer.getActiveEffects())
												serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(serverPlayer.getId(), effect, false));
											serverPlayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
											// Brief disorientation effect (nausea for 1 second)
											serverPlayer.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 40, 0, false, false));
											// Brief blindness for dramatic effect (0.5 seconds)
											serverPlayer.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 10, 0, false, false));
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
									if (_ent instanceof ServerPlayer _serverPlayer) {
										_serverPlayer.connection.teleport(targetX, targetY, targetZ, _ent.getYRot(), _ent.getXRot());
										// Brief disorientation effect (nausea for 1 second)
										_serverPlayer.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 40, 0, false, false));
										// Brief blindness for dramatic effect (0.5 seconds)
										_serverPlayer.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 10, 0, false, false));
									}
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
						// === DRAMATIC TELEPORTATION BURST EFFECTS ===
						double targetX = ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo"));
						double targetY = ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo"));
						double targetZ = ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo"));

						// Color-changing burst particles (yellow to white flash)
						ParticleOptions burstParticle = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.3f), 2.0f);
						ParticleOptions flashParticle = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.8f), 1.5f);
						
						// Source location effects
						if (world instanceof ServerLevel _level) {
							// Massive color-changing burst at source
							for (int i = 0; i < 40; i++) {
								double spread = 0.8;
								_level.sendParticles(burstParticle, x + 0.5 + (Math.random() - 0.5) * spread, y + 1 + (Math.random() - 0.5) * spread, z + 0.5 + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.2);
							}
							// Lightning-like flash with POOF
							_level.sendParticles(ParticleTypes.POOF, x + 0.5, y + 1, z + 0.5, 30, 0.5, 0.5, 0.5, 0.15);
							// Expanding sphere effect with GLOW particles
							_level.sendParticles(ParticleTypes.GLOW, x + 0.5, y + 1, z + 0.5, 25, 1.0, 1.0, 1.0, 0.1);
							// Reversal portal particles (going down)
							_level.sendParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, y + 0.5, z + 0.5, 20, 0.6, 0.3, 0.6, 0.1);
						}

						// Destination location effects
						if (world instanceof ServerLevel _level) {
							// Massive color-changing burst at destination
							for (int i = 0; i < 40; i++) {
								double spread = 0.8;
								_level.sendParticles(burstParticle, targetX + 0.5 + (Math.random() - 0.5) * spread, targetY + 1 + (Math.random() - 0.5) * spread, targetZ + 0.5 + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.2);
							}
							// Lightning-like flash with POOF
							_level.sendParticles(ParticleTypes.POOF, targetX + 0.5, targetY + 1, targetZ + 0.5, 30, 0.5, 0.5, 0.5, 0.15);
							// Expanding sphere effect with GLOW particles
							_level.sendParticles(ParticleTypes.GLOW, targetX + 0.5, targetY + 1, targetZ + 0.5, 25, 1.0, 1.0, 1.0, 0.1);
							// Portal arrival effect
							_level.sendParticles(ParticleTypes.REVERSE_PORTAL, targetX + 0.5, targetY + 0.5, targetZ + 0.5, 20, 0.6, 0.3, 0.6, 0.1);
						}

						// Flash effect at both locations
						if (world instanceof ServerLevel _level) {
							for (int i = 0; i < 20; i++) {
								double spread = 1.2;
								_level.sendParticles(flashParticle, x + 0.5 + (Math.random() - 0.5) * spread, y + 1 + (Math.random() - 0.5) * spread, z + 0.5 + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.15);
								_level.sendParticles(flashParticle, targetX + 0.5 + (Math.random() - 0.5) * spread, targetY + 1 + (Math.random() - 0.5) * spread, targetZ + 0.5 + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.15);
							}
						}
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