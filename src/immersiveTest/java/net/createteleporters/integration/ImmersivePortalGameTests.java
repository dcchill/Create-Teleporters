package net.createteleporters.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.createteleporters.block.CustomPortalBaseBlock;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.configuration.CTPConfigConfiguration;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.init.CreateteleportersModFluids;
import net.createteleporters.init.CreateteleportersModItems;
import net.createteleporters.procedures.CustomPortalBaseBlockDestroyedByPlayerProcedure;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;


@GameTestHolder("createteleporters")
public final class ImmersivePortalGameTests {
    private static final String ID = "immersivePortalClusterId";
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final BlockPos BASE = new BlockPos(-160, 100, -160);

    @GameTestGenerator
    public static List<TestFunction> tests() {
        if (!ImmersivePortalsIntegration.isImmersivePortalsLoaded()) return List.of();
        return List.of(new TestFunction("immersive", "createteleporters.immersiveportalregressions",
            "createteleporters:train_test", 1800, 0, true, ImmersivePortalGameTests::start));
    }

    @BeforeBatch(batch = "immersive")
    public static void enable(ServerLevel level) { CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.set(true); }
    @AfterBatch(batch = "immersive")
    public static void disable(ServerLevel level) { CTPConfigConfiguration.IMMERSIVE_PORTALS_COMPAT.set(false); }

    private static void start(GameTestHelper helper) {
        tickets(helper.getLevel(), BASE, true);
        tickets(helper.getLevel(), BASE.offset(24, 9, 24), true);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        tickets(nether, BASE.offset(24, 9, 24), true);
        helper.runAfterDelay(20, () -> rotationCase(helper, 0));
    }

    private static void rotationCase(GameTestHelper helper, int index) {
        if (index == 16) { lifecycle(helper); return; }
        Direction[] directions = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        ServerLevel level = helper.getLevel();
        ServerLevel targetLevel = index == 15 ? level.getServer().getLevel(Level.NETHER) : level;
        Frame source = frame(level, BASE, directions[index % 4], -2, 4);
        Frame target = frame(targetLevel, BASE.offset(24, 9, 24), directions[index / 4], -2, 2);
        link(source, target);
        
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        helper.runAfterDelay(20, () -> checked(helper, () -> {
            try {
                verify(source, target);
                remove(source);
                clear(source); clear(target);
                net.createteleporters.CreateteleportersMod.LOGGER.info("Immersive rotation case {} passed", index);
            } finally { Locale.setDefault(previous); }
            rotationCase(helper, index + 1);
        }));
    }

