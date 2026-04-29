package net.createteleporters.integration;

import net.createteleporters.CreateteleportersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

public class ImmersivePortalsIntegration {
    
    private static Boolean isImmersivePortalsLoaded = null;
    
    public static boolean isImmersivePortalsLoaded() {
        if (isImmersivePortalsLoaded == null) {
            isImmersivePortalsLoaded = ModList.get().isLoaded("immersive_portals") ||
                                       ModList.get().isLoaded("immersive_portals_core") ||
                                       ModList.get().isLoaded("immersiveportals") ||
                                       ModList.get().isLoaded("immersive-portals");
            CreateteleportersMod.LOGGER.info("Immersive Portals loaded: {}", isImmersivePortalsLoaded);
        }
        return isImmersivePortalsLoaded;
    }
    
    /**
     * Creates an Immersive Portals portal.
     * Each portal base creates its own portal independently.
     */
    public static boolean createImmersivePortal(LevelAccessor world, double x, double y, double z,
            String rotation, int portalWidth, int portalHeight, int minExtent, int maxExtent,
            String targetDim, double targetX, double targetY, double targetZ) {
        
        if (!isImmersivePortalsLoaded()) {
            CreateteleportersMod.LOGGER.warn("Cannot create IP - mod not loaded");
            return false;
        }
        
        if (!(world instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        int interiorWidth = Math.abs(maxExtent - minExtent) - 1;
        int interiorHeight = portalHeight - 1;
        int sourceSquareSize = Math.max(1, Math.min(interiorWidth, interiorHeight));
        
        if (interiorWidth < 1 || interiorHeight < 1) {
            CreateteleportersMod.LOGGER.warn("Portal too small: {}x{}", interiorWidth, interiorHeight);
            return false;
        }
        
        // Calculate portal position
        int baseX = (int) Math.floor(x);
        int baseY = (int) y;
        int baseZ = (int) Math.floor(z);
        int interiorMin = minExtent + 1;
        int interiorMax = maxExtent - 1;
        double horizontalCenterOffset = (interiorMin + interiorMax + 1) / 2.0;
        double portalCenterY = baseY + 1 + interiorHeight / 2.0;

        double portalX, portalY, portalZ;
        Direction portalNormal = getPortalNormal(rotation);

        if ("north".equals(rotation) || "south".equals(rotation)) {
            portalX = baseX + horizontalCenterOffset;
            portalY = portalCenterY;
            portalZ = baseZ + 0.5;
        } else {
            portalX = baseX + 0.5;
            portalY = portalCenterY;
            portalZ = baseZ + horizontalCenterOffset;
        }
        
        try {
            CreateteleportersMod.LOGGER.info("=== CREATING IP PORTAL ===");
            CreateteleportersMod.LOGGER.info("Base: ({},{},{}) | Portal: ({},{},{}) | Frame size: {}x{} | Square size: {}", 
                x, y, z, portalX, portalY, portalZ, interiorWidth, interiorHeight, sourceSquareSize);
            CreateteleportersMod.LOGGER.info("Target: {} ({},{},{})", targetDim, targetX, targetY, targetZ);
            CreateteleportersMod.LOGGER.info("Rotation: {}, Normal: {} (yaw={})", rotation, portalNormal, portalNormal.toYRot());
            
            // make_portal creates the portal one block in front of the executor and the
            // created portal faces the executor. To make the portal face portalNormal,
            // the executor must stand on the opposite side.
            double execX = portalX + portalNormal.getStepX();
            double execY = portalY;
            double execZ = portalZ + portalNormal.getStepZ();
            
            CommandSourceStack cmdSource = getCommandSource(serverLevel, execX, execY, execZ, portalNormal, x, y, z);
            
            PortalTargetInfo targetInfo = resolveTargetPortalInfo(serverLevel, targetDim, targetX, targetY, targetZ,
                rotation, minExtent, maxExtent, portalHeight);
            double squareScale = targetInfo.squareSize / (double) sourceSquareSize;
            double portalScale = Math.max(0.1, squareScale);

            // Create portal with explicit Euler orientation to avoid block-hit based
            // make_portal rotation ambiguity (which can create flat portals).
            String createCmd = String.format(
                "portal euler make_portal %.3f %.3f %.3f %.1f %.1f %d %d %.4f {}",
                portalX, portalY, portalZ, 0.0f, portalNormal.toYRot(), sourceSquareSize, sourceSquareSize, portalScale
            );
            
            serverLevel.getServer().getCommands().performPrefixedCommand(cmdSource.withSuppressedOutput(), createCmd);
            CreateteleportersMod.LOGGER.info("Executed: {}", createCmd);

            // Set destination to the target portal center before making this portal
            // bi-way/bi-faced, so generated reverse portals are centered correctly.
            String destCmd = String.format("portal set_portal_destination %s %.3f %.3f %.3f",
                targetDim, targetInfo.center.x, targetInfo.center.y, targetInfo.center.z);
            executeAsNearestPortal(serverLevel, portalX, portalY, portalZ, destCmd, true);
            CreateteleportersMod.LOGGER.info("Executed: {}", destCmd);

            if (!orientNearestPortalOtherSide(serverLevel, portalX, portalY, portalZ, targetInfo.rotation)) {
                CreateteleportersMod.LOGGER.warn("Could not align IP portal other-side orientation to target rotation {}", targetInfo.rotation);
            }

            // Make the portal bi-way and bi-faced (4 connected portal entities total)
            // so both sides in both dimensions work automatically.
            String completeCmd = "portal complete_bi_way_bi_faced_portal";
            executeAsNearestPortal(serverLevel, portalX, portalY, portalZ, completeCmd, true);
            CreateteleportersMod.LOGGER.info("Executed: {}", completeCmd);

            // Keep all nearby IP portal entities static so the portal cluster is immovable.
            String lockPortalMotionCmd =
                "execute as @e[type=immersive_portals:portal,distance=..6] run data merge entity @s {NoGravity:1b,Motion:[0.0d,0.0d,0.0d]}";
            executeAtPosition(serverLevel, portalX, portalY, portalZ, lockPortalMotionCmd, true);
            CreateteleportersMod.LOGGER.info("Executed: {}", lockPortalMotionCmd);
            
            CreateteleportersMod.LOGGER.info("=== PORTAL CREATED ===");
            return true;
        } catch (Exception e) {
            CreateteleportersMod.LOGGER.error("Failed to create IP portal", e);
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean removeImmersivePortal(LevelAccessor world, double x, double y, double z) {
        if (!isImmersivePortalsLoaded() || !(world instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        try {
            executeAsNearestPortal(serverLevel, x + 0.5, y + 1.5, z + 0.5, "portal eradicate_portal_cluster", true);
            CreateteleportersMod.LOGGER.info("Removed IP portal");
            return true;
        } catch (Exception e) {
            CreateteleportersMod.LOGGER.error("Failed to remove IP portal", e);
            return false;
        }
    }
    
    public static void teleportEntity(Entity entity, ServerLevel targetLevel, 
            double targetX, double targetY, double targetZ, float yaw) {
        entity.teleportTo(targetLevel, targetX, targetY, targetZ, java.util.Set.of(), yaw, entity.getXRot());
    }
    
    private static Direction getPortalNormal(String rotation) {
        // Block "rotation" stores the frame-facing direction.
        // The portal normal for command placement needs the opposite direction.
        return switch (rotation) {
            case "north" -> Direction.SOUTH;
            case "south" -> Direction.NORTH;
            case "east" -> Direction.WEST;
            case "west" -> Direction.EAST;
            default -> Direction.NORTH;
        };
    }
    
    private static CommandSourceStack getCommandSource(ServerLevel level, double px, double py, double pz, 
            Direction facing, double refX, double refY, double refZ) {
        return new CommandSourceStack(CommandSource.NULL, new Vec3(px, py, pz),
            new net.minecraft.world.phys.Vec2(0, facing.toYRot()),
            level, 4, "", Component.literal(""), level.getServer(), null);
    }

    private static boolean orientNearestPortalOtherSide(ServerLevel level, double x, double y, double z, String targetRotation) {
        Entity portalEntity = level.getEntities((Entity) null, new AABB(x - 3.0, y - 3.0, z - 3.0, x + 3.0, y + 3.0, z + 3.0),
            entity -> "immersive_portals:portal".equals(entityTypeId(entity)))
            .stream()
            .min((a, b) -> Double.compare(a.distanceToSqr(x, y, z), b.distanceToSqr(x, y, z)))
            .orElse(null);

        if (portalEntity == null) {
            return false;
        }

        try {
            Class<?> dQuaternionClass = Class.forName("qouteall.q_misc_util.my_util.DQuaternion");
            Class<?> portalManipulationClass = Class.forName("qouteall.imm_ptl.core.portal.PortalManipulation");
            Direction targetNormal = getPortalNormal(targetRotation);
            Vec3 targetAxisH = new Vec3(0.0, 1.0, 0.0);
            Vec3 targetAxisW = axisWForNormal(targetNormal);
            Object targetOrientation = portalManipulationClass
                .getMethod("getPortalOrientationQuaternion", Vec3.class, Vec3.class)
                .invoke(null, targetAxisW, targetAxisH);

            portalEntity.getClass()
                .getMethod("setOtherSideOrientation", dQuaternionClass)
                .invoke(portalEntity, targetOrientation);
            portalEntity.getClass().getMethod("reloadAndSyncToClient").invoke(portalEntity);
            CreateteleportersMod.LOGGER.info("Aligned IP portal other side to target rotation {} (normal {})", targetRotation, targetNormal);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            CreateteleportersMod.LOGGER.warn("Failed to align IP portal orientation via reflection", e);
            return false;
        }
    }

    private static String entityTypeId(Entity entity) {
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null ? key.toString() : "";
    }

    private static Vec3 axisWForNormal(Direction normal) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 normalVec = Vec3.atLowerCornerOf(normal.getNormal());
        return up.cross(normalVec).normalize();
    }

    private static void executeAtPosition(ServerLevel level, double x, double y, double z, 
            String command, boolean suppress) {
        CommandSourceStack cmdSource = new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z),
            new net.minecraft.world.phys.Vec2(0, 0), level, 4, "", Component.literal(""),
            level.getServer(), null);
        
        if (suppress) {
            cmdSource = cmdSource.withSuppressedOutput();
        }
        
        level.getServer().getCommands().performPrefixedCommand(cmdSource, command);
    }

    private static void executeAsNearestPortal(ServerLevel level, double x, double y, double z,
            String portalCommand, boolean suppress) {
        String command = String.format(
            "execute positioned %.3f %.3f %.3f as @e[type=immersive_portals:portal,sort=nearest,limit=1,distance=..3] run %s",
            x, y, z, portalCommand
        );
        executeAtPosition(level, x, y, z, command, suppress);
    }

    private static PortalTargetInfo resolveTargetPortalInfo(ServerLevel sourceLevel, String targetDimId,
            double targetBaseX, double targetBaseY, double targetBaseZ,
            String fallbackRotation, int fallbackMinExtent, int fallbackMaxExtent, int fallbackPortalHeight) {
        String rotation = fallbackRotation;
        int minExtent = fallbackMinExtent;
        int maxExtent = fallbackMaxExtent;
        int portalHeight = fallbackPortalHeight;

        ResourceLocation dimLocation = ResourceLocation.tryParse(targetDimId);
        if (dimLocation != null) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
            ServerLevel targetLevel = sourceLevel.getServer().getLevel(dimKey);
            if (targetLevel != null) {
                BlockEntity targetBe = targetLevel.getBlockEntity(BlockPos.containing(targetBaseX, targetBaseY, targetBaseZ));
                if (targetBe != null) {
                    var nbt = targetBe.getPersistentData();
                    if (nbt.contains("rotation")) {
                        rotation = nbt.getString("rotation");
                    }
                    if (nbt.contains("portalMinExtent")) {
                        minExtent = nbt.getInt("portalMinExtent");
                    }
                    if (nbt.contains("portalMaxExtent")) {
                        maxExtent = nbt.getInt("portalMaxExtent");
                    }
                    if (nbt.contains("portalHeight")) {
                        portalHeight = nbt.getInt("portalHeight");
                    }
                }
            }
        }

        int baseX = (int) Math.floor(targetBaseX);
        int baseY = (int) Math.floor(targetBaseY);
        int baseZ = (int) Math.floor(targetBaseZ);
        int interiorHeight = Math.max(1, portalHeight - 1);
        int interiorWidth = Math.max(1, Math.abs(maxExtent - minExtent) - 1);
        int squareSize = Math.max(1, Math.min(interiorWidth, interiorHeight));
        int interiorMin = minExtent + 1;
        int interiorMax = maxExtent - 1;
        double horizontalCenterOffset = (interiorMin + interiorMax + 1) / 2.0;
        double portalCenterY = baseY + 1 + interiorHeight / 2.0;

        if ("north".equals(rotation) || "south".equals(rotation)) {
            return new PortalTargetInfo(new Vec3(baseX + horizontalCenterOffset, portalCenterY, baseZ + 0.5), squareSize, rotation);
        }
        return new PortalTargetInfo(new Vec3(baseX + 0.5, portalCenterY, baseZ + horizontalCenterOffset), squareSize, rotation);
    }

    private record PortalTargetInfo(Vec3 center, int squareSize, String rotation) {}
}
