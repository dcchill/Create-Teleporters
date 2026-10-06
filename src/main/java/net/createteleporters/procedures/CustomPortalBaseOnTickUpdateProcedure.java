package net.createteleporters.procedures;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.integration.ImmersivePortalsIntegration;
import net.createteleporters.integration.SableAeronauticsIntegration;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.createteleporters.block.QuantumPortalBlockBlock;
import net.createteleporters.network.CustomPortalEffectPayload;

public class CustomPortalBaseOnTickUpdateProcedure {

	public static String execute(LevelAccessor world, double x, double y, double z) {
		
		ScalablePortalCheckerProcedure.execute(world, x, y, z);
		BlockPos basePos = BlockPos.containing(x, y, z);
		CustomPortalTeleportMode.getOrMigrate(world, basePos);
		ensurePortalChunksLoaded(world, basePos);

		if (getBlockNBTLogic(world, basePos, "portalActive")) {
			
				int portalWidth = getBlockNBTInt(world, basePos, "portalWidth");
				int portalHeight = getBlockNBTInt(world, basePos, "portalHeight");
				int minExtent = getBlockNBTInt(world, basePos, "portalMinExtent");
				int maxExtent = getBlockNBTInt(world, basePos, "portalMaxExtent");
				String rotation = getBlockNBTString(world, basePos, "rotation");

				
				boolean coordinateMode = CustomPortalTeleportMode.isCoordinateMode(world, basePos);
				boolean useImmersivePortals = !coordinateMode && CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.get();
				boolean immersiveMode = useImmersivePortals && ImmersivePortalsIntegration.isImmersivePortalsLoaded();
				
				
				int interiorMin = minExtent + 1;
				int interiorMax = maxExtent - 1;

				
				BlockEntity linkedBE = world.getBlockEntity(basePos);
				boolean isLinked = linkedBE != null && linkedBE.getPersistentData().getBoolean("isLinked");

				
				if (isLinked && !coordinateMode) {
					int linkedX = linkedBE.getPersistentData().getInt("linkedX");
					int linkedY = linkedBE.getPersistentData().getInt("linkedY");
					int linkedZ = linkedBE.getPersistentData().getInt("linkedZ");
					BlockEntity remoteBE = null;
					String linkedDimId = linkedBE.getPersistentData().getString("linkedDim");
					ResourceLocation linkedDimLoc = ResourceLocation.tryParse(linkedDimId);
					if (world instanceof ServerLevel sourceLevel && linkedDimLoc != null) {
						ServerLevel linkedLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, linkedDimLoc));
						if (linkedLevel != null) {
							remoteBE = linkedLevel.getBlockEntity(new BlockPos(linkedX, linkedY, linkedZ));
						}
					}
					boolean remoteActive = remoteBE != null && remoteBE.getPersistentData().getBoolean("portalActive");

					if (!remoteActive) {
						
						clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);
						if (getBlockNBTLogic(world, basePos, "immersivePortalCreated")) {
							removeTrackedImmersivePortal(world, x, y, z);
						}
						setPortalVisualActive(world, basePos, false);
						return "Waiting for Linked Portal";
					}
				}

				
				boolean ipPortalActive = false;

				if (immersiveMode) {
					
					
					
					
					BlockEntity be = world.getBlockEntity(BlockPos.containing(x, y, z));
					boolean canCreateIP = false;

					if (isLinked) {
						
						boolean hasTpLink = hasAdvancedTpLink(world, basePos) ||
										   hasAdvancedTpLinkAtLinkedPortal(world, linkedBE);
						canCreateIP = hasTpLink;
					}

					if (canCreateIP) {
						
						clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);

						String targetDim = be != null ? be.getPersistentData().getString("linkedDim") : "minecraft:overworld";
						double tx = be != null ? be.getPersistentData().getDouble("linkedX") : x;
						double ty = be != null ? be.getPersistentData().getDouble("linkedY") : y;
						double tz = be != null ? be.getPersistentData().getDouble("linkedZ") : z;

						
						
						boolean needsCreation = !getBlockNBTLogic(world, basePos, "immersivePortalCreated")
							|| world instanceof ServerLevel level && level.getGameTime() % 20 == 0
								&& !ImmersivePortalsIntegration.hasImmersivePortal(world, x, y, z);
						boolean needsOrientationRebuild = be != null && be.getPersistentData().getInt("immersivePortalCompatVersion") < ImmersivePortalsIntegration.COMPAT_VERSION;

						if (needsCreation || needsOrientationRebuild) {
							net.createteleporters.CreateteleportersMod.LOGGER.info("Creating new IP portal...");
							boolean created = ImmersivePortalsIntegration.createImmersivePortal(
								world, x, y, z, rotation,
								portalWidth, portalHeight, minExtent, maxExtent,
								targetDim, tx, ty, tz
							);
							net.createteleporters.CreateteleportersMod.LOGGER.info("IP portal creation result: {}", created);
							if (created && be != null) {
								ipPortalActive = true;
							}
						} else {
							ipPortalActive = true;
						}
					} else {
						
						
						if (getBlockNBTLogic(world, basePos, "immersivePortalCreated")) {
							removeTrackedImmersivePortal(world, x, y, z);
						}
						
						clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);
						setPortalVisualActive(world, basePos, false);
					}
				} else {
					
					
					removeTrackedImmersivePortal(world, x, y, z);

					
					
					DyeColor portalColor = DyeColor.byName(QuantumPortalBlockBlock.getStoredPortalColorName(world.getBlockEntity(basePos)), DyeColor.PURPLE);
					updateQuantumPortalBlocks(world, basePos, rotation, portalHeight, minExtent, maxExtent,
						CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState().setValue(QuantumPortalBlockBlock.COLOR, portalColor));
					setPortalVisualActive(world, basePos, true);

				}
				setPortalVisualActive(world, basePos, !immersiveMode || ipPortalActive);
				
				
				AABB portalArea;
				int portalInnerHeight = portalHeight - 1;

				if ("east".equals(rotation) || "west".equals(rotation)) {
					portalArea = new AABB(x, y + 1, z + interiorMin, x + 1, y + portalInnerHeight + 1, z + interiorMax + 1);
				} else {
					portalArea = new AABB(x + interiorMin, y + 1, z, x + interiorMax + 1, y + portalInnerHeight + 1, z + 1);
				}

				
				String targetDim;
				double tx, ty, tz, yaw;

				if (coordinateMode) {
					ItemStack invStack = (itemFromBlockInventory(world, basePos, 0).copy());

					if (invStack.isEmpty() || !(invStack.getItem() instanceof net.createteleporters.item.ADVTplinkItem)) {
						setPortalVisualActive(world, basePos, false);
						return "Missing Advanced TP Link";
					}

					CompoundTag cd = invStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
					targetDim = cd.getString("dimension").trim();
					tx = cd.getDouble("xpo");
					ty = cd.getDouble("ypo");
					tz = cd.getDouble("zpo");
					yaw = cd.getDouble("yawpo");
				} else {
					if (!isLinked) {
						setPortalVisualActive(world, basePos, false);
						return "Portal Not Linked";
					}

					boolean hasTpLink = hasAdvancedTpLink(world, basePos) ||
									   hasAdvancedTpLinkAtLinkedPortal(world, linkedBE);

					if (!hasTpLink) {
						setPortalVisualActive(world, basePos, false);
						return "Missing Advanced TP Link";
					}

					tx = linkedBE.getPersistentData().getDouble("linkedX");
					ty = linkedBE.getPersistentData().getDouble("linkedY");
					tz = linkedBE.getPersistentData().getDouble("linkedZ");
					targetDim = linkedBE.getPersistentData().getString("linkedDim");
					yaw = linkedBE.getPersistentData().getDouble("linkedYaw");
				}
				int effectColor = DyeColor.byName(QuantumPortalBlockBlock.getStoredPortalColorName(
					world.getBlockEntity(basePos)), DyeColor.PURPLE).getTextureDiffuseColor();

				
				
				if (ipPortalActive) {
					
					for (Entity entityiterator : SableAeronauticsIntegration.getEntities(world, portalArea, e -> true)) {
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

				
				for (Entity entityiterator : SableAeronauticsIntegration.getEntities(world, portalArea, e -> true)) {
					if (shouldIgnorePortalTeleport(entityiterator)) {
						if (entityiterator instanceof net.minecraft.server.level.ServerPlayer player
								&& entityiterator.getPersistentData().contains("TeleportCharge"))
							PacketDistributor.sendToPlayer(player,
								new CustomPortalEffectPayload(CustomPortalEffectPayload.CANCEL, effectColor, 0));
						entityiterator.getPersistentData().remove("TeleportCharge");
						entityiterator.getPersistentData().remove("TeleportChargeDuration");
						continue;
					}

					
					CompoundTag entityData = entityiterator.getPersistentData();
					if (entityData.contains("PortalTeleportCooldown")) {
						int cooldown = entityData.getInt("PortalTeleportCooldown");
						if (cooldown > 0) {
							entityData.putInt("PortalTeleportCooldown", cooldown - 1);
							continue; 
						}
					}

					
					int chargeTime = entityData.contains("TeleportCharge") ? entityData.getInt("TeleportCharge") : 0;
					
					
					AABB entityBounds = SableAeronauticsIntegration.getEntityBounds(world, portalArea, entityiterator);
					if (isInsidePortal(portalArea, entityBounds)) {
						
						if (chargeTime <= 0) {
							
							int chargeDuration = entityiterator instanceof net.minecraft.server.level.ServerPlayer
								? calculatePlayerChargeTicks(entityiterator, targetDim, tx, ty, tz, world)
								: 10;
							entityData.putInt("TeleportCharge", chargeDuration);
							entityData.putInt("TeleportChargeDuration", chargeDuration);
							if (entityiterator instanceof net.minecraft.server.level.ServerPlayer player)
								PacketDistributor.sendToPlayer(player,
									new CustomPortalEffectPayload(CustomPortalEffectPayload.CHARGE, effectColor, chargeDuration + 1));
						} else {
							
							chargeTime--;
							entityData.putInt("TeleportCharge", chargeTime);
							
							
							if (chargeTime <= 0) {
								
								String playerTeamName = null;
								if (entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									Scoreboard scoreboard = serverPlayer.getScoreboard();
									Team playerTeam = scoreboard.getPlayerTeam(serverPlayer.getScoreboardName());
									if (playerTeam != null) {
										playerTeamName = playerTeam.getName();
									}
								}

								ServerLevel arrivalLevel = null;
								
								if (world instanceof ServerLevel _level) {
									ResourceLocation dimLoc = ResourceLocation.tryParse(targetDim);
									if (dimLoc != null) {
										ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
										ServerLevel targetLevel = _level.getServer().getLevel(targetDimKey);
										if (targetLevel != null) {
											
												SableAeronauticsIntegration.teleportEntity(entityiterator, targetLevel, tx + 0.5, ty + 1, tz + 0.5, (float) yaw);
												arrivalLevel = entityiterator.level() == targetLevel ? targetLevel : null;
										} else {
											
											_level.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null),
												"execute in " + targetDim + " run tp " + entityiterator.getStringUUID() + " " + (tx + 0.5) + " " + (ty + 1) + " " + (tz + 0.5));
										}
									} else {
												SableAeronauticsIntegration.teleportEntity(entityiterator, _level, tx + 0.5, ty + 1, tz + 0.5, (float) yaw);
												arrivalLevel = entityiterator.level() == _level ? _level : null;
									}
								}
								if (arrivalLevel == null) {
									entityData.remove("TeleportCharge");
									entityData.remove("TeleportChargeDuration");
									continue;
								}
								drainTelejuice(world, basePos, 4);
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

								
								entityiterator.getPersistentData().putInt("PortalTeleportCooldown", 20);
								entityData.remove("TeleportCharge"); 
								entityData.remove("TeleportChargeDuration");

								if (world instanceof ServerLevel sourceLevel)
									spawnTeleportBurst(sourceLevel, x + 0.5, y + 1, z + 0.5);
								if (arrivalLevel != null)
									spawnTeleportBurst(arrivalLevel, tx + 0.5, ty + 1, tz + 0.5);
								if (entityiterator instanceof net.minecraft.server.level.ServerPlayer player)
									PacketDistributor.sendToPlayer(player,
										new CustomPortalEffectPayload(CustomPortalEffectPayload.ARRIVAL, effectColor, 15));

								
								if (playerTeamName != null && entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
									Scoreboard scoreboard = serverPlayer.getScoreboard();
									
									net.minecraft.world.scores.PlayerTeam playerTeam = scoreboard.getPlayerTeam(playerTeamName);
									if (playerTeam != null) {
										scoreboard.addPlayerToTeam(serverPlayer.getScoreboardName(), playerTeam);
									}
								}

								if (world instanceof ServerLevel sourceLevel)
									sourceLevel.playSound(null, BlockPos.containing(x, y + 1, z),
										net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.2f, 1.5f);
								if (arrivalLevel != null)
									arrivalLevel.playSound(null, BlockPos.containing(tx, ty, tz),
										net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.2f, 1.5f);
							}
						}
					} else {
						
						if (chargeTime > 0) {
							if (entityiterator instanceof net.minecraft.server.level.ServerPlayer player)
								PacketDistributor.sendToPlayer(player,
									new CustomPortalEffectPayload(CustomPortalEffectPayload.CANCEL, effectColor, 0));
							entityData.remove("TeleportCharge");
							entityData.remove("TeleportChargeDuration");
						}
					}
				}

				return "Portal Ready";
		} else {
			setPortalVisualActive(world, basePos, false);
			
			String rotation = getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation");
			int portalWidth = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalWidth");
			int portalHeight = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalHeight");
			int minExtent = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalMinExtent");
			int maxExtent = getBlockNBTInt(world, BlockPos.containing(x, y, z), "portalMaxExtent");

			removeTrackedImmersivePortal(world, x, y, z);
			clearQuantumPortalBlocks(world, x, y, z, rotation, portalWidth, portalHeight, minExtent, maxExtent);
		}
		
		String errorReason = getBlockNBTString(world, BlockPos.containing(x, y, z), "portalError");
		if (!errorReason.isEmpty()) {
			return errorReason;
		}
		return "Portal Frame Incorrect";
	}

	private static void clearQuantumPortalBlocks(LevelAccessor world, double x, double y, double z, String rotation,
			int portalWidth, int portalHeight, int minExtent, int maxExtent) {
		if (portalWidth <= 0 || portalHeight <= 0) {
			portalHeight = 4;
			minExtent = -2;
			maxExtent = 2;
		}
		updateQuantumPortalBlocks(world, BlockPos.containing(x, y, z), rotation, portalHeight, minExtent, maxExtent, Blocks.AIR.defaultBlockState());
	}

	private static int updateQuantumPortalBlocks(LevelAccessor world, BlockPos basePos, String rotation,
			int portalHeight, int minExtent, int maxExtent, BlockState targetState) {
		if (!(world instanceof ServerLevel level)) return 0;
		boolean eastWest = "east".equals(rotation) || "west".equals(rotation);
		if (!eastWest && !"north".equals(rotation) && !"south".equals(rotation)) return 0;
		int changed = 0;
		for (int y = 1; y < portalHeight; y++) {
			for (int horizontal = minExtent + 1; horizontal < maxExtent; horizontal++) {
				BlockPos pos = eastWest ? basePos.offset(0, y, horizontal) : basePos.offset(horizontal, y, 0);
				BlockState current = level.getBlockState(pos);
				boolean isPortal = current.is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get());
				if (targetState.isAir() && !isPortal) continue;
				// Preserve pane connections when only the portal color changes.
				BlockState replacement = isPortal && !targetState.isAir()
					? current.setValue(QuantumPortalBlockBlock.COLOR, targetState.getValue(QuantumPortalBlockBlock.COLOR)) : targetState;
				if (current == replacement) continue;
				Clearable.tryClear(level.getBlockEntity(pos));
				if (level.setBlock(pos, Block.updateFromNeighbourShapes(replacement, level, pos), 3)) changed++;
			}
		}
		return changed;
	}

	private static void removeTrackedImmersivePortal(LevelAccessor world, double x, double y, double z) {
		BlockPos basePos = BlockPos.containing(x, y, z);
		if (!getBlockNBTLogic(world, basePos, "immersivePortalCreated")) {
			return;
		}

		ImmersivePortalsIntegration.removeImmersivePortal(world, x, y, z);
	}

	private static void spawnTeleportBurst(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.END_ROD, x, y, z, 36, 0.8, 1.1, 0.8, 0.18);
		level.sendParticles(ParticleTypes.POOF, x, y, z, 22, 0.55, 0.75, 0.55, 0.12);
		level.sendParticles(ParticleTypes.GLOW, x, y, z, 20, 1.0, 1.25, 1.0, 0.1);
	}

	private static void setPortalVisualActive(LevelAccessor world, BlockPos pos, boolean active) {
		setSyncedFlag(world, pos, "portalVisualActive", active);
	}

	private static int calculatePlayerChargeTicks(Entity entity, String targetDim, double tx, double ty, double tz,
			LevelAccessor world) {
		double distance = Math.sqrt(entity.distanceToSqr(tx + 0.5, ty + 1, tz + 0.5));
		boolean crossDimension = world instanceof Level level
			&& !level.dimension().location().toString().equals(targetDim);
		return Math.min(200, 10 + (int) (distance / 16.0) + (crossDimension ? 20 : 0));
	}

	private static void setSyncedFlag(LevelAccessor world, BlockPos pos, String key, boolean value) {
		if (world.isClientSide())
			return;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null || blockEntity.getPersistentData().getBoolean(key) == value)
			return;
		blockEntity.getPersistentData().putBoolean(key, value);
		blockEntity.setChanged();
		if (world instanceof Level level)
			level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
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

	public static boolean drainTelejuice(LevelAccessor level, BlockPos pos, int amount) {
		if (level instanceof ILevelExtension ext) {
			IFluidHandler fluidHandler = ext.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
			return fluidHandler != null && fluidHandler.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount() == amount;
		}
		return false;
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
		if (!(world instanceof ServerLevel sourceLevel) || linkedBE == null) {
			return false;
		}

		ResourceLocation linkedDimLoc = ResourceLocation.tryParse(linkedBE.getPersistentData().getString("linkedDim"));
		if (linkedDimLoc == null) {
			return false;
		}

		ServerLevel linkedLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, linkedDimLoc));
		if (linkedLevel == null) {
			return false;
		}

		BlockPos linkedPos = BlockPos.containing(
			linkedBE.getPersistentData().getDouble("linkedX"),
			linkedBE.getPersistentData().getDouble("linkedY"),
			linkedBE.getPersistentData().getDouble("linkedZ")
		);
		return hasAdvancedTpLink(linkedLevel, linkedPos);
	}

	public static void ensurePortalChunksLoaded(LevelAccessor world, BlockPos basePos) {
		if (!(world instanceof ServerLevel sourceLevel)) {
			return;
		}

		forceChunkSquare(sourceLevel, basePos, true);

		BlockEntity blockEntity = sourceLevel.getBlockEntity(basePos);
		if (blockEntity == null) {
			return;
		}

		CompoundTag nbt = blockEntity.getPersistentData();
		if (!nbt.getBoolean("isLinked")) {
			return;
		}

		ResourceLocation linkedDimLoc = ResourceLocation.tryParse(nbt.getString("linkedDim"));
		if (linkedDimLoc == null) {
			return;
		}

		ServerLevel linkedLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, linkedDimLoc));
		if (linkedLevel == null) {
			return;
		}

		BlockPos linkedPos = BlockPos.containing(nbt.getDouble("linkedX"), nbt.getDouble("linkedY"), nbt.getDouble("linkedZ"));
		forceChunkSquare(linkedLevel, linkedPos, true);
	}

	public static void releasePortalChunks(LevelAccessor world, BlockPos basePos) {
		if (!(world instanceof ServerLevel sourceLevel)) {
			return;
		}

		forceChunkSquare(sourceLevel, basePos, false);

		BlockEntity blockEntity = sourceLevel.getBlockEntity(basePos);
		if (blockEntity == null) {
			return;
		}

		CompoundTag nbt = blockEntity.getPersistentData();
		if (!nbt.getBoolean("isLinked")) {
			return;
		}

		ResourceLocation linkedDimLoc = ResourceLocation.tryParse(nbt.getString("linkedDim"));
		if (linkedDimLoc == null) {
			return;
		}

		ServerLevel linkedLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, linkedDimLoc));
		if (linkedLevel == null) {
			return;
		}

		BlockPos linkedPos = BlockPos.containing(nbt.getDouble("linkedX"), nbt.getDouble("linkedY"), nbt.getDouble("linkedZ"));
		forceChunkSquare(linkedLevel, linkedPos, false);
	}

	private static void forceChunkSquare(ServerLevel level, BlockPos centerPos, boolean force) {
		ChunkPos centerChunk = new ChunkPos(centerPos);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setChunkForced(centerChunk.x + dx, centerChunk.z + dz, force);
			}
		}
	}

	private static Direction rotationToNormal(String rotation) {
		return switch (rotation) {
			case "east" -> Direction.EAST;
			case "west" -> Direction.WEST;
			case "south" -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
	}

	private static BlockPos horizontalDirection(String rotation) {
		return switch (rotation) {
			case "south" -> new BlockPos(-1, 0, 0);
			case "east" -> new BlockPos(0, 0, 1);
			case "west" -> new BlockPos(0, 0, -1);
			default -> new BlockPos(1, 0, 0);
		};
	}

	private static boolean isInsidePortal(AABB portalArea, AABB entityBounds) {
		return entityBounds.intersects(portalArea) && entityBounds.minY >= portalArea.minY
			&& entityBounds.minY < portalArea.maxY;
	}

	private static boolean shouldIgnorePortalTeleport(Entity entity) {
		if (entity instanceof CarriageContraptionEntity) {
			return true;
		}

		return entity.isPassenger() && entity.getRootVehicle() instanceof CarriageContraptionEntity;
	}
}
