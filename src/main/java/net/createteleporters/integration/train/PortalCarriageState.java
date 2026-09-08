package net.createteleporters.integration.train;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class PortalCarriageState {
	private final Carriage carriage;
	private final Map<TrackNode, ResourceKey<Level>> sides = new IdentityHashMap<>();
	private PortalPath.Crossing crossing;
	private Carriage.DimensionalCarriageEntity hiddenCoupling;
	public long lastClientTick = Long.MIN_VALUE;

	public PortalCarriageState(Carriage carriage) { this.carriage = carriage; }
	public static PortalCarriageState of(Carriage carriage) { return ((PortalCarriage) carriage).ctp$state(); }
	public boolean active() { return crossing != null; }
	public Carriage.DimensionalCarriageEntity hiddenCoupling() {
		if (hiddenCoupling == null) hiddenCoupling = carriage.new DimensionalCarriageEntity();
		return hiddenCoupling;
	}
	public Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> entities() { return ((PortalCarriage) carriage).ctp$entities(); }

	public void prepare() {
		if (carriage.train == null || carriage.train.graph == null) return;
		TravellingPoint front = carriage.getLeadingPoint();
		TravellingPoint rear = carriage.getTrailingPoint();
		PortalPath.Result path = PortalPath.between(carriage.train.graph, rear, front, carriage.bogeySpacing + 20);
		if (path == null) return;
		sides.clear();
		crossing = path == null || path.portals() != 1 ? null : path.crossing();
		if (crossing != null && !crossing.entrance().dimension.equals(crossing.exit().dimension)) crossing = null;
		if (crossing == null) {
			collapse(front);
			return;
		}
		ResourceKey<Level> entrance = PortalSide.key(crossing.entrance());
		ResourceKey<Level> exit = PortalSide.key(crossing.exit());
		for (CarriageBogey bogey : new CarriageBogey[] { carriage.leadingBogey(), carriage.trailingBogey() }) {
			for (TravellingPoint point : new TravellingPoint[] { bogey.leading(), bogey.trailing() }) {
				PortalPath.Result segment = PortalPath.between(carriage.train.graph, rear, point, carriage.bogeySpacing + 20);
				sides.put(point.node1, segment != null && segment.crossing() != null ? exit : entrance);
			}
		}
		Carriage.DimensionalCarriageEntity existing = entities().remove(crossing.entrance().dimension);
		if (existing != null) {
			// The existing entity stays at its physical end even when travelling backwards.
			ResourceKey<Level> key = existing.positionAnchor != null && existing.positionAnchor.distanceToSqr(crossing.exit().getLocation())
				< existing.positionAnchor.distanceToSqr(crossing.entrance().getLocation()) ? exit : entrance;
			entities().put(key, existing);
			setEntitySide(existing, key);
		}
	}

	private void collapse(TravellingPoint front) {
		if (front.edge == null || front.node1 == null) return;
		ResourceKey<Level> dimension = front.node1.getLocation().dimension;
		if (entities().containsKey(dimension)) return;
		var position = front.getPosition(carriage.train.graph);
		ResourceKey<Level> closest = null;
		double distance = Double.MAX_VALUE;
		for (var entry : entities().entrySet()) {
			if (!PortalSide.isSide(entry.getKey()) || !PortalSide.dimension(entry.getKey()).equals(dimension) || entry.getValue().positionAnchor == null) continue;
			double candidate = entry.getValue().positionAnchor.distanceToSqr(position);
			if (candidate < distance) { distance = candidate; closest = entry.getKey(); }
		}
		if (closest != null) {
			Carriage.DimensionalCarriageEntity survivor = entities().remove(closest);
			entities().put(dimension, survivor);
			setEntitySide(survivor, dimension);
		}
	}

	public TrackNodeLocation location(TrackNode node) {
		TrackNodeLocation actual = node.getLocation();
		ResourceKey<Level> side = sides.get(node);
		if (side == null) return actual;
		TrackNodeLocation scoped = new TrackNodeLocation(actual.getLocation()).in(side);
		scoped.yOffsetPixels = actual.yOffsetPixels;
		return scoped;
	}
	public ResourceKey<Level> bogeyDimension(CarriageBogey bogey) {
		ResourceKey<Level> first = sides.get(bogey.leading().node1);
		ResourceKey<Level> second = sides.get(bogey.trailing().node1);
		return first == null ? bogey.getDimension() : first.equals(second) ? first : null;
	}
	public static void setEntitySide(Carriage.DimensionalCarriageEntity dce, ResourceKey<Level> key) {
		CarriageContraptionEntity entity = dce.entity.get();
		if (entity != null) ((PortalCarriageEntity) entity).ctp$setSide(PortalSide.isSide(key) ? key.location().toString() : "");
	}
	public static Carriage.DimensionalCarriageEntity forEntity(Carriage carriage, CarriageContraptionEntity entity) {
		var entities = ((PortalCarriage) carriage).ctp$entities();
		// createEntity assigns the weak reference before calling setCarriage.
		for (var entry : entities.entrySet()) {
			if (entry.getValue().entity.get() == entity) {
				if (!entity.level().isClientSide) setEntitySide(entry.getValue(), entry.getKey());
				return entry.getValue();
			}
		}
		String side = ((PortalCarriageEntity) entity).ctp$getSide();
		ResourceKey<Level> key = side.isEmpty() ? entity.level().dimension() : ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(side));
		return carriage.getDimensional(key);
	}
}
