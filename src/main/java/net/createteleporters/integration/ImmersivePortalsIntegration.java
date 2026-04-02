package net.createteleporters.integration;

import net.createteleporters.CreateteleportersMod;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
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
        
        if (interiorWidth < 1 || interiorHeight < 1) {
            CreateteleportersMod.LOGGER.warn("Portal too small: {}x{}", interiorWidth, interiorHeight);
            return false;
        }
        
        // Calculate portal position
        int baseX = (int) Math.floor(x);
        int baseY = (int) y;
        int baseZ = (int) Math.floor(z);
        int extentCenter = (minExtent + maxExtent) / 2;
        int portalBottomY = baseY + 1;
        int portalCenterY = portalBottomY + interiorHeight / 2;
        
        double portalX, portalY, portalZ;
        Direction portalNormal = getPortalNormal(rotation);
        
        if ("north".equals(rotation) || "south".equals(rotation)) {
            portalX = baseX + extentCenter + 0.5;
            portalY = portalCenterY + 0.5;
            portalZ = baseZ + 0.5;
        } else {
            portalX = baseX + 0.5;
            portalY = portalCenterY + 0.5;
            portalZ = baseZ + extentCenter + 0.5;
        }
        
        try {
            CreateteleportersMod.LOGGER.info("=== CREATING IP PORTAL ===");
            CreateteleportersMod.LOGGER.info("Base: ({},{},{}) | Portal: ({},{},{}) | Size: {}x{}", 
                x, y, z, portalX, portalY, portalZ, interiorWidth, interiorHeight);
            CreateteleportersMod.LOGGER.info("Target: {} ({},{},{})", targetDim, targetX, targetY, targetZ);
            CreateteleportersMod.LOGGER.info("Rotation: {}, Normal: {} (yaw={})", rotation, portalNormal, portalNormal.toYRot());
            
            // Executor stands 1 block behind portal, facing the portal normal
            // make_portal creates portal 1 block in front of executor, facing executor
            double execX = portalX - portalNormal.getStepX();
            double execY = portalY;
            double execZ = portalZ - portalNormal.getStepZ();
            
            CommandSourceStack cmdSource = getCommandSource(serverLevel, execX, execY, execZ, portalNormal, x, y, z);
            
            // Create portal - destination is the OTHER portal's base position
            String createCmd = String.format("portal make_portal %d %d %s %d %d %d",
                interiorWidth, interiorHeight, targetDim,
                (int) Math.floor(targetX), (int) Math.floor(targetY), (int) Math.floor(targetZ));
            
            serverLevel.getServer().getCommands().performPrefixedCommand(cmdSource.withSuppressedOutput(), createCmd);
            CreateteleportersMod.LOGGER.info("Executed: {}", createCmd);
            
            // Set destination explicitly
            String destCmd = String.format("portal set_portal_destination %s %d %d %d",
                targetDim, (int) Math.floor(targetX), (int) Math.floor(targetY), (int) Math.floor(targetZ));
            executeAtPosition(serverLevel, portalX, portalY, portalZ, destCmd, true);
            
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
            executeAtPosition(serverLevel, x + 0.5, y + 1.5, z + 0.5, "portal delete_portal", true);
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
        // The direction you walk THROUGH the portal
        return switch (rotation) {
            case "north" -> Direction.NORTH;  // Walk north through it
            case "south" -> Direction.SOUTH;  // Walk south through it
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }
    
    private static CommandSourceStack getCommandSource(ServerLevel level, double px, double py, double pz, 
            Direction facing, double refX, double refY, double refZ) {
        net.minecraft.world.entity.player.Player playerEntity = level.getNearestPlayer(refX, refY, refZ, 64, false);
        
        if (playerEntity instanceof net.minecraft.server.level.ServerPlayer player) {
            return player.createCommandSourceStack()
                .withPosition(new Vec3(px, py, pz))
                .withRotation(new net.minecraft.world.phys.Vec2(0, facing.toYRot()));
        }
        
        return new CommandSourceStack(CommandSource.NULL, new Vec3(px, py, pz),
            new net.minecraft.world.phys.Vec2(0, facing.toYRot()),
            level, 4, "", Component.literal(""), level.getServer(), null);
    }
    
    private static void executeAtPosition(ServerLevel level, double x, double y, double z, 
            String command, boolean suppress) {
        net.minecraft.world.entity.player.Player playerEntity = level.getNearestPlayer(x, y, z, 64, false);
        
        CommandSourceStack cmdSource;
        if (playerEntity instanceof net.minecraft.server.level.ServerPlayer player) {
            cmdSource = player.createCommandSourceStack()
                .withPosition(new Vec3(x, y, z))
                .withRotation(new net.minecraft.world.phys.Vec2(0, 0));
        } else {
            cmdSource = new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z),
                new net.minecraft.world.phys.Vec2(0, 0), level, 4, "", Component.literal(""),
                level.getServer(), null);
        }
        
        if (suppress) {
            cmdSource = cmdSource.withSuppressedOutput();
        }
        
        level.getServer().getCommands().performPrefixedCommand(cmdSource, command);
    }
}
