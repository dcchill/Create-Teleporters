package net.createteleporters.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import net.createteleporters.network.CreateteleportersModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PocketGenProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (CreateteleportersModVariables.MapVariables.get(world).shouldGen == true) {
			if ((entity.level().dimension()) == ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("createteleporters:pocket_dimension"))) {
				if (world instanceof ServerLevel _serverworld) {
					StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("createteleporters", "pocketdimensionwalls"));
					if (template != null) {
						template.placeInWorld(_serverworld, BlockPos.containing(entity.getX() - 7.5, entity.getY() - 3, entity.getZ() - 7.5), BlockPos.containing(entity.getX() - 7.5, entity.getY() - 3, entity.getZ() - 7.5),
								new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
					}
				}
				CreateteleportersModVariables.MapVariables.get(world).shouldGen = false;
				CreateteleportersModVariables.MapVariables.get(world).syncData(world);
			}
		}
	}
}