    private static void lifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Frame a = frame(level, BASE, Direction.NORTH, -2, 2);
        Frame b = frame(level, BASE.offset(24, 9, 24), Direction.EAST, -2, 2);
        Frame neighborA = frame(level, BASE.south(), Direction.NORTH, -2, 2);
        Frame neighborB = frame(level, b.base.east(), Direction.EAST, -2, 2);
        link(a, b); link(neighborA, neighborB);
        helper.runAfterDelay(20, () -> checked(helper, () -> {
            verify(a, b); verify(neighborA, neighborB);
            String neighborId = id(neighborA);
            String originalId = id(a);
            
            List<CompoundTag> saved = new ArrayList<>();
            for (Entity portal : owned(a)) { CompoundTag tag = new CompoundTag(); portal.save(tag); saved.add(tag); portal.discard(); }
            for (Frame frame : List.of(a, b)) {
                CompoundTag savedController = controller(frame).saveWithFullMetadata(level.registryAccess());
                controller(frame).loadWithComponents(savedController, level.registryAccess());
                check(id(frame).equals(originalId), "Controller ownership must survive NBT reload");
            }
            helper.runAfterDelay(2, () -> checked(helper, () -> {
                for (CompoundTag tag : saved) {
                    Entity restored = EntityType.loadEntityRecursive(tag, level, Function.identity());
                    check(restored != null && level.addFreshEntity(restored), "Portal entity NBT reload");
                }
                verify(a, b);
                
                for (Entity portal : owned(a)) portal.getClass().getField("portalTag").set(portal, null);
                for (Frame frame : List.of(a, b)) {
                    data(frame).remove(ID); data(frame).putInt("immersivePortalCompatVersion", 1);
                }
                helper.runAfterDelay(20, () -> checked(helper, () -> {
                    verify(a, b);
                    check(!id(a).equals(originalId), "Version-1 cluster must rebuild automatically");
                    check(portalsAt(a).size() == 2 && portalsAt(b).size() == 2, "Migration must remove all legacy entities");
                    check(id(neighborA).equals(neighborId), "Migration must preserve adjacent ownership");
                    verify(neighborA, neighborB);
                    
                    owned(a).getFirst().discard();
                    helper.runAfterDelay(25, () -> checked(helper, () -> {
                        verify(a, b); verify(neighborA, neighborB);
                        String activeId = id(a);
                        controller(a).getFluidTank().setFluid(FluidStack.EMPTY);
                        helper.runAfterDelay(5, () -> checked(helper, () -> {
                            check(ownedWithId(level, activeId).isEmpty() && !data(b).getBoolean("immersivePortalCreated"), "Deactivation removes the whole pair");
                            verify(neighborA, neighborB);
                            fuel(a);
                            helper.runAfterDelay(5, () -> checked(helper, () -> {
                                verify(a, b);
                                CustomPortalBaseBlockDestroyedByPlayerProcedure.execute(level, a.base.getX(), a.base.getY(), a.base.getZ(), level.getBlockState(a.base));
                                level.removeBlock(a.base, false);
                                check(!data(b).getBoolean("immersivePortalCreated") && portalsAt(b).isEmpty(), "Controller destruction removes reciprocal entities");
                                verify(neighborA, neighborB);
                                failureCleanup(helper, a, b, neighborA, neighborB);
                            }));
                        }));
                    }));
                }));
            }));
        }));
    }

    private static void failureCleanup(GameTestHelper helper, Frame oldA, Frame b, Frame neighborA, Frame neighborB) throws Exception {
        tickets(oldA.level, oldA.base, true); tickets(b.level, b.base, true);
        Frame a = frame(oldA.level, oldA.base, Direction.NORTH, -2, 2);
        link(a, b);
        
        data(b).putString("rotation", "invalid");
        check(!create(a, b) && portalsAt(a).isEmpty(), "Invalid destination must not spawn a portal");
        data(b).putString("rotation", "east");
        net.createteleporters.procedures.ScalablePortalCheckerProcedure.execute(a.level, a.base.getX(), a.base.getY(), a.base.getZ());
        var command = a.level.getServer().getCommands().getDispatcher().getRoot().getChild("portal");
        var completion = command.getChild("complete_bi_way_bi_faced_portal");
        
        for (String name : List.of("children", "literals")) {
            var field = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            ((java.util.Map<?, ?>) field.get(command)).remove(completion.getName());
        }
        try {
            check(!create(a, b), "Missing completion command must fail setup");
            check(portalsAt(a).isEmpty() && portalsAt(b).isEmpty(), "Failure must remove the partial owned cluster");
            check(!data(a).getBoolean("immersivePortalCreated") && !data(b).getBoolean("immersivePortalCreated"), "Failure must leave both flags unset");
            verify(neighborA, neighborB);
        } finally { command.addChild(completion); }
        helper.runAfterDelay(30, () -> checked(helper, () -> {
            verify(a, b); verify(neighborA, neighborB);
            remove(a); remove(neighborA);
            for (Frame frame : List.of(a, b, neighborA, neighborB)) clear(frame);
            tickets(a.level, BASE, false); tickets(a.level, BASE.offset(24, 9, 24), false);
            tickets(a.level.getServer().getLevel(Level.NETHER), BASE.offset(24, 9, 24), false);
            helper.succeed();
        }));
    }

    private static boolean create(Frame a, Frame b) {
        return ImmersivePortalsIntegration.createImmersivePortal(a.level, a.base.getX(), a.base.getY(), a.base.getZ(),
            a.rotation.getName(), a.max - a.min, a.max - a.min, a.min, a.max,
            b.level.dimension().location().toString(), b.base.getX(), b.base.getY(), b.base.getZ());
    }

    private static void verify(Frame source, Frame target) throws Exception {
        String id = id(source);
        check(!id.isEmpty() && id.equals(id(target)), "Pair must share ownership");
        check(owned(source).size() == 4, "Both controller ticks must produce exactly four entities");
        for (Frame from : List.of(source, target)) {
            Frame to = from == source ? target : source;
            check(data(from).getBoolean("immersivePortalCreated") && data(from).getInt("immersivePortalCompatVersion") == 2, "Both controllers marked version 2");
            List<Entity> portals = portalsAt(from);
            check(portals.size() == 2, "Exactly two faces at each center");
            check(vector(portals.get(0), "getNormal").add(vector(portals.get(1), "getNormal")).lengthSqr() < 1e-6, "Faces must have opposite normals");
            double scale = to.size() / (double) from.size();
            for (Entity portal : portals) {
                check(vector(portal, "getAxisH").distanceToSqr(UP) < 1e-6, "Height axis must remain upright");
                check(Math.abs(vector(portal, "getNormal").dot(Vec3.atLowerCornerOf(from.rotation.getNormal()))) > .999, "Portal plane must be vertical in frame");
                check(vector(portal, "getDestPos").distanceToSqr(to.center()) < 1e-6, "Exact destination center and elevation");
                check(portal.getClass().getMethod("getDestDim").invoke(portal).equals(to.level.dimension()), "Destination dimension");
                check(Math.abs((Double) portal.getClass().getMethod("getWidth").invoke(portal) - from.size()) < 1e-6
                    && Math.abs((Double) portal.getClass().getMethod("getHeight").invoke(portal) - from.size()) < 1e-6, "Preserve frame size");
                Vec3 transformedUp = transform(portal, "transformLocalVec", UP);
                check(transformedUp.distanceToSqr(UP.scale(scale)) < 1e-6, "Scaling must preserve upright destination");
                Vec3 floor = from.center().add(.25, -from.size() / 2.0, .125);
                Vec3 destination = transform(portal, "transformPoint", floor);
                check(Math.abs(destination.y - (to.base.getY() + 1)) < 1e-6, "Rendered floor transform must match destination floor");
                Entity reverse = portalsAt(to).stream().filter(e -> {
                    try { return transform(e, "transformPoint", destination).distanceToSqr(floor) < 1e-6; }
                    catch (Exception failure) { throw new RuntimeException(failure); }
                }).findFirst().orElseThrow(() -> new AssertionError("Reciprocal transform"));
                check(transform(reverse, "transformPoint", destination).distanceToSqr(floor) < 1e-6, "Round trip restores the source point");
            }
        }
    }

    private static Frame frame(ServerLevel level, BlockPos base, Direction rotation, int min, int max) {
        Direction horizontal = rotation.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        for (int x = min; x <= max; x++) for (int y = 0; y <= max - min; y++) {
            var block = y == 0 && Math.abs(x) <= 1 ? Blocks.AIR.defaultBlockState()
                : y == 0 || y == max - min || x == min || x == max ? CreateteleportersModBlocks.QUANTUM_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(base.relative(horizontal, x).above(y), block, 2);
        }
        level.setBlock(base, CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get().defaultBlockState().setValue(CustomPortalBaseBlock.FACING, rotation), 2);
        Frame frame = new Frame(level, base, rotation, min, max);
        data(frame).putString("rotation", rotation.getName());
        data(frame).putString(CustomPortalTeleportMode.TAG, CustomPortalTeleportMode.PORTAL_TO_PORTAL);
        controller(frame).setItem(0, new ItemStack(CreateteleportersModItems.ADV_TPLINK.get()));
        fuel(frame);
        return frame;
    }

    private static void link(Frame a, Frame b) {
        for (Frame frame : List.of(a, b)) {
            Frame to = frame == a ? b : a;
            CompoundTag tag = data(frame);
            tag.putBoolean("isLinked", true); tag.putString("linkedDim", to.level.dimension().location().toString());
            tag.putDouble("linkedX", to.base.getX()); tag.putDouble("linkedY", to.base.getY()); tag.putDouble("linkedZ", to.base.getZ());
        }
    }
    private static void fuel(Frame frame) { controller(frame).getFluidTank().setFluid(new FluidStack(CreateteleportersModFluids.QUANTUM_FLUID.get(), 32000)); }
    private static void remove(Frame frame) { ImmersivePortalsIntegration.removeImmersivePortal(frame.level, frame.base.getX(), frame.base.getY(), frame.base.getZ()); }
    private static void clear(Frame frame) {
        for (BlockPos pos : BlockPos.betweenClosed(frame.base.offset(-5, 0, -5), frame.base.offset(5, 7, 5))) frame.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }
    private static CustomPortalBaseBlockEntity controller(Frame frame) { return (CustomPortalBaseBlockEntity) frame.level.getBlockEntity(frame.base); }
    private static CompoundTag data(Frame frame) { return controller(frame).getPersistentData(); }
    private static String id(Frame frame) { return data(frame).getString(ID); }
    private static List<Entity> owned(Frame frame) throws Exception { return ownedWithId(frame.level, id(frame)); }
    private static List<Entity> ownedWithId(ServerLevel level, String id) throws Exception {
        List<Entity> portals = new ArrayList<>();
        for (ServerLevel dimension : level.getServer().getAllLevels()) for (Entity entity : dimension.getAllEntities()) {
            if (!entity.isRemoved() && entity.getClass().getName().equals("qouteall.imm_ptl.core.portal.Portal")
                && ("createteleporters:" + id).equals(entity.getClass().getField("portalTag").get(entity))) portals.add(entity);
        }
        return portals;
    }
    private static List<Entity> portalsAt(Frame frame) {
        List<Entity> portals = new ArrayList<>();
        for (Entity entity : frame.level.getAllEntities()) if (!entity.isRemoved()
            && entity.getClass().getName().equals("qouteall.imm_ptl.core.portal.Portal") && entity.position().distanceToSqr(frame.center()) < 1e-6) portals.add(entity);
        return portals;
    }
    private static Vec3 vector(Entity entity, String method) throws Exception { return (Vec3) entity.getClass().getMethod(method).invoke(entity); }
    private static Vec3 transform(Entity entity, String method, Vec3 value) throws Exception { return (Vec3) entity.getClass().getMethod(method, Vec3.class).invoke(entity, value); }
    private static void tickets(ServerLevel level, BlockPos base, boolean forced) {
        int x = base.getX() >> 4, z = base.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setChunkForced(x + dx, z + dz, forced);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void checked(GameTestHelper helper, Check check) {
        try { check.run(); }
        catch (Exception | AssertionError failure) {
            net.createteleporters.CreateteleportersMod.LOGGER.error("Immersive regression failed", failure);
            helper.fail(failure.toString());
        }
    }
    @FunctionalInterface private interface Check { void run() throws Exception; }
    private record Frame(ServerLevel level, BlockPos base, Direction rotation, int min, int max) {
        int size() { return max - min - 1; }
        Vec3 center() {
            double horizontal = (min + max + 1) / 2.0;
            return rotation.getAxis() == Direction.Axis.X
                ? new Vec3(base.getX() + .5, base.getY() + 1 + size() / 2.0, base.getZ() + horizontal)
                : new Vec3(base.getX() + horizontal, base.getY() + 1 + size() / 2.0, base.getZ() + .5);
        }
    }
}
