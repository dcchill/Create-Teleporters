package net.createteleporters.integration.train;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.content.trains.track.TrackPropagator;
import net.createteleporters.init.CreateteleportersModBlocks;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("createteleporters")
@PrefixGameTestTemplate(false)
public final class TrainPortalGameTests {
	@GameTest(template = "train_test")
	public static void portalGraphAndPersistence(GameTestHelper helper) {
		Fixture f = fixture();
		check(f.portal.isInterDimensional() && f.portal.getLength() == 0, "Quantum connection must be a zero-length portal edge");
		check(!f.before.isInterDimensional() && f.before.getLength() == 20, "Local approach rail must remain ordinary rail");
		check(f.before.canTravelTo(f.portal) && f.portal.canTravelTo(f.after), "Perpendicular exit must be traversable");
		TravellingPoint probe = point(f.before, 19);
		double travelled = probe.travel(f.graph, 4, probe.follow(point(f.after, 3)));
		check(Math.abs(travelled - 4) < 1e-6 && probe.edge == f.after && Math.abs(probe.position - 3) < 1e-6, "Wheels must cross the portal without traversing world-space distance");
		probe.travel(f.graph, -4, probe.follow(point(f.before, 19)));
		check(probe.edge == f.before && Math.abs(probe.position - 19) < 1e-6, "Reverse passage must return to the approach rail");
		DimensionPalette palette = new DimensionPalette();
		TrackNodeLocation source = f.portal.node1.getLocation();
		TrackNodeLocation target = f.portal.node2.getLocation();
		TrackNodeLocation restoredSource = TrackNodeLocation.read(source.write(palette), palette);
		TrackNodeLocation restoredTarget = TrackNodeLocation.read(target.write(palette), palette);
		check(PortalNode.connects(restoredSource, restoredTarget), "Portal identity must survive graph NBT");
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			source.send(buffer, palette); target.send(buffer, palette);
			check(PortalNode.connects(TrackNodeLocation.receive(buffer, palette), TrackNodeLocation.receive(buffer, palette)), "Portal identity must reach clients");
			check(buffer.readableBytes() == 0, "Node packet must be consumed exactly");
		} finally { buffer.release(); }
		check(PortalSide.pivot(PortalSide.key(target)).equals(target), "Endpoint key must round trip");
		check(PortalSide.dimension(PortalSide.key(target)).equals(Level.OVERWORLD), "Endpoint identity must resolve to the real world");
		helper.succeed();
	}

	@GameTest(template = "train_test")
	public static void splitCarriageAnchorsAndReload(GameTestHelper helper) {
		Fixture f = fixture();
		CarriageBogey front = new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, new CompoundTag(), point(f.after, 3), point(f.after, 1));
		CarriageBogey rear = new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, new CompoundTag(), point(f.before, 15), point(f.before, 13));
		Carriage carriage = new Carriage(front, rear, 8);
		new Train(UUID.randomUUID(), UUID.randomUUID(), f.graph, List.of(carriage), List.of(), false, 0);
		carriage.updateContraptionAnchors();
		var portions = ((PortalCarriage) carriage).ctp$entities();
		check(portions.size() == 2, "A same-world carriage needs two independent portions");
		for (var dce : portions.values()) {
			check(dce.pivot != null, "Both portions need a portal plane");
			check(Math.abs(dce.rotationAnchors.getFirst().distanceTo(dce.rotationAnchors.getSecond()) - 8) < 1e-4, "Carriage must keep its bogey spacing at each end");
			check(Double.isFinite(dce.positionAnchor.x) && Double.isFinite(dce.positionAnchor.z), "Anchors must be finite");
		}
		check(carriage.getPresentDimensions().equals(List.of(Level.OVERWORLD)), "Internal endpoint keys must not leak into world dimensions");
		float[] cutoffs = new float[2];
		for (var dce : portions.values()) cutoffs[dce.cutoff > 0 ? 0 : 1] = dce.cutoff;
		check(cutoffs[0] > 0 && cutoffs[1] < 0, "Entrance and exit must have complementary clipping");
		carriage.updateContraptionAnchors();
		check(portions.values().stream().anyMatch(d -> d.cutoff == cutoffs[0]), "Stopping must leave clipping unchanged");
		DimensionPalette palette = new DimensionPalette();
		carriage.storage.read(new CompoundTag(), helper.getLevel().registryAccess(), false, null);
		Carriage restored = Carriage.read(carriage.write(palette, helper.getLevel().registryAccess()), helper.getLevel().registryAccess(), f.graph, palette);
		new Train(UUID.randomUUID(), UUID.randomUUID(), f.graph, List.of(restored), List.of(), false, 0);
		restored.updateContraptionAnchors();
		check(((PortalCarriage) restored).ctp$entities().size() == 2, "Reload must retain both portions");
		helper.succeed();
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

	@GameTest(template = "train_test")
	public static void actualPortalTracksBuildOneGraph(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos source = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockPos target = helper.absolutePos(new BlockPos(12, 1, 12));
		var rail = AllBlocks.TRACK.getDefaultState();
		level.setBlock(source.east(), CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState(), 3);
		level.setBlock(target.north(), CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get().defaultBlockState(), 3);
		for (int i = 1; i <= 8; i++) {
			level.setBlock(source.west(i), rail.setValue(TrackBlock.SHAPE, TrackShape.XO), 3);
			level.setBlock(target.south(i), rail.setValue(TrackBlock.SHAPE, TrackShape.ZO), 3);
		}
		level.setBlock(source, rail.setValue(TrackBlock.SHAPE, TrackShape.TE).setValue(TrackBlock.HAS_BE, true), 3);
		level.setBlock(target, rail.setValue(TrackBlock.SHAPE, TrackShape.TN).setValue(TrackBlock.HAS_BE, true), 3);
		((TrackBlockEntity) level.getBlockEntity(source)).bind(level.dimension(), target);
		((TrackBlockEntity) level.getBlockEntity(target)).bind(level.dimension(), source);
		try {
			TrackGraph graph = TrackPropagator.onRailAdded(level, source, level.getBlockState(source));
			check(graph != null, "Portal tracks must produce a graph");
			TrackNode a = graph.locateNode(QuantumTrainPortals.endpoint(level, source, level.getBlockState(source)));
			TrackNode b = graph.locateNode(QuantumTrainPortals.endpoint(level, target, level.getBlockState(target)));
			check(a != null && b != null, "Both portal planes must be explicit graph nodes");
			TrackEdge connection = graph.getConnectionsFrom(a).get(b);
			check(connection != null && connection.getLength() == 0 && connection.isInterDimensional(), "Create's real propagator must build a portal edge, not a long physical rail");
			check(graph.getConnectionsFrom(a).values().stream().anyMatch(edge -> !edge.isInterDimensional()), "Entrance must retain its local approach rail");
			check(graph.getConnectionsFrom(b).values().stream().anyMatch(edge -> !edge.isInterDimensional()), "Exit must retain its local departure rail");
		} finally {
			level.destroyBlock(source, false); level.destroyBlock(target, false);
			level.removeBlock(source.east(), false); level.removeBlock(target.north(), false);
			for (int i = 1; i <= 8; i++) { level.destroyBlock(source.west(i), false); level.destroyBlock(target.south(i), false); }
		}
		helper.succeed();
	}

	@GameTest(template = "train_test")
	public static void singleBogeyStopsAndReverses(GameTestHelper helper) {
		Fixture f = fixture();
		CarriageBogey bogey = new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, new CompoundTag(), point(f.after, 1), point(f.before, 19));
		Carriage carriage = new Carriage(bogey, null, 0);
		new Train(UUID.randomUUID(), UUID.randomUUID(), f.graph, List.of(carriage), List.of(), false, 0);
		carriage.updateContraptionAnchors();
		check(((PortalCarriage) carriage).ctp$entities().size() == 2, "A single bogey can straddle the portal");
		check(bogey.getDimension() == null && bogey.getStress() == 0, "A split bogey must not stretch between distant wheels");
		PortalCarriageState.of(carriage).recoverShutdown(helper.getLevel());
		check(!PortalCarriageState.of(carriage).active(), "Portal shutdown must collapse a split carriage");
		check(((PortalCarriage) carriage).ctp$entities().size() == 1, "Recovery must leave one accessible carriage portion");
		check(carriage.getPresentDimensions().equals(List.of(Level.OVERWORLD)), "Recovery must restore the real world dimension");
		for (int i = 0; i < 3; i++) carriage.updateContraptionAnchors();
		for (var dce : ((PortalCarriage) carriage).ctp$entities().values()) check(Double.isFinite(dce.positionAnchor.x), "A wheel at the portal plane must have a finite anchor");
		bogey.leading().travel(f.graph, -3, bogey.leading().follow(point(f.before, 18)));
		bogey.trailing().travel(f.graph, -3, bogey.trailing().follow(point(f.before, 16)));
		carriage.updateContraptionAnchors();
		check(!PortalCarriageState.of(carriage).active(), "Reversing fully out must finish the transition");
		check(carriage.getDimensional(Level.OVERWORLD).cutoff == 0, "The surviving carriage must be fully visible");
		helper.succeed();
	}

	@GameTest(template = "train_test")
	public static void passengersAndCargoHaveOneOwner(GameTestHelper helper) {
		Fixture f = fixture(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)));
		BlockPos sourceChunk = BlockPos.containing(f.before.getPosition(f.graph, 0.95));
		BlockPos targetChunk = BlockPos.containing(f.after.getPosition(f.graph, 0.1));
		forceEndpointChunks(helper, sourceChunk, true);
		forceEndpointChunks(helper, targetChunk, true);
		helper.runAfterDelay(40, () -> {
			try { verifyPassengersAndCargo(helper, f); helper.succeed(); }
			catch (Exception failure) { throw new RuntimeException(failure); }
			finally {
				forceEndpointChunks(helper, sourceChunk, false);
				forceEndpointChunks(helper, targetChunk, false);
			}
		});
	}
	private static void forceEndpointChunks(GameTestHelper helper, BlockPos endpoint, boolean forced) {
		// Carriage anchors can extend into neighboring chunks as the wheels cross.
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
			helper.getLevel().setChunkForced((endpoint.getX() >> 4) + x, (endpoint.getZ() >> 4) + z, forced);
	}
	private static void verifyPassengersAndCargo(GameTestHelper helper, Fixture f) throws Exception {
		var level = helper.getLevel();
		CarriageBogey front = new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, new CompoundTag(), point(f.before, 19), point(f.before, 17));
		CarriageBogey rear = new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, new CompoundTag(), point(f.before, 11), point(f.before, 9));
		Carriage carriage = new Carriage(front, rear, 8);
		Train train = new Train(UUID.randomUUID(), UUID.randomUUID(), f.graph, List.of(carriage), List.of(), false, 0);
		BlockPos chestPos = helper.absolutePos(BlockPos.ZERO);
		helper.setBlock(BlockPos.ZERO, Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
		CarriageContraption contraption = new TestContraption(level, chestPos, chest);
		carriage.setContraption(level, contraption);
		level.removeBlock(chestPos, false);
		var create = Carriage.DimensionalCarriageEntity.class.getDeclaredMethod("createEntity", Level.class, boolean.class);
		create.setAccessible(true);
		var source = carriage.getDimensional(level);
		create.invoke(source, level, true);
		var rider = EntityType.CHICKEN.create(level);
		UUID riderId = rider.getUUID();
		rider.setPos(source.positionAnchor);
		level.addFreshEntity(rider);
		source.entity.get().addSittingPassenger(rider, 0);
		try {
			for (TravellingPoint wheel : new TravellingPoint[] { front.leading(), front.trailing(), rear.leading(), rear.trailing() }) wheel.travel(f.graph, 4, wheel.follow(point(f.after, 10)));
			carriage.updateContraptionAnchors();
			var portions = ((PortalCarriage) carriage).ctp$entities();
			check(portions.size() == 2, "Crossing must produce exactly two portions");
			for (var dce : List.copyOf(portions.values())) if (dce.entity.get() == null) create.invoke(dce, level, false);
			for (var dce : portions.values()) check(level.getEntity(dce.entity.get().getUUID()) == dce.entity.get(), "Both portions must actually be registered in the same world");
			for (var dce : portions.values()) dce.updatePassengerLoadout();
			var exit = portions.get(PortalSide.key(f.portal.node2.getLocation()));
			check(exit.entity.get().getPassengers().size() == 1 && exit.entity.get().getPassengers().getFirst().getUUID().equals(riderId), "Passenger must move to the emerging portion exactly once");
			check(source.entity.get().getPassengers().isEmpty(), "Entrance must relinquish the passenger");
			for (var dce : portions.values()) check(dce.entity.get().getContraption().getStorage() == carriage.storage, "Both portions must use the one authoritative cargo manager");
			exit.entity.get().getContraption().getStorage().getAllItems().extractItem(0, 1, false);
			check(source.entity.get().getContraption().getStorage().getAllItems().getStackInSlot(0).getCount() == 6, "Cargo changes at the exit must be visible at the entrance");
			DimensionPalette palette = new DimensionPalette();
			Carriage loaded = Carriage.read(carriage.write(palette, level.registryAccess()), level.registryAccess(), f.graph, palette);
			check(loaded.storage.getAllItems().getStackInSlot(0).getCount() == 6, "Cargo must survive a mid-transition save");
			check(!train.invalid, "Entity creation must not invalidate the train");
			exit.entity.get().getPassengers().forEach(net.minecraft.world.entity.Entity::discard);
			var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "portal-driver-test");
			var driver = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile, net.minecraft.server.level.ClientInformation.createDefault());
			var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
			new io.netty.channel.embedded.EmbeddedChannel(connection);
			driver.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, driver,
				net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
				@Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
			};
			// Register lookup only: the in-game test connection has no mod payload handshake.
			var playerMapField = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
			playerMapField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var playerMap = (java.util.Map<UUID, net.minecraft.server.level.ServerPlayer>) playerMapField.get(level.getServer().getPlayerList());
			playerMap.put(driver.getUUID(), driver);
			var inputs = com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler.receivedInputs.get(level);
			try {
				for (boolean holdingKey : new boolean[] { true, false }) {
					driver.teleportTo(level, source.positionAnchor.x, source.positionAnchor.y, source.positionAnchor.z, 0, 0);
					source.entity.get().addSittingPassenger(driver, 0);
					source.entity.get().startControlling(BlockPos.ZERO, driver);
					source.entity.get().setControllingPlayer(driver.getUUID());
					if (holdingKey) com.simibubi.create.content.contraptions.actors.trainControls.ControlsServerHandler.receivePressed(
						level, source.entity.get(), BlockPos.ZERO, driver.getUUID(), List.of(0), true);
					else inputs.remove(driver.getUUID());
					var dismount = Carriage.DimensionalCarriageEntity.class.getDeclaredMethod("dismountPlayer", net.minecraft.server.level.ServerLevel.class,
						net.minecraft.server.level.ServerPlayer.class, Integer.class, boolean.class);
					dismount.setAccessible(true);
					dismount.invoke(source, level, driver, 0, true);
					check(!inputs.containsKey(driver.getUUID()), "The entrance must release the old input session");
					exit.updatePassengerLoadout();
					check(driver.getVehicle() == exit.entity.get(), "The driver must retain their seat at the exit");
					check(exit.entity.get().getControllingPlayer().filter(driver.getUUID()::equals).isPresent(), "Portal passage must retain the controlling player");
					var context = (net.createteleporters.mixin.PortalControlsContextAccessor) (Object) inputs.get(driver.getUUID());
					check(context != null && context.ctp$entity() == exit.entity.get(), "Inputs must target the emerging carriage");
					check(context.ctp$keys().size() == (holdingKey ? 1 : 0), "Held inputs must survive without inventing a throttle input");
					check(((PortalCarriageEntity) exit.entity.get()).ctp$getControls().orElseThrow().equals(BlockPos.ZERO), "Client control resumption must use the original local controls");
					driver.stopRiding();
					exit.entity.get().setControllingPlayer(null);
				}
			} finally {
				inputs.remove(driver.getUUID());
				driver.stopRiding();
				playerMap.remove(driver.getUUID());
				driver.discard();
			}
		} finally {
			carriage.forEachPresentEntity(entity -> { entity.getPassengers().forEach(net.minecraft.world.entity.Entity::discard); entity.discard(); });
			rider.discard();
		}
	}
	private static final class TestContraption extends CarriageContraption {
		TestContraption(Level level, BlockPos pos, ChestBlockEntity chest) {
			super(Direction.EAST);
			anchor = BlockPos.ZERO;
			bounds = new AABB(0, 0, 0, 9, 2, 1);
			blocks.put(BlockPos.ZERO, new StructureBlockInfo(BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), chest.saveWithFullMetadata(level.registryAccess())));
			blocks.put(new BlockPos(8, 0, 0), new StructureBlockInfo(new BlockPos(8, 0, 0), Blocks.STONE.defaultBlockState(), null));
			seats.add(new BlockPos(0, 1, 0));
			storage.addBlock(level, chest.getBlockState(), pos, BlockPos.ZERO, chest);
			storage.initialize();
		}
	}
	private static TravellingPoint point(TrackEdge edge, double position) { return new TravellingPoint(edge.node1, edge.node2, edge, position, false); }
	private record Fixture(TrackGraph graph, TrackEdge before, TrackEdge portal, TrackEdge after) { }
	private static Fixture fixture() {
		return fixture(Vec3.ZERO);
	}
	private static Fixture fixture(Vec3 origin) {
		TrackGraph graph = new TrackGraph();
		TrackNodeLocation a = new TrackNodeLocation(origin.add(-20, 0, 0)).in(Level.OVERWORLD);
		TrackNodeLocation b = new TrackNodeLocation(origin).in(Level.OVERWORLD);
		TrackNodeLocation c = new TrackNodeLocation(origin.add(100, 5, 100)).in(Level.OVERWORLD);
		TrackNodeLocation d = new TrackNodeLocation(origin.add(100, 5, 120)).in(Level.OVERWORLD);
		((PortalNode) b).ctp$setCounterpart(c); ((PortalNode) c).ctp$setCounterpart(b);
		int id = 1;
		for (TrackNodeLocation location : List.of(a, b, c, d)) graph.loadNode(location, id++, new Vec3(0, 1, 0));
		return new Fixture(graph, connect(graph, a, b), connect(graph, b, c), connect(graph, c, d));
	}
	private static TrackEdge connect(TrackGraph graph, TrackNodeLocation from, TrackNodeLocation to) {
		TrackNode a = graph.locateNode(from), b = graph.locateNode(to);
		TrackEdge edge = new TrackEdge(a, b, null, TrackMaterial.ANDESITE);
		graph.putConnection(a, b, edge);
		graph.putConnection(b, a, new TrackEdge(b, a, null, TrackMaterial.ANDESITE));
		return edge;
	}
}
