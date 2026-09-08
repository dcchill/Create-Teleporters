package net.createteleporters.integration.train;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Keys used only inside Create's carriage-portion map; these are not world dimensions. */
public final class PortalSide {
	private static final String PREFIX = "train_side/";
	private PortalSide() { }
	public static ResourceKey<Level> key(TrackNodeLocation node) {
		ResourceLocation dim = node.dimension.location();
		return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("createteleporters",
			PREFIX + node.getX() + "/" + node.getY() + "/" + node.getZ() + "/" + node.yOffsetPixels + "/" + dim.getNamespace() + "/" + dim.getPath()));
	}
	public static boolean isSide(ResourceKey<Level> key) {
		return key != null && key.location().getNamespace().equals("createteleporters") && key.location().getPath().startsWith(PREFIX);
	}
	public static ResourceKey<Level> dimension(ResourceKey<Level> key) {
		if (!isSide(key)) return key;
		String[] parts = key.location().getPath().substring(PREFIX.length()).split("/", 6);
		return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(parts[4], parts[5]));
	}
	public static TrackNodeLocation pivot(ResourceKey<Level> key) {
		String[] parts = key.location().getPath().substring(PREFIX.length()).split("/", 6);
		TrackNodeLocation node = new TrackNodeLocation(Integer.parseInt(parts[0]) / 2.0, Integer.parseInt(parts[1]) / 2.0,
			Integer.parseInt(parts[2]) / 2.0).in(dimension(key));
		node.yOffsetPixels = Integer.parseInt(parts[3]);
		return node;
	}
}
