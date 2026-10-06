package net.createteleporters.integration;

import net.createteleporters.CreateteleportersMod;
import net.neoforged.fml.ModList;
import net.minecraft.core.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.function.IntPredicate;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SableAeronauticsIntegration {
	private static Boolean sableAvailable;
	private static Class<?> subLevelClass;
	private static Class<?> subLevelHelperClass;
	private static Class<?> poseClass;
	private static Class<?> serverSubLevelClass;
	private static Class<?> rigidBodyHandleClass;
	private static Class<?> vector3dcClass;
	private static Class<?> quaterniondcClass;
	private static Class<?> poseImplClass;
	private static Class<?> boundingBox3dcClass;
	private static Class<?> boundingBox3dClass;
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
				poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
				serverSubLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
				rigidBodyHandleClass = Class.forName("dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle");
				vector3dcClass = Class.forName("org.joml.Vector3dc");
				quaterniondcClass = Class.forName("org.joml.Quaterniondc");
				poseImplClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3d");
				boundingBox3dcClass = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3dc");
				boundingBox3dClass = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3d");
				sableAvailable = sableHelper != null;
				CreateteleportersMod.LOGGER.info("Sable/Create Aeronautics compatibility available: {}", sableAvailable);
			} catch (ReflectiveOperationException | LinkageError e) {
				sableAvailable = false;
			}
		}
		return sableAvailable;
	}

	public record WarpLaunch(String status, int cost, Vec3 origin, Vec3 destination, Quaterniond orientation,
			Vector3d linearVelocity, Vector3d angularVelocity, AABB bounds) {
	}
	public record WarpRelocation(String status, Vec3 destination) {}

	public static WarpLaunch beginShipWarp(Level level, BlockPos drivePos, Direction shipFront,
			int forwardOffset, int rightOffset,
			IntPredicate charge) {
		if (!ModList.get().isLoaded("aeronautics") || !isSableAvailable())
			return failedLaunch("aeronautics_missing", 0);
		if (!(level instanceof ServerLevel serverLevel))
			return failedLaunch("invalid_target", 0);

		Object subLevel = getContainingSubLevel(level, Vec3.atCenterOf(drivePos));
		if (subLevel == null || !serverSubLevelClass.isInstance(subLevel))
			return failedLaunch("not_on_ship", 0);

		int calculatedCost = 0;
		try {
			Object massTracker = serverSubLevelClass.getMethod("getMassTracker").invoke(subLevel);
			double mass = massTracker == null ? 0 : ((Number) massTracker.getClass().getMethod("getMass").invoke(massTracker)).doubleValue();
			if (!(mass > 0)) return failedLaunch("ship_not_ready", 0);

			Vec3 localDrive = Vec3.atCenterOf(drivePos);
			Vec3 origin = transformToGlobal(subLevel, localDrive);
			Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
			Vec3 requestedTarget = relativeTarget(pose, origin, shipFront, forwardOffset, rightOffset);
			if (requestedTarget == null) return failedLaunch("invalid_direction", 0);
			Quaterniond orientation = new Quaterniond((Quaterniondc) poseClass.getMethod("orientation").invoke(pose));
			PoseTarget alignedOrigin = poseAt(subLevel, pose, localDrive, origin, orientation);

			Clearance destination = findClearDestination(serverLevel, subLevel, pose, localDrive, requestedTarget, orientation);
			if (!destination.clear())
				return failedLaunch(destination.status(), 0);
			int cost = telejuiceCost(mass, origin.distanceTo(destination.target()));
			calculatedCost = cost;
			Object handle = rigidBodyHandleClass.getMethod("of", serverSubLevelClass).invoke(null, subLevel);
			if (handle == null || !(boolean) rigidBodyHandleClass.getMethod("isValid").invoke(handle))
				return failedLaunch("ship_not_ready", cost);
			Vector3d linearVelocity = velocity(handle, "getLinearVelocity");
			Vector3d angularVelocity = velocity(handle, "getAngularVelocity");
			if (!charge.test(cost)) return failedLaunch("not_enough_telejuice", cost);
			teleport(handle, alignedOrigin);
			zeroVelocity(handle);
			return new WarpLaunch("warping", cost, origin, destination.target(), orientation, linearVelocity, angularVelocity,
				alignedOrigin.bounds());
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to start Aeronautics hyperspace jump", e);
			return failedLaunch("ship_not_ready", calculatedCost);
		}
	}

	private static Vec3 relativeTarget(Object pose, Vec3 origin, Direction shipFront, int forwardOffset,
			int rightOffset) throws ReflectiveOperationException {
		Vec3 facing = new Vec3(shipFront.getStepX(), shipFront.getStepY(), shipFront.getStepZ());
		Vec3 globalFacing = (Vec3) poseClass.getMethod("transformNormal", Vec3.class).invoke(pose, facing);
		Vec3 forward = new Vec3(globalFacing.x, 0, globalFacing.z);
		if (forward.lengthSqr() < 1.0e-8) return null;
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		return origin.add(forward.scale(forwardOffset)).add(right.scale(rightOffset));
	}

	private static WarpLaunch failedLaunch(String status, int cost) {
		return new WarpLaunch(status, cost, null, null, null, null, null, null);
	}

	public static boolean holdShip(Level level, BlockPos drivePos, Vec3 origin, Quaterniond orientation) {
		if (!isSableAvailable()) return false;
		Object subLevel = getContainingSubLevel(level, Vec3.atCenterOf(drivePos));
		if (subLevel == null || !serverSubLevelClass.isInstance(subLevel)) return false;
		try {
			Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
			Object handle = rigidBodyHandleClass.getMethod("of", serverSubLevelClass).invoke(null, subLevel);
			if (handle == null || !(boolean) rigidBodyHandleClass.getMethod("isValid").invoke(handle)) return false;
			Vec3 currentDrive = (Vec3) poseClass.getMethod("transformPosition", Vec3.class)
				.invoke(pose, Vec3.atCenterOf(drivePos));
			Quaterniond currentOrientation = new Quaterniond((Quaterniondc) poseClass.getMethod("orientation").invoke(pose));
			if (currentDrive.distanceToSqr(origin) > 0.0001 || Math.abs(currentOrientation.dot(orientation)) < 0.999999)
				teleport(handle, poseAt(subLevel, pose, Vec3.atCenterOf(drivePos), origin, orientation));
			zeroVelocity(handle);
			return true;
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			return false;
		}
	}

	public static WarpRelocation relocateShip(Level level, BlockPos drivePos, Vec3 target, Quaterniond orientation,
			Vector3d linearVelocity, Vector3d angularVelocity) {
		if (!(level instanceof ServerLevel serverLevel) || !isSableAvailable()) return new WarpRelocation("ship_not_ready", null);
		Object subLevel = getContainingSubLevel(level, Vec3.atCenterOf(drivePos));
		if (subLevel == null || !serverSubLevelClass.isInstance(subLevel)) return new WarpRelocation("ship_not_ready", null);
		try {
			Vec3 localDrive = Vec3.atCenterOf(drivePos);
			Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
			Vec3 currentDrive = (Vec3) poseClass.getMethod("transformPosition", Vec3.class).invoke(pose, localDrive);
			Clearance destination = findClearDestination(serverLevel, subLevel, pose, localDrive, target, orientation);
			if (!destination.clear()) return new WarpRelocation(destination.status(), null);
			Object handle = rigidBodyHandleClass.getMethod("of", serverSubLevelClass).invoke(null, subLevel);
			if (handle == null || !(boolean) rigidBodyHandleClass.getMethod("isValid").invoke(handle)) return new WarpRelocation("ship_not_ready", null);
			List<ShipEntity> entities = captureShipEntities(serverLevel, subLevel);
			teleport(handle, destination.pose());
			moveShipEntities(entities, destination.target().subtract(currentDrive));
			rigidBodyHandleClass.getMethod("addLinearAndAngularVelocity", vector3dcClass, vector3dcClass)
				.invoke(handle, linearVelocity, angularVelocity);
			return new WarpRelocation("success", destination.target());
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to finish Aeronautics hyperspace jump", e);
			return new WarpRelocation("ship_not_ready", null);
		}
	}

	private static List<ShipEntity> captureShipEntities(ServerLevel level, Object subLevel) {
		List<ShipEntity> entities = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity.isRemoved() || getTrackingSubLevel(entity) != subLevel) continue;
			entities.add(new ShipEntity(entity, entity.position()));
		}
		return entities;
	}

	private static void moveShipEntities(List<ShipEntity> entities, Vec3 displacement) {
		for (ShipEntity shipEntity : entities) {
			Entity entity = shipEntity.entity();
			if (entity.isRemoved()) continue;
			Vec3 destination = shipEntity.worldPosition().add(displacement);
			entity.teleportTo(destination.x, destination.y, destination.z);
		}
	}

	public static void restoreVelocity(Level level, BlockPos drivePos, Vector3d linearVelocity, Vector3d angularVelocity) {
		if (!isSableAvailable()) return;
		Object subLevel = getContainingSubLevel(level, Vec3.atCenterOf(drivePos));
		if (subLevel == null || !serverSubLevelClass.isInstance(subLevel)) return;
		try {
			Object handle = rigidBodyHandleClass.getMethod("of", serverSubLevelClass).invoke(null, subLevel);
			if (handle == null || !(boolean) rigidBodyHandleClass.getMethod("isValid").invoke(handle)) return;
			zeroVelocity(handle);
			rigidBodyHandleClass.getMethod("addLinearAndAngularVelocity", vector3dcClass, vector3dcClass)
				.invoke(handle, linearVelocity, angularVelocity);
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to restore Aeronautics ship momentum", e);
		}
	}

	public static List<ServerPlayer> getPlayersOnShip(Level level, BlockPos drivePos) {
		if (!(level instanceof ServerLevel serverLevel) || !isSableAvailable()) return List.of();
		Object subLevel = getContainingSubLevel(level, Vec3.atCenterOf(drivePos));
		if (subLevel == null) return List.of();
		return serverLevel.players().stream().filter(player -> getTrackingSubLevel(player) == subLevel).toList();
	}

	private static Clearance findClearDestination(ServerLevel level, Object subLevel, Object pose, Vec3 localDrive,
			Vec3 target, Quaterniond orientation) throws ReflectiveOperationException {
		String failure = "invalid_target";
		boolean chunksLoaded = false;
		for (int offset = 0; offset <= 64; offset++) {
			for (int sign : offset == 0 ? new int[]{1} : new int[]{1, -1}) {
				Vec3 candidate = target.add(0, offset * sign, 0);
				PoseTarget proposed = poseAt(subLevel, pose, localDrive, candidate, orientation);
				Clearance result = clearance(level, subLevel, proposed, !chunksLoaded);
				if (result.clear() || result.status().equals("ship_too_large")) return result;
				if (result.status().equals("destination_blocked")) {
					failure = "destination_blocked";
					chunksLoaded = true;
				}
			}
		}
		return new Clearance(failure, null, null);
	}

	private static PoseTarget poseAt(Object subLevel, Object currentPose, Vec3 localDrive, Vec3 target,
			Quaterniond orientation) throws ReflectiveOperationException {
		Vector3d position = new Vector3d((Vector3dc) poseClass.getMethod("position").invoke(currentPose));
		Vector3d rotationPoint = new Vector3d((Vector3dc) poseClass.getMethod("rotationPoint").invoke(currentPose));
		Vector3d scale = new Vector3d((Vector3dc) poseClass.getMethod("scale").invoke(currentPose));
		Object proposed = newPose(position, orientation, rotationPoint, scale);
		Vec3 provisionalDrive = (Vec3) poseClass.getMethod("transformPosition", Vec3.class).invoke(proposed, localDrive);
		position.add(target.x - provisionalDrive.x, target.y - provisionalDrive.y, target.z - provisionalDrive.z);
		proposed = newPose(position, orientation, rotationPoint, scale);
		return new PoseTarget(position, orientation, shipBounds(subLevel, proposed), target);
	}

	private static Object newPose(Vector3d position, Quaterniond orientation, Vector3d rotationPoint, Vector3d scale)
			throws ReflectiveOperationException {
		return poseImplClass.getConstructor(Vector3d.class, Quaterniond.class, Vector3d.class, Vector3d.class)
			.newInstance(position, orientation, rotationPoint, scale);
	}

	private static AABB shipBounds(Object subLevel, Object pose) throws ReflectiveOperationException {
		Object plot = subLevel.getClass().getMethod("getPlot").invoke(subLevel);
		Object box = plot.getClass().getMethod("getBoundingBox").invoke(plot);
		double[] xs = {number(box, "minX"), number(box, "maxX") + 1};
		double[] ys = {number(box, "minY"), number(box, "maxY") + 1};
		double[] zs = {number(box, "minZ"), number(box, "maxZ") + 1};
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		Method transform = poseClass.getMethod("transformPosition", Vec3.class);
		for (double x : xs) for (double y : ys) for (double z : zs) {
			Vec3 point = (Vec3) transform.invoke(pose, new Vec3(x, y, z));
			minX = Math.min(minX, point.x); minY = Math.min(minY, point.y); minZ = Math.min(minZ, point.z);
			maxX = Math.max(maxX, point.x); maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
		}
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static double number(Object object, String method) throws ReflectiveOperationException {
		return ((Number) object.getClass().getMethod(method).invoke(object)).doubleValue();
	}

	private static Clearance clearance(ServerLevel level, Object ownShip, PoseTarget proposed, boolean loadChunks) {
		AABB bounds = proposed.bounds().deflate(0.001);
		if (bounds.minY < level.getMinBuildHeight() || bounds.maxY >= level.getMaxBuildHeight()
				|| !level.getWorldBorder().isWithinBounds(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ))
				|| !level.getWorldBorder().isWithinBounds(BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)))
			return new Clearance("invalid_target", null, null);
		int minChunkX = ((int) Math.floor(bounds.minX)) >> 4;
		int maxChunkX = ((int) Math.floor(bounds.maxX)) >> 4;
		int minChunkZ = ((int) Math.floor(bounds.minZ)) >> 4;
		int maxChunkZ = ((int) Math.floor(bounds.maxZ)) >> 4;
		if ((long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1) > 64)
			return new Clearance("ship_too_large", null, null);
		if (loadChunks)
			for (int x = minChunkX; x <= maxChunkX; x++) for (int z = minChunkZ; z <= maxChunkZ; z++) level.getChunk(x, z);
		if (level.getBlockCollisions(null, bounds).iterator().hasNext())
			return new Clearance("destination_blocked", null, null);
		if (intersectsAnotherShip(level, ownShip, bounds))
			return new Clearance("destination_blocked", null, null);
		return new Clearance("clear", proposed, proposed.driveTarget());
	}

	private static boolean intersectsAnotherShip(Level level, Object ownShip, AABB bounds) {
		try {
			Object sableBounds = boundingBox3dClass.getConstructor(AABB.class).newInstance(bounds);
			Object result = sableHelper.getClass().getMethod("getAllIntersecting", Level.class, boundingBox3dcClass)
				.invoke(sableHelper, level, sableBounds);
			if (result instanceof Iterable<?> ships)
				for (Object ship : ships) if (ship != ownShip) return true;
			return false;
		} catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
			CreateteleportersMod.LOGGER.warn("Failed to validate Aeronautics ship clearance", e);
			return true;
		}
	}

	private static void teleport(Object handle, PoseTarget target) throws ReflectiveOperationException {
		rigidBodyHandleClass.getMethod("teleport", vector3dcClass, quaterniondcClass)
			.invoke(handle, target.position(), target.orientation());
	}

	private static Vector3d velocity(Object handle, String method) throws ReflectiveOperationException {
		return new Vector3d((Vector3dc) rigidBodyHandleClass.getMethod(method).invoke(handle));
	}

	private static void zeroVelocity(Object handle) throws ReflectiveOperationException {
		Vector3d linear = velocity(handle, "getLinearVelocity").negate();
		Vector3d angular = velocity(handle, "getAngularVelocity").negate();
		rigidBodyHandleClass.getMethod("addLinearAndAngularVelocity", vector3dcClass, vector3dcClass)
			.invoke(handle, linear, angular);
	}

	private record PoseTarget(Vector3d position, Quaterniond orientation, AABB bounds, Vec3 driveTarget) {}
	private record Clearance(String status, PoseTarget pose, Vec3 target) {
		private boolean clear() { return pose != null; }
	}
	private record ShipEntity(Entity entity, Vec3 worldPosition) {}

	public static int telejuiceCost(double mass, double distance) {
		double cost = 1000 + mass * distance * 2;
		return !Double.isFinite(cost) || cost >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.ceil(cost);
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

	public static void detachEntity(Entity entity) {
		if (!isSableAvailable()) return;
		Object sourceSubLevel = getTrackingSubLevel(entity);
		if (sourceSubLevel != null) popEntityLocal(sourceSubLevel, entity);
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

	public static AABB getEntityBounds(LevelAccessor world, AABB localReferenceBox, Entity entity) {
		AABB entityBox = entity.getBoundingBox();
		if (entityBox.intersects(localReferenceBox) || !isSableAvailable() || !(world instanceof Level level)) {
			return entityBox;
		}

		Object subLevel = getContainingSubLevel(level, localReferenceBox.getCenter());
		if (subLevel == null) {
			return entityBox;
		}

		return transformAabbToLocal(subLevel, entityBox);
	}

	public static Vec3 resolveWorldPosition(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof Level level)) {
			return new Vec3(x, y, z);
		}
		return resolveParticlePosition(level, x, y, z);
	}

	private static Object getTrackingSubLevel(Entity entity) {
		try {
			Method method = sableHelper.getClass().getMethod("getTrackingOrVehicleSubLevel", Entity.class);
			return method.invoke(sableHelper, entity);
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
