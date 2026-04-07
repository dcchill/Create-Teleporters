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
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.Scoreboard;

import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.integration.ImmersivePortalsIntegration;
import net.createteleporters.procedures.BindCustomPortalProcedure;

public class CustomPortalBaseOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		// Use scalable portal checker instead of fixed size
		ScalablePortalCheckerProcedure.execute(world, x, y, z);

		if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "portalActive")) {
			if (4 <= getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {
				// Get portal dimensions from NBT
				int portalWidth = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalWidth");
				int portalHeight = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalHeight");
				int minExtent = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalMinExtent");
				int maxExtent = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalMaxExtent");
				String rotation = getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation");

				// Check if Immersive Portals compatibility is enabled
				boolean useImmersivePortals = CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.get();
				
				// Calculate interior dimensions (needed for both IP and vanilla)
				int interiorMin = minExtent + 1;
				int interiorMax = maxExtent - 1;
				int fillHeight = portalHeight - 1;

				// Check if this portal is linked to another portal
				BlockEntity linkedBE = world.getBlockEntity(BlockPos.containing(x, y, z));
				boolean isLinked = linkedBE != null && linkedBE.getPersistentData().getBoolean("isLinked");

				// For linked portals, only create portals when BOTH sides are valid
				if (isLinked) {
					int linkedX = linkedBE.getPersistentData().getInt("linkedX");
					int linkedY = linkedBE.getPersistentData().getInt("linkedY");
					int linkedZ = linkedBE.getPersistentData().getInt("linkedZ");
					BlockEntity remoteBE = world.getBlockEntity(new BlockPos(linkedX, linkedY, linkedZ));
					boolean remoteActive = remoteBE != null && remoteBE.getPersistentData().getBoolean("portalActive");

					if (!remoteActive) {
						// Linked portal not ready yet — wait
						clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);
						if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "immersivePortalCreated")) {
							removeTrackedImmersivePortal(world, x, y, z);
							if (linkedBE != null)
								linkedBE.getPersistentData().putBoolean("immersivePortalCreated", false);
						}
						return "Waiting for linked portal";
					}
				}

				if (useImmersivePortals && ImmersivePortalsIntegration.isImmersivePortalsLoaded()) {
					// Use Immersive Portals integration - NO quantum portal blocks
					// IP handles the portal rendering and teleportation automatically
					clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);
					// Ambient particles while immersive portals are enabled.
					if (world instanceof ServerLevel _level) {
						// Spawn particles across the full width and height of the portal interior
						boolean northSouth = "north".equals(rotation) || "south".equals(rotation);
						
						// Spawn particles every 2 blocks for a more sparse effect
						for (int h = 1; h <= fillHeight; h += 2) {
							for (int i = interiorMin; i <= interiorMax; i += 2) {
								if (_level.random.nextFloat() < 0.5f) continue; // 50% chance to skip
								double particleX = northSouth ? x + i + 0.5 : x + 0.5;
								double particleY = y + h + 0.5;
								double particleZ = northSouth ? z + 0.5 : z + i + 0.5;
								_level.sendParticles(ParticleTypes.PORTAL, particleX, particleY, particleZ, 1, 0.5,
										0.5, 0.5, 0.01);
							}
						}
					}

					BlockEntity be = world.getBlockEntity(BlockPos.containing(x, y, z));
					String targetDim = be != null ? be.getPersistentData().getString("linkedDim") : "minecraft:overworld";
					double tx = be != null ? be.getPersistentData().getDouble("linkedX") : x;
					double ty = be != null ? be.getPersistentData().getDouble("linkedY") : y;
					double tz = be != null ? be.getPersistentData().getDouble("linkedZ") : z;

					// Create Immersive Portals portal (only needs to be done once)
					// But verify it actually exists first
					boolean needsCreation = !getBlockNBTLogic(world, BlockPos.containing(x, y, z), "immersivePortalCreated");
					
					if (needsCreation) {
						net.createteleporters.CreateteleportersMod.LOGGER.info("Creating new IP portal...");
						boolean created = ImmersivePortalsIntegration.createImmersivePortal(
							world, x, y, z, rotation,
							portalWidth, portalHeight, minExtent, maxExtent,
							targetDim, tx, ty, tz
						);
						net.createteleporters.CreateteleportersMod.LOGGER.info("IP portal creation result: {}", created);
						if (created && be != null) {
							be.getPersistentData().putBoolean("immersivePortalCreated", true);
						}
					}
				} else {
					// Config may have been turned off while IP portals were active.
					// Remove existing IP portals and switch back to quantum portal blocks.
					removeTrackedImmersivePortal(world, x, y, z);

					// Use vanilla quantum portal blocks
					// Fill portal interior only (not the frame)
					if ("east".equals(rotation) || "west".equals(rotation)) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"fill ~ ~1 ~" + interiorMin + " ~ ~" + fillHeight + " ~" + interiorMax + " createteleporters:quantum_portal_block");
					} else if ("north".equals(rotation) || "south".equals(rotation)) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"fill ~" + interiorMin + " ~1 ~ ~" + interiorMax + " ~" + fillHeight + " ~ createteleporters:quantum_portal_block");
					}
				}
				
				// Drain fluid
				if (world instanceof ILevelExtension _ext) {
					IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
					if (_fluidHandler != null)
						_fluidHandler.drain(4, IFluidHandler.FluidAction.EXECUTE);
				}

				// Build AABB based on portal interior extents
				AABB portalArea;
				int portalInnerHeight = portalHeight - 1;

				if ("east".equals(rotation) || "west".equals(rotation)) {
					portalArea = new AABB(x, y + 1, z + interiorMin, x + 1, y + portalInnerHeight + 1, z + interiorMax + 1);
				} else {
					portalArea = new AABB(x + interiorMin, y + 1, z, x + interiorMax + 1, y + portalInnerHeight + 1, z + 1);
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

				// Get teleportation target - prefer linked portal coordinates over item data
				String targetDim;
				double tx, ty, tz, yaw;

				// Check if portal-to-portal binding is forced by config
				boolean forceBinding = BindCustomPortalProcedure.isForcePortalToPortalBinding();

				if (forceBinding) {
					// When binding is forced, read coordinates from the Advanced TP Link in inventory
					ItemStack invStack = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy());
					
					if (invStack.isEmpty() || !(invStack.getItem() instanceof net.createteleporters.item.ADVTplinkItem)) {
						return "Missing Advanced TP Link"; // Require TP Link when binding is forced
					}
					
					CompoundTag cd = invStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
					targetDim = cd.getString("dimension");
					tx = cd.getDouble("xpo");
					ty = cd.getDouble("ypo");
					tz = cd.getDouble("zpo");
					yaw = cd.getDouble("yawpo");
				} else if (isLinked) {
					// Portal is explicitly linked to another portal
					// Check if either portal has the Advanced TP Link in its inventory
					boolean hasTpLink = hasAdvancedTpLink(world, BlockPos.containing(x, y, z)) ||
									   hasAdvancedTpLinkAtLinkedPortal(world, linkedBE);

					if (!hasTpLink) {
						return "Missing Advanced TP Link"; // Require TP Link for linked portals
					}

					// Use linked portal coordinates
					tx = linkedBE.getPersistentData().getDouble("linkedX");
					ty = linkedBE.getPersistentData().getDouble("linkedY");
					tz = linkedBE.getPersistentData().getDouble("linkedZ");
					targetDim = linkedBE.getPersistentData().getString("linkedDim");
					yaw = linkedBE.getPersistentData().getDouble("linkedYaw");
				} else {
					// Fall back to item data (for backwards compatibility when binding not forced)
					ItemStack invStack = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy());
					CompoundTag cd = invStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
					targetDim = cd.getString("dimension");
					tx = cd.getDouble("xpo");
					ty = cd.getDouble("ypo");
					tz = cd.getDouble("zpo");
					yaw = cd.getDouble("yawpo");
				}

				// Check if using Immersive Portals - if so, skip vanilla teleportation
				// IP handles teleportation automatically when entities walk through portal entity
				if (useImmersivePortals) {
					// IP handles teleportation - just maintain cooldown to prevent loops
					for (Entity entityiterator : world.getEntities(null, portalArea)) {
						CompoundTag entityData = entityiterator.getPersistentData();
						if (entityData.contains("PortalTeleportCooldown")) {
							int cooldown = entityData.getInt("PortalTeleportCooldown");
							if (cooldown > 0) {
								entityData.putInt("PortalTeleportCooldown", cooldown - 1);
							}
						}
					}
					return "Portal Ready (IP)";
				}

				// Vanilla teleportation (original code)
				for (Entity entityiterator : world.getEntities(null, portalArea)) {
					// Decrement teleportation cooldown if present
					CompoundTag entityData = entityiterator.getPersistentData();
					if (entityData.contains("PortalTeleportCooldown")) {
						int cooldown = entityData.getInt("PortalTeleportCooldown");
						if (cooldown > 0) {
							entityData.putInt("PortalTeleportCooldown", cooldown - 1);
							continue; // Skip this entity, still on cooldown
						}
					}

					// Check if entity is charging up for teleport
					int chargeTime = entityData.contains("TeleportCharge") ? entityData.getInt("TeleportCharge") : 0;
					
					// use the entity's feet (minY of bounding box) to prevent premature triggers
					double feetY = entityiterator.getBoundingBox().minY;
					if (feetY + 1e-6 >= (y + 1) && feetY <= (y + portalInnerHeight + 1) + 1e-6) {
						// Start or continue charging
						if (chargeTime <= 0) {
							// First tick in portal - start charging
							entityData.putInt("TeleportCharge", 10); // 10 ticks = 0.5 seconds
						} else {
							// Continue charging
							chargeTime--;
							entityData.putInt("TeleportCharge", chargeTime);
							
							// Apply vision effects during charge
							if (entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
								// Nausea creates a swirling/warping vision effect
								serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
									net.minecraft.world.effect.MobEffects.CONFUSION, 15, 1, false, false, false));
								// Brief darkness for dramatic effect
								serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
									net.minecraft.world.effect.MobEffects.DARKNESS, 10, 0, false, false, false));
							}
							
							// Teleport when charge completes
							if (chargeTime <= 0) {
								// Save player's team before teleportation (Bug fix: preserve team assignment)
								String playerTeamName = null;
								if (entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									Scoreboard scoreboard = serverPlayer.getScoreboard();
									Team playerTeam = scoreboard.getPlayerTeam(serverPlayer.getScoreboardName());
									if (playerTeam != null) {
										playerTeamName = playerTeam.getName();
									}
								}

								if (world instanceof ServerLevel _level)
									_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
											("execute in " + targetDim
													+ (" run tp " + entityiterator.getStringUUID() + " "
															+ (tx + 0.5) + " "
															+ (ty + 1) + " "
															+ (tz + 0.5))));
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

								// Set teleportation cooldown (20 ticks = 1 second)
								entityiterator.getPersistentData().putInt("PortalTeleportCooldown", 20);
								entityData.remove("TeleportCharge"); // Reset charge counter

								// === ENHANCED TELEPORTATION EFFECTS ===
								// Dramatic burst at source
								if (world instanceof ServerLevel _level) {
									_level.sendParticles(ParticleTypes.END_ROD, x + 0.5, y + 1, z + 0.5, 30, 0.6, 0.6, 0.6, 0.15);
									_level.sendParticles(ParticleTypes.POOF, x + 0.5, y + 1, z + 0.5, 20, 0.4, 0.4, 0.4, 0.1);
									_level.sendParticles(ParticleTypes.GLOW, x + 0.5, y + 1, z + 0.5, 15, 0.8, 0.8, 0.8, 0.08);
								}

								// Dramatic burst at destination
								if (world instanceof ServerLevel _level) {
									_level.sendParticles(ParticleTypes.END_ROD, tx + 0.5, ty + 1, tz + 0.5, 30, 0.6, 0.6, 0.6, 0.15);
									_level.sendParticles(ParticleTypes.POOF, tx + 0.5, ty + 1, tz + 0.5, 20, 0.4, 0.4, 0.4, 0.1);
									_level.sendParticles(ParticleTypes.GLOW, tx + 0.5, ty + 1, tz + 0.5, 15, 0.8, 0.8, 0.8, 0.08);
								}

								// Apply disorientation effect to players
								if (entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									// Brief nausea (swirl effect) for 2 seconds
									serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
										net.minecraft.world.effect.MobEffects.CONFUSION, 40, 0, false, false));
									// Very brief blindness for dramatic "re-materializing" effect
									serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
										net.minecraft.world.effect.MobEffects.BLINDNESS, 8, 0, false, false));
								}

								// Restore player's team after teleportation (Bug fix: preserve team assignment)
								if (playerTeamName != null && entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									Scoreboard scoreboard = serverPlayer.getScoreboard();
									// Get the team by name and re-add the player
									net.minecraft.world.scores.PlayerTeam playerTeam = scoreboard.getPlayerTeam(playerTeamName);
									if (playerTeam != null) {
										scoreboard.addPlayerToTeam(serverPlayer.getScoreboardName(), playerTeam);
									}
								}

								// Original particle effects (keeping for compatibility)
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.END_ROD, x, (y + 1), z, 20, 1.5, 1.5, 1.5, 0.1);
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 20, 1.5, 1.5, 1.5, 0.1);
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(x, y + 1, z), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
									} else {
										_level.playLocalSound(x, (y + 1), z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
									}
								}
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(tx, ty, tz), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
									} else {
										_level.playLocalSound(tx, ty, tz, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
									}
								}
							}
						}
					} else {
						// Entity left portal before charging completed - reset charge
						if (chargeTime > 0) {
							entityData.remove("TeleportCharge");
						}
					}
				}

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
			// Clear portal blocks when frame is invalid
			String rotation = getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation");
			int portalWidth = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalWidth");
			int portalHeight = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalHeight");
			int minExtent = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalMinExtent");
			int maxExtent = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalMaxExtent");

			// Check if Immersive Portals compatibility is enabled
			boolean useImmersivePortals = CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.get();
			
			if (useImmersivePortals && ImmersivePortalsIntegration.isImmersivePortalsLoaded()) {
				// Remove Immersive Portals portal
				removeTrackedImmersivePortal(world, x, y, z);
				clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);
			} else if (portalWidth > 0 && portalHeight > 0) {
				// If compatibility was disabled, ensure old IP portals are removed.
				removeTrackedImmersivePortal(world, x, y, z);

				// Use interior dimensions (not the frame)
				int interiorMin = minExtent + 1;
				int interiorMax = maxExtent - 1;
				int fillHeight = portalHeight - 1;

				if ("east".equals(rotation) || "west".equals(rotation)) {
					// Horizontal axis is Z, X stays same as base
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~ ~1 ~" + interiorMin + " ~ ~" + fillHeight + " ~" + interiorMax + " air replace createteleporters:quantum_portal_block");
				} else if ("north".equals(rotation) || "south".equals(rotation)) {
					// Horizontal axis is X, Z stays same as base
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~" + interiorMin + " ~1 ~ ~" + interiorMax + " ~" + fillHeight + " ~ air replace createteleporters:quantum_portal_block");
				}
			} else {
				// Fallback to old 5x5 clearing
				if ("east".equals(rotation) || "west".equals(rotation)) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~ ~1 ~-1 ~ ~3 ~1 air replace createteleporters:quantum_portal_block");
				} else if ("north".equals(rotation) || "south".equals(rotation)) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"fill ~-1 ~1 ~ ~1 ~3 ~ air replace createteleporters:quantum_portal_block");
				}
			}
		}
		return "Portal Frame Incorrect";
	}

	private static void clearQuantumPortalBlocks(LevelAccessor world, double x, double y, double z, String rotation,
			int portalWidth, int portalHeight, int minExtent, int maxExtent) {
		if (!(world instanceof ServerLevel level)) {
			return;
		}

		if (portalWidth > 0 && portalHeight > 0) {
			int interiorMin = minExtent + 1;
			int interiorMax = maxExtent - 1;
			int fillHeight = portalHeight - 1;

			if ("east".equals(rotation) || "west".equals(rotation)) {
				level.getServer().getCommands().performPrefixedCommand(
						new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, level, 4, "",
								Component.literal(""), level.getServer(), null).withSuppressedOutput(),
						"fill ~ ~1 ~" + interiorMin + " ~ ~" + fillHeight + " ~" + interiorMax
								+ " air replace createteleporters:quantum_portal_block");
				return;
			}

			if ("north".equals(rotation) || "south".equals(rotation)) {
				level.getServer().getCommands().performPrefixedCommand(
						new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, level, 4, "",
								Component.literal(""), level.getServer(), null).withSuppressedOutput(),
						"fill ~" + interiorMin + " ~1 ~ ~" + interiorMax + " ~" + fillHeight
								+ " ~ air replace createteleporters:quantum_portal_block");
				return;
			}
		}

		if ("east".equals(rotation) || "west".equals(rotation)) {
			level.getServer().getCommands().performPrefixedCommand(
					new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, level, 4, "",
							Component.literal(""), level.getServer(), null).withSuppressedOutput(),
					"fill ~ ~1 ~-1 ~ ~3 ~1 air replace createteleporters:quantum_portal_block");
		} else if ("north".equals(rotation) || "south".equals(rotation)) {
			level.getServer().getCommands().performPrefixedCommand(
					new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, level, 4, "",
							Component.literal(""), level.getServer(), null).withSuppressedOutput(),
					"fill ~-1 ~1 ~ ~1 ~3 ~ air replace createteleporters:quantum_portal_block");
		}
	}

	private static void removeTrackedImmersivePortal(LevelAccessor world, double x, double y, double z) {
		BlockPos basePos = BlockPos.containing(x, y, z);
		if (!getBlockNBTLogic(world, basePos, "immersivePortalCreated")) {
			return;
		}

		ImmersivePortalsIntegration.removeImmersivePortal(world, x, y, z);
		BlockEntity be = world.getBlockEntity(basePos);
		if (be != null) {
			be.getPersistentData().putBoolean("immersivePortalCreated", false);
		}
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getBoolean(tag);
		return false;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension ext) {
			IFluidHandler fluidHandler = ext.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
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
	
	private static int getBlockNBTInt(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getInt(tag);
		return 0;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
	
	/**
	 * Checks if this portal has an Advanced TP Link in its inventory.
	 */
	private static boolean hasAdvancedTpLink(LevelAccessor world, BlockPos pos) {
		ItemStack stack = itemFromBlockInventory(world, pos, 0);
		return !stack.isEmpty() && stack.getItem() instanceof net.createteleporters.item.ADVTplinkItem;
	}
	
	/**
	 * Checks if the linked portal has an Advanced TP Link in its inventory.
	 */
	private static boolean hasAdvancedTpLinkAtLinkedPortal(LevelAccessor world, BlockEntity linkedBE) {
		int linkedX = linkedBE.getPersistentData().getInt("linkedX");
		int linkedY = linkedBE.getPersistentData().getInt("linkedY");
		int linkedZ = linkedBE.getPersistentData().getInt("linkedZ");
		return hasAdvancedTpLink(world, new BlockPos(linkedX, linkedY, linkedZ));
	}
}
