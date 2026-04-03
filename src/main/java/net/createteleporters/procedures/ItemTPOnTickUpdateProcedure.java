package net.createteleporters.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.CreateteleportersMod;

public class ItemTPOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
		double FluidIntake = 0;
		double maxDist = 0;
		double dist = 0;
		{
			int _value = (blockstate.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _getip1 ? blockstate.getValue(_getip1) : -1) + 2;
			BlockPos _pos = BlockPos.containing(x, y, z);
			BlockState _bs = world.getBlockState(_pos);
			if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
				world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
		}
		dist = Math.abs(x - (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo"))
				+ Math.abs(y - (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo"))
				+ Math.abs(z - (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo"));
		maxDist = (double) CTPConfigConfiguration.ITEM_TP_RANGE.get();
		if (dist >= maxDist) {
			return "Out Of Range";
		}
		FluidIntake = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() * 1.5625;
		if (getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) >= FluidIntake && dist <= maxDist && !(Blocks.AIR.asItem() == (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem())) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.END_ROD, x, y, z, 4, 1, 1, 1, 0.01);
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.END_ROD, ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")),
						((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo") + 0.25),
						((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")), 4, 0.1, 0.1, 0.1, 0);
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
			if (world instanceof ILevelExtension _ext) {
				IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
				if (_fluidHandler != null)
					_fluidHandler.drain((int) FluidIntake, IFluidHandler.FluidAction.EXECUTE);
			}
			CreateteleportersMod.queueServerWork(5, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("xpo")),
							((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("ypo")),
							((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("zpo")),
							(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable)
					_itemHandlerModifiable.setStackInSlot(1, ItemStack.EMPTY);
			});
		}
		return "Distance OK!";
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