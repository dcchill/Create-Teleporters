package net.createteleporters.procedures;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.createteleporters.init.CreateteleportersModItems;

import org.joml.Vector3f;

public class BlockTeleporterOnTickUpdateProcedure {
	private static final Logger LOGGER = LogManager.getLogger(BlockTeleporterOnTickUpdateProcedure.class);
	
	public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		double maxDist = 0;
		double dist = 0;
		BlockState block = Blocks.AIR.defaultBlockState();
		block = (world.getBlockState(BlockPos.containing(x + (getDirectionFromBlockState(blockstate)).getStepX(), y + (getDirectionFromBlockState(blockstate)).getStepY(), z + (getDirectionFromBlockState(blockstate)).getStepZ())));
		if (CreateteleportersModItems.ADV_TPLINK.get() == (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem()) {
			if (1000 <= getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) && world instanceof Level _level13 && _level13.hasNeighborSignal(BlockPos.containing(x, y, z))
					&& !block.is(BlockTags.create(ResourceLocation.parse("createteleporters:non_teleportable")))
					&& (world.getBlockState(BlockPos.containing((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo"),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo") + 1,
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")))).getBlock() == Blocks.AIR) {
				if (world instanceof ILevelExtension _ext) {
					IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
					if (_fluidHandler != null)
						_fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
				}
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							("clone "
									+ ((int) Math.floor(x + (getDirectionFromBlockState(blockstate)).getStepX()) + " " + (int) Math.floor(y + (getDirectionFromBlockState(blockstate)).getStepY()) + " "
											+ (int) Math.floor(z + (getDirectionFromBlockState(blockstate)).getStepZ()) + " ")
									+ ((int) Math.floor(x + (getDirectionFromBlockState(blockstate)).getStepX()) + " " + (int) Math.floor(y + (getDirectionFromBlockState(blockstate)).getStepY()) + " "
											+ (int) Math.floor(z + (getDirectionFromBlockState(blockstate)).getStepZ()) + " ")
									+ ((int) Math.floor((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")) + " "
											+ (int) Math.floor((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo") + 1) + " "
											+ (int) Math.floor((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")))));
				{
					BlockPos _bp = BlockPos.containing(x + (getDirectionFromBlockState(blockstate)).getStepX(), y + (getDirectionFromBlockState(blockstate)).getStepY(), z + (getDirectionFromBlockState(blockstate)).getStepZ());
					BlockState _bs = Blocks.AIR.defaultBlockState();
					BlockState _bso = world.getBlockState(_bp);
					for (Property<?> _propertyOld : _bso.getProperties()) {
						Property _propertyNew = _bs.getBlock().getStateDefinition().getProperty(_propertyOld.getName());
						if (_propertyNew != null && _bs.getValue(_propertyNew) != null)
							try {
								_bs = _bs.setValue(_propertyNew, _bso.getValue(_propertyOld));
							} catch (Exception e) {
								LOGGER.debug("Failed to copy property '{}' during block teleport: {}", _propertyOld.getName(), e.getMessage());
							}
					}
					BlockEntity _be = world.getBlockEntity(_bp);
					CompoundTag _bnbt = null;
					if (_be != null) {
						_bnbt = _be.saveWithFullMetadata(world.registryAccess());
						_be.setRemoved();
					}
					world.setBlock(_bp, _bs, 3);
					if (_bnbt != null) {
						_be = world.getBlockEntity(_bp);
						if (_be != null) {
							try {
								_be.loadWithComponents(_bnbt, world.registryAccess());
							} catch (Exception e) {
								LOGGER.debug("Failed to load block entity after block teleport: {}", e.getMessage());
							}
						}
					}
				}
				// Color-changing burst particles (yellow theme)
				ParticleOptions burstParticle = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.3f), 2.0f);
				ParticleOptions flashParticle = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.8f), 1.5f);

				// Source location effects
				double srcX = Math.floor(x + (getDirectionFromBlockState(blockstate)).getStepX()) + 0.5;
				double srcY = Math.floor(y + (getDirectionFromBlockState(blockstate)).getStepY()) + 1.0;
				double srcZ = Math.floor(z + (getDirectionFromBlockState(blockstate)).getStepZ()) + 0.5;

				if (world instanceof ServerLevel _level) {
					// Burst at source
					for (int i = 0; i < 25; i++) {
						double spread = 0.6;
						_level.sendParticles(burstParticle, srcX + (Math.random() - 0.5) * spread, srcY + (Math.random() - 0.5) * spread, srcZ + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.15);
					}
				}

				// Destination location effects (floor + 0.5 to center on block, +1.0 for Y)
				double destX = Math.floor((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")) + 0.5;
				double destY = Math.floor((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo")) + 1.0;
				double destZ = Math.floor((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")) + 0.5;

				if (world instanceof ServerLevel _level) {
					// Burst at destination
					for (int i = 0; i < 30; i++) {
						double spread = 0.8;
						_level.sendParticles(burstParticle, destX + (Math.random() - 0.5) * spread, destY + (Math.random() - 0.5) * spread, destZ + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.15);
					}
					// Flash effect
					for (int i = 0; i < 15; i++) {
						double spread = 1.0;
						_level.sendParticles(flashParticle, destX + (Math.random() - 0.5) * spread, destY + (Math.random() - 0.5) * spread, destZ + (Math.random() - 0.5) * spread, 1, 0, 0, 0, 0.1);
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
										(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo") + 1,
										(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")),
								net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5);
					} else {
						_level.playLocalSound(((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")),
								((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo") + 1),
								((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")),
								net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, (float) 0.2, (float) 1.5, false);
					}
				}
			}
		}
	}

	private static Direction getDirectionFromBlockState(BlockState blockState) {
		Property<?> prop = blockState.getBlock().getStateDefinition().getProperty("facing");
		if (prop instanceof DirectionProperty dp)
			return blockState.getValue(dp);
		prop = blockState.getBlock().getStateDefinition().getProperty("axis");
		return prop instanceof EnumProperty ep && ep.getPossibleValues().toArray()[0] instanceof Direction.Axis ? Direction.fromAxisAndDirection((Direction.Axis) blockState.getValue(ep), Direction.AxisDirection.POSITIVE) : Direction.NORTH;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
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