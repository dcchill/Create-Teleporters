package net.createteleporters.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public class ImmersivePortalsIntegration {
    public static final int COMPAT_VERSION = 2;
    private static final String CLUSTER_ID = "immersivePortalClusterId";
    private static final String OWNER_PREFIX = "createteleporters:";
    private static Boolean isImmersivePortalsLoaded;

    public static boolean isImmersivePortalsLoaded() {
        if (isImmersivePortalsLoaded == null) {
            isImmersivePortalsLoaded = ModList.get().isLoaded("immersive_portals") ||
                ModList.get().isLoaded("immersive_portals_core") ||
                ModList.get().isLoaded("immersiveportals") || ModList.get().isLoaded("immersive-portals");
            CreateteleportersMod.LOGGER.info("Immersive Portals loaded: {}", isImmersivePortalsLoaded);
        }
        return isImmersivePortalsLoaded;
    }

    /** Creates one bi-way, bi-faced cluster shared by both linked controllers. */
    public static boolean createImmersivePortal(LevelAccessor world, double x, double y, double z,
            String rotation, int portalWidth, int portalHeight, int minExtent, int maxExtent,
            String targetDim, double targetX, double targetY, double targetZ) {
        if (!isImmersivePortalsLoaded() || !(world instanceof ServerLevel level)) return false;
        PortalFrame source = frame(level, BlockPos.containing(x, y, z), true);
        ResourceLocation dim = ResourceLocation.tryParse(targetDim);
        ServerLevel targetLevel = dim == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        // Controller chunks can load before their saved portal entities. Wait rather than duplicate them.
        if (!level.areEntitiesLoaded(new ChunkPos(BlockPos.containing(x, y, z)).toLong())
                || targetLevel != null && !targetLevel.areEntitiesLoaded(new ChunkPos(BlockPos.containing(targetX, targetY, targetZ)).toLong())) return false;
        PortalFrame target = targetLevel == null ? null : frame(targetLevel, BlockPos.containing(targetX, targetY, targetZ), true);
        if (source == null || target == null || source.controller == target.controller
                || !linksTo(source, target) || !linksTo(target, source)) {
            removeImmersivePortal(level, x, y, z);
            return false;
        }

        String id = source.controller.getPersistentData().getString(CLUSTER_ID);
        if (id.isEmpty()) id = target.controller.getPersistentData().getString(CLUSTER_ID);
        String newId = UUID.randomUUID().toString();
        try {
            if (!id.isEmpty() && validCluster(level, id, source, target)) {
                markCreated(source.controller, id);
                markCreated(target.controller, id);
                return true;
            }
            if (!removeImmersivePortal(level, x, y, z)
                    || !removeImmersivePortal(targetLevel, targetX, targetY, targetZ)) return false;

            String rootTag = "ctp_root_" + newId;
            Vec3 center = source.center;
            double scale = target.size / (double) source.size;
            // RotationArgument reads yaw then pitch; every frame must remain upright.
            String command = String.format(Locale.ROOT,
                "portal euler make_portal %.3f %.3f %.3f %.1f 0 %d %d %.16f {Tags:[\"%s\"],portalTag:\"%s%s\",NoGravity:1b}",
                center.x, center.y, center.z, getPortalNormal(source.rotation).toYRot(),
                source.size, source.size, scale, rootTag, OWNER_PREFIX, newId);
            level.getServer().getCommands().performPrefixedCommand(commandSource(level, center), command);
            Entity portal = level.getEntities((Entity) null, new AABB(center, center).inflate(0.05),
                e -> isPortal(e) && e.getTags().contains(rootTag)).stream().findFirst().orElseThrow();

            executeAsPortal(portal, String.format(Locale.ROOT, "portal set_portal_destination %s %.3f %.3f %.3f",
                targetLevel.dimension().location(), target.center.x, target.center.y, target.center.z));
            if (!targetLevel.dimension().equals(portal.getClass().getMethod("getDestDim").invoke(portal))
                    || vector(portal, "getDestPos").distanceToSqr(target.center) > 1e-6) {
                throw new IllegalStateException("Immersive portal destination was not applied");
            }
            orientOtherSide(portal, target.rotation);
            executeAsPortal(portal, "portal complete_bi_way_bi_faced_portal");
            if (!validCluster(level, newId, source, target)) {
                throw new IllegalStateException("Immersive portal cluster is incomplete or misaligned");
            }
            markCreated(source.controller, newId);
            markCreated(target.controller, newId);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            CreateteleportersMod.LOGGER.error("Failed to create Immersive Portals cluster", failure);
            try { for (Entity portal : ownedPortals(level, newId)) portal.discard(); }
            catch (ReflectiveOperationException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            clearCreated(source.controller);
            clearCreated(target.controller);
            return false;
        }
    }

    /** Called periodically, after the existing portal chunk tickets have loaded both endpoints. */
    public static boolean hasImmersivePortal(LevelAccessor world, double x, double y, double z) {
        if (!isImmersivePortalsLoaded() || !(world instanceof ServerLevel level)) return false;
        PortalFrame source = frame(level, BlockPos.containing(x, y, z), true);
        if (source == null) return false;
        PortalFrame target = linkedFrame(source.controller, true);
        String id = source.controller.getPersistentData().getString(CLUSTER_ID);
        if (target == null || id.isEmpty() || !linksTo(target, source)
                || !id.equals(target.controller.getPersistentData().getString(CLUSTER_ID))) return false;
        if (!source.level.areEntitiesLoaded(new ChunkPos(source.base).toLong())
                || !target.level.areEntitiesLoaded(new ChunkPos(target.base).toLong())) return true;
        try { return validCluster(level, id, source, target); }
        catch (ReflectiveOperationException | LinkageError failure) {
            CreateteleportersMod.LOGGER.warn("Could not verify Immersive Portals cluster", failure);
            return false;
        }
    }

    public static boolean removeImmersivePortal(LevelAccessor world, double x, double y, double z) {
        if (!isImmersivePortalsLoaded() || !(world instanceof ServerLevel level)) return false;
        BlockEntity controller = level.getBlockEntity(BlockPos.containing(x, y, z));
        if (controller == null) return true;
        String id = controller.getPersistentData().getString(CLUSTER_ID);
        PortalFrame target = linkedFrame(controller, false);
        if (!level.areEntitiesLoaded(new ChunkPos(controller.getBlockPos()).toLong())
                || target != null && !target.level.areEntitiesLoaded(new ChunkPos(target.base).toLong())) return false;
        try {
            if (!id.isEmpty()) {
                for (Entity portal : ownedPortals(level, id)) portal.discard();
            } else if (controller.getPersistentData().getBoolean("immersivePortalCreated")) {
                removeLegacy(frame(level, controller.getBlockPos(), false));
                if (target != null && target.controller.getPersistentData().getString(CLUSTER_ID).isEmpty()) removeLegacy(target);
            }
            clearCreated(controller);
            if (target != null && id.equals(target.controller.getPersistentData().getString(CLUSTER_ID))) clearCreated(target.controller);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            CreateteleportersMod.LOGGER.error("Failed to remove Immersive Portals cluster", failure);
            return false;
        }
    }

    public static void teleportEntity(Entity entity, ServerLevel targetLevel,
            double targetX, double targetY, double targetZ, float yaw) {
        entity.teleportTo(targetLevel, targetX, targetY, targetZ, java.util.Set.of(), yaw, entity.getXRot());
    }

    private static void orientOtherSide(Entity portal, String targetRotation) throws ReflectiveOperationException {
        Class<?> quaternion = Class.forName("qouteall.q_misc_util.my_util.DQuaternion");
        Class<?> manipulation = Class.forName("qouteall.imm_ptl.core.portal.PortalManipulation");
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 width = up.cross(Vec3.atLowerCornerOf(getPortalNormal(targetRotation).getNormal()));
        Object orientation = manipulation.getMethod("getPortalOrientationQuaternion", Vec3.class, Vec3.class)
            .invoke(null, width, up);
        portal.getClass().getMethod("setOtherSideOrientation", quaternion).invoke(portal, orientation);
        portal.getClass().getMethod("reloadAndSyncToClient").invoke(portal);
    }

    private static boolean validCluster(ServerLevel level, String id, PortalFrame source, PortalFrame target)
            throws ReflectiveOperationException {
        List<Entity> portals = ownedPortals(level, id);
        if (portals.size() != 4) return false;
        int sourceCount = 0;
        for (Entity portal : portals) {
            boolean atSource = portal.level() == source.level && portal.position().distanceToSqr(source.center) < 1e-6;
            PortalFrame from = atSource ? source : target;
            PortalFrame to = atSource ? target : source;
            if (atSource) sourceCount++;
            if (portal.level() != from.level || portal.position().distanceToSqr(from.center) > 1e-6
                    || !to.level.dimension().equals(portal.getClass().getMethod("getDestDim").invoke(portal))
                    || vector(portal, "getDestPos").distanceToSqr(to.center) > 1e-6
                    || vector(portal, "getAxisH").distanceToSqr(new Vec3(0, 1, 0)) > 1e-6
                    || Math.abs((Double) portal.getClass().getMethod("getWidth").invoke(portal) - from.size) > 1e-6
                    || Math.abs((Double) portal.getClass().getMethod("getHeight").invoke(portal) - from.size) > 1e-6
                    || ((Vec3) portal.getClass().getMethod("transformLocalVec", Vec3.class)
                        .invoke(portal, new Vec3(0, 1, 0))).distanceToSqr(new Vec3(0, to.size / (double) from.size, 0)) > 1e-6
                    || Math.abs(vector(portal, "getNormal").dot(Vec3.atLowerCornerOf(getPortalNormal(from.rotation).getNormal()))) < 0.999
                    || !(Boolean) portal.getClass().getMethod("isPortalValid").invoke(portal)) return false;
        }
        return sourceCount == 2;
    }

    private static List<Entity> ownedPortals(ServerLevel level, String id) throws ReflectiveOperationException {
        // ponytail: scan loaded entities once per second; index clusters if portal counts become large.
        List<Entity> portals = new ArrayList<>();
        for (ServerLevel dimension : level.getServer().getAllLevels()) for (Entity entity : dimension.getAllEntities()) {
            if (!entity.isRemoved() && isPortal(entity) && (OWNER_PREFIX + id).equals(entity.getClass().getField("portalTag").get(entity))) portals.add(entity);
        }
        return portals;
    }

    private static void removeLegacy(PortalFrame frame) throws ReflectiveOperationException {
        if (frame == null) return;
        // Old saves have no ownership tag. Restrict migration to the exact stored frame center.
        List<Entity> portals = frame.level.getEntities((Entity) null, new AABB(frame.center, frame.center).inflate(0.05),
            e -> isPortal(e) && e.position().distanceToSqr(frame.center) < 1e-6);
        for (Entity portal : portals) {
            Object tag = portal.getClass().getField("portalTag").get(portal);
            if (!portal.isRemoved() && (tag == null || !tag.toString().startsWith(OWNER_PREFIX))) {
                executeAsPortal(portal, "portal eradicate_portal_cluster");
                if (!portal.isRemoved()) throw new IllegalStateException("Legacy portal removal failed");
            }
        }
    }

    private static Vec3 vector(Entity portal, String method) throws ReflectiveOperationException {
        return (Vec3) portal.getClass().getMethod(method).invoke(portal);
    }

    private static boolean isPortal(Entity entity) {
        return "immersive_portals:portal".equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    private static CommandSourceStack commandSource(ServerLevel level, Vec3 position) {
        return new CommandSourceStack(CommandSource.NULL, position, net.minecraft.world.phys.Vec2.ZERO,
            level, 4, "", Component.literal(""), level.getServer(), null).withSuppressedOutput();
    }

    private static void executeAsPortal(Entity portal, String command) {
        ServerLevel level = (ServerLevel) portal.level();
        level.getServer().getCommands().performPrefixedCommand(commandSource(level, portal.position()).withEntity(portal), command);
    }

    private static PortalFrame linkedFrame(BlockEntity controller, boolean active) {
        if (!(controller.getLevel() instanceof ServerLevel level)) return null;
        CompoundTag tag = controller.getPersistentData();
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("linkedDim"));
        ServerLevel target = dim == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        return target == null || !tag.getBoolean("isLinked") ? null : frame(target,
            BlockPos.containing(tag.getDouble("linkedX"), tag.getDouble("linkedY"), tag.getDouble("linkedZ")), active);
    }

    private static boolean linksTo(PortalFrame from, PortalFrame to) {
        CompoundTag tag = from.controller.getPersistentData();
        return tag.getBoolean("isLinked") && tag.getString("linkedDim").equals(to.level.dimension().location().toString())
            && BlockPos.containing(tag.getDouble("linkedX"), tag.getDouble("linkedY"), tag.getDouble("linkedZ")).equals(to.base);
    }

    private static PortalFrame frame(ServerLevel level, BlockPos base, boolean active) {
        if (!level.getBlockState(base).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) return null;
        BlockEntity controller = level.getBlockEntity(base);
        if (controller == null) return null;
        CompoundTag tag = controller.getPersistentData();
        String rotation = tag.getString("rotation");
        if (!List.of("north", "south", "east", "west").contains(rotation)
                || !tag.contains("portalMinExtent") || !tag.contains("portalMaxExtent") || !tag.contains("portalHeight")) return null;
        int min = tag.getInt("portalMinExtent"), max = tag.getInt("portalMaxExtent"), height = tag.getInt("portalHeight");
        if (min > -2 || max < 2 || height < 4 || height > 22 || max - min != height) return null;
        if (active && (!tag.getBoolean("portalActive") || CustomPortalTeleportMode.isCoordinateMode(level, base))) return null;
        double horizontal = (min + max + 1) / 2.0;
        double y = base.getY() + 1 + (height - 1) / 2.0;
        Vec3 center = rotation.equals("north") || rotation.equals("south")
            ? new Vec3(base.getX() + horizontal, y, base.getZ() + 0.5)
            : new Vec3(base.getX() + 0.5, y, base.getZ() + horizontal);
        return new PortalFrame(level, base, center, height - 1, rotation, controller);
    }

    private static Direction getPortalNormal(String rotation) {
        return Direction.byName(rotation).getOpposite();
    }

    private static void markCreated(BlockEntity controller, String id) {
        CompoundTag tag = controller.getPersistentData();
        tag.putString(CLUSTER_ID, id);
        tag.putBoolean("immersivePortalCreated", true);
        tag.putInt("immersivePortalCompatVersion", COMPAT_VERSION);
        tag.putBoolean("portalVisualActive", true);
        sync(controller);
    }

    private static void clearCreated(BlockEntity controller) {
        controller.getPersistentData().remove(CLUSTER_ID);
        controller.getPersistentData().putBoolean("immersivePortalCreated", false);
        controller.getPersistentData().putBoolean("portalVisualActive", false);
        sync(controller);
    }

    private static void sync(BlockEntity controller) {
        controller.setChanged();
        if (controller.getLevel() instanceof ServerLevel level) level.sendBlockUpdated(controller.getBlockPos(), controller.getBlockState(), controller.getBlockState(), 3);
    }

    private record PortalFrame(ServerLevel level, BlockPos base, Vec3 center, int size, String rotation, BlockEntity controller) {}
}
