package net.createteleporters.integration;

import net.createteleporters.CreateteleportersMod;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class SableAeronauticsIntegration {
	private static Boolean sableAvailable;
	private static Class<?> subLevelClass;
	private static Class<?> subLevelHelperClass;
	private static Class<?> entitySubLevelUtilClass;
	private static Class<?> poseClass;
	private static Object sableHelper;

	private SableAeronauticsIntegration() {
	}

	public static boolean isSableAvailable() {
		if (sableAvailable == null) {
			try {
				Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
				Field helperField = sableClass.getField("HELPER");
				sableHelper = helperField.get(null);
				subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
				subLevelHelperClass = Class.forName("dev.ryanhcode.sable.api.SubLevelHelper");
				entitySubLevelUtilClass = Class.forName("dev.ryanhcode.sable.api.entity.EntitySubLevelUtil");
				poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
				sableAvailable = sableHelper != null;
				CreateteleportersMod.LOGGER.info("Sable/Create Aeronautics compatibility available: {}", sableAvailable);
			} catch (ReflectiveOperationException | LinkageError e) {
				sableAvailable = false;
			}
		}
		return sableAvailable;
	}

	public static void teleportEntity(Entity entity, ServerLevel targetLevel, double targetX, double targetY, double targetZ, float yaw) {
		if (!isSableAvailable()) {
			entity.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), yaw, entity.getXRot());
			return;
		}

		Object sourceSubLevel = getTrackingSubLevel(entity);
		if (sourceSubLevel != null) {
			popEntityLocal(sourceSubLevel, entity);
		}

		Vec3 rawTarget = new Vec3(targetX, targetY, targetZ);
		Object targetSubLevel = getContainingSubLevel(targetLevel, rawTarget);
		Vec3 globalTarget = targetSubLevel == null ? rawTarget : transformToGlobal(targetSubLevel, rawTarget);

		entity.teleportTo(targetLevel, globalTarget.x, globalTarget.y, globalTarget.z, Set.of(), yaw, entity.getXRot());

		if (targetSubLevel != null) {
			pushEntityLocal(targetSubLevel, entity);
		}
	}

	public static void teleportEntity(Entity entity, double targetX, double targetY, double targetZ, float yaw) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			teleportEntity(entity, serverLevel, targetX, targetY, targetZ, yaw);
			return;
		}
		entity.teleportTo(targetX, targetY, targetZ);
	}

	public static ItemEntity spawnItem(ServerLevel level, double targetX, double targetY, double targetZ, ItemStack stack) {
		Vec3 rawTarget = new Vec3(targetX, targetY, targetZ);
		Object targetSubLevel = getContainingSubLevel(level, rawTarget);
		Vec3 globalTarget = targetSubLevel == null ? rawTarget : transformToGlobal(targetSubLevel, rawTarget);

		ItemEntity entityToSpawn = new ItemEntity(level, globalTarget.x, globalTarget.y, globalTarget.z, stack);
		entityToSpawn.setPickUpDelay(10);
		entityToSpawn.setUnlimitedLifetime();

		if (targetSubLevel != null) {
			pushEntityLocal(targetSubLevel, entityToSpawn);
		}
		level.addFreshEntity(entityToSpawn);
		return entityToSpawn;
	}

	public static Vec3 resolveParticlePosition(Level level, double targetX, double targetY, double targetZ) {
		Vec3 rawTarget = new Vec3(targetX, targetY, targetZ);
		Object targetSubLevel = getContainingSubLevel(level, rawTarget);
		return targetSubLevel == null ? rawTarget : transformToGlobal(targetSubLevel, rawTarget);
	}

	public static List<Entity> getEntities(LevelAccessor world, AABB localBox, Predicate<Entity> predicate) {
		if (!(world instanceof Level level)) {
			return List.of();
		}

		List<Entity> entities = new ArrayList<>(level.getEntitiesOfClass(Entity.class, localBox, predicate));
		if (!isSableAvailable()) {
			return entities;
		}

		Vec3 localCenter = localBox.getCenter();
		Object subLevel = getContainingSubLevel(level, localCenter);
		if (subLevel == null) {
			return entities;
		}

		AABB globalBox = transformAabbToGlobal(subLevel, localBox).inflate(0.25);
		Set<UUID> seen = new HashSet<>();
		for (Entity entity : entities) {
			seen.add(entity.getUUID());
		}

		for (Entity entity : level.getEntitiesOfClass(Entity.class, globalBox, predicate)) {
			if (seen.add(entity.getUUID())) {
				entities.add(entity);
			}
		}

		return entities;
	}

	public static double getEntityFeetY(LevelAccessor world, AABB localReferenceBox, Entity entity) {
		AABB entityBox = entity.getBoundingBox();
		if (entityBox.intersects(localReferenceBox) || !isSableAvailable() || !(world instanceof Level level)) {
			return entityBox.minY;
		}

		Object subLevel = getContainingSubLevel(level, localReferenceBox.getCenter());
		if (subLevel == null) {
			return entityBox.minY;
		}

		return transformAabbToLocal(subLevel, entityBox).minY;
	}

	public static Vec3 resolveWorldPosition(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof Level level)) {
			return new Vec3(x, y, z);
		}
		return resolveParticlePosition(level, x, y, z);
	}

	private static Object getTrackingSubLevel(Entity entity) {
		try {
			Method method = entitySubLevelUtilClass.getMethod("getTrackingOrVehicleSubLevel", Entity.class);
			return method.invoke(null, entity);
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			return null;
		}
	}

	private static Object getContainingSubLevel(Level level, Vec3 rawPosition) {
		if (!isSableAvailable()) {
			return null;
		}

		try {
			Method method = sableHelper.getClass().getMethod("getContaining", Level.class, Position.class);
			return method.invoke(sableHelper, level, rawPosition);
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			return null;
		}
	}

	private static Vec3 transformToGlobal(Object subLevel, Vec3 rawPosition) {
		try {
			Method logicalPoseMethod = subLevel.getClass().getMethod("logicalPose");
			Object pose = logicalPoseMethod.invoke(subLevel);
			Method transformPositionMethod = poseClass.getMethod("transformPosition", Vec3.class);
			Object transformed = transformPositionMethod.invoke(pose, rawPosition);
			if (transformed instanceof Vec3 vec3) {
				return vec3;
			}
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to transform Sable sub-level position", e);
		}
		return rawPosition;
	}

	private static Vec3 transformToLocal(Object subLevel, Vec3 globalPosition) {
		try {
			Method logicalPoseMethod = subLevel.getClass().getMethod("logicalPose");
			Object pose = logicalPoseMethod.invoke(subLevel);
			Method transformPositionMethod = poseClass.getMethod("transformPositionInverse", Vec3.class);
			Object transformed = transformPositionMethod.invoke(pose, globalPosition);
			if (transformed instanceof Vec3 vec3) {
				return vec3;
			}
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to inverse-transform Sable sub-level position", e);
		}
		return globalPosition;
	}

	private static AABB transformAabbToGlobal(Object subLevel, AABB localBox) {
		Vec3 first = transformToGlobal(subLevel, new Vec3(localBox.minX, localBox.minY, localBox.minZ));
		double minX = first.x;
		double minY = first.y;
		double minZ = first.z;
		double maxX = first.x;
		double maxY = first.y;
		double maxZ = first.z;

		double[] xs = {localBox.minX, localBox.maxX};
		double[] ys = {localBox.minY, localBox.maxY};
		double[] zs = {localBox.minZ, localBox.maxZ};
		for (double px : xs) {
			for (double py : ys) {
				for (double pz : zs) {
					Vec3 transformed = transformToGlobal(subLevel, new Vec3(px, py, pz));
					minX = Math.min(minX, transformed.x);
					minY = Math.min(minY, transformed.y);
					minZ = Math.min(minZ, transformed.z);
					maxX = Math.max(maxX, transformed.x);
					maxY = Math.max(maxY, transformed.y);
					maxZ = Math.max(maxZ, transformed.z);
				}
			}
		}

		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static AABB transformAabbToLocal(Object subLevel, AABB globalBox) {
		Vec3 first = transformToLocal(subLevel, new Vec3(globalBox.minX, globalBox.minY, globalBox.minZ));
		double minX = first.x;
		double minY = first.y;
		double minZ = first.z;
		double maxX = first.x;
		double maxY = first.y;
		double maxZ = first.z;

		double[] xs = {globalBox.minX, globalBox.maxX};
		double[] ys = {globalBox.minY, globalBox.maxY};
		double[] zs = {globalBox.minZ, globalBox.maxZ};
		for (double px : xs) {
			for (double py : ys) {
				for (double pz : zs) {
					Vec3 transformed = transformToLocal(subLevel, new Vec3(px, py, pz));
					minX = Math.min(minX, transformed.x);
					minY = Math.min(minY, transformed.y);
					minZ = Math.min(minZ, transformed.z);
					maxX = Math.max(maxX, transformed.x);
					maxY = Math.max(maxY, transformed.y);
					maxZ = Math.max(maxZ, transformed.z);
				}
			}
		}

		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static void pushEntityLocal(Object subLevel, Entity entity) {
		invokeEntityLocalTransform("pushEntityLocal", subLevel, entity);
	}

	private static void popEntityLocal(Object subLevel, Entity entity) {
		invokeEntityLocalTransform("popEntityLocal", subLevel, entity);
	}

	private static void invokeEntityLocalTransform(String methodName, Object subLevel, Entity entity) {
		try {
			Method method = subLevelHelperClass.getMethod(methodName, subLevelClass, Entity.class);
			method.invoke(null, subLevel, entity);
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to {} Sable entity {}", methodName, entity.getStringUUID(), e);
		}
	}
}
