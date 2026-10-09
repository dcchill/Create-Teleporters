package net.createteleporters.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.configuration.CTPClientConfiguration;
import net.createteleporters.integration.ImmersivePortalsIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = CreateteleportersMod.MODID, value = Dist.CLIENT)
public final class CustomPortalSurfaceClient {
	private static final Set<CustomPortalBaseBlockEntity> CONTROLLERS = new HashSet<>();
	private static volatile Map<LevelLightEngine, WorldSurface> surfaces = Map.of();
	private static boolean dirty;
	private static boolean enabled;

	private CustomPortalSurfaceClient() {
	}

	public static boolean isEnabled() {
		return CTPClientConfiguration.LIQUID_CUSTOM_PORTAL.get() && !ImmersivePortalsIntegration.isImmersivePortalsLoaded();
	}

	public static void track(CustomPortalBaseBlockEntity controller) {
		CONTROLLERS.add(controller);
		dirty = true;
	}

	public static void remove(CustomPortalBaseBlockEntity controller) {
		if (CONTROLLERS.remove(controller)) dirty = true;
	}

	static BlockPos owner(BlockAndTintGetter world, BlockPos pos) {
		WorldSurface surface = surfaces.get(world.getLightEngine());
		return surface == null ? null : surface.owners.get(pos);
	}

	@SubscribeEvent
	public static void unload(LevelEvent.Unload event) {
		if (!(event.getLevel() instanceof Level world) || !world.isClientSide()) return;
		CONTROLLERS.removeIf(controller -> controller.getLevel() == world);
		Map<LevelLightEngine, WorldSurface> remaining = new HashMap<>(surfaces);
		remaining.remove(world.getLightEngine());
		surfaces = Map.copyOf(remaining);
		dirty = true;
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			CONTROLLERS.clear();
			surfaces = Map.of();
			return;
		}
		boolean currentEnabled = isEnabled();
		if (currentEnabled != enabled) dirty = true;
		if (!dirty) return;
		enabled = currentEnabled;
		dirty = false;
		Map<Level, Map<BlockPos, BlockPos>> byWorld = new HashMap<>();
		CONTROLLERS.removeIf(CustomPortalBaseBlockEntity::isRemoved);
		if (enabled) for (CustomPortalBaseBlockEntity controller : CONTROLLERS) {
			var data = controller.getPersistentData();
			int min = data.getInt("portalMinExtent") + 1;
			int max = data.getInt("portalMaxExtent");
			int top = data.getInt("portalHeight");
			String rotation = data.getString("rotation");
			boolean northSouth = "north".equals(rotation) || "south".equals(rotation);
			if (!data.getBoolean("portalVisualActive") || data.getBoolean("immersivePortalCreated")
					|| max <= min || max - min > 21 || top <= 1 || top > 22
					|| !northSouth && !"east".equals(rotation) && !"west".equals(rotation)) continue;
			BlockPos base = controller.getBlockPos();
			Map<BlockPos, BlockPos> owners = byWorld.computeIfAbsent(controller.getLevel(), world -> new HashMap<>());
			for (int h = min; h < max; h++) for (int y = 1; y < top; y++) {
				BlockPos pos = northSouth ? base.offset(h, y, 0) : base.offset(0, y, h);
				owners.put(pos, base);
			}
		}
		Map<LevelLightEngine, WorldSurface> next = new HashMap<>();
		byWorld.forEach((world, owners) -> next.put(world.getLightEngine(), new WorldSurface(world, Map.copyOf(owners))));
		Map<LevelLightEngine, WorldSurface> previous = surfaces;
		surfaces = Map.copyOf(next);
		Set<LevelLightEngine> worlds = new HashSet<>(previous.keySet());
		worlds.addAll(next.keySet());
		for (LevelLightEngine key : worlds) {
			WorldSurface before = previous.get(key), after = next.get(key);
			Map<BlockPos, BlockPos> oldOwners = before == null ? Map.of() : before.owners;
			Map<BlockPos, BlockPos> newOwners = after == null ? Map.of() : after.owners;
			Set<BlockPos> changed = new HashSet<>(oldOwners.keySet());
			changed.addAll(newOwners.keySet());
			Set<SectionPos> sections = new HashSet<>();
			for (BlockPos pos : changed) if (!Objects.equals(oldOwners.get(pos), newOwners.get(pos))) sections.add(SectionPos.of(pos));
			Level world = after == null ? before.level : after.level;
			for (SectionPos section : sections) {
				BlockPos pos = section.origin().offset(8, 8, 8);
				var state = world.getBlockState(pos);
				world.sendBlockUpdated(pos, state, state, 8);
			}
		}
	}

	private record WorldSurface(Level level, Map<BlockPos, BlockPos> owners) {
	}
}
