package net.createteleporters.integration;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackShape;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.init.CreateteleportersModBlocks;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SameDimensionPortalTrackHelper {
	private static final Set<String> LOGGED_PORTAL_EDGES = ConcurrentHashMap.newKeySet();

	private SameDimensionPortalTrackHelper() {
	}

	public static boolean isSameDimensionPortalEdge(MinecraftServer server, TrackNodeLocation first, TrackNodeLocation second) {
		if (server == null || first == null || second == null) {
			return false;
		}
		if (first.getDimension() == null || second.getDimension() == null || !Objects.equals(first.getDimension(), second.getDimension())) {
			return false;
		}

		ServerLevel level = server.getLevel(first.getDimension());
		if (level == null) {
			return false;
		}

		for (BlockPos firstPos : first.allAdjacent()) {
			if (isPortalTrackBoundToNode(level, firstPos, second.getDimension(), second)) {
				return true;
			}
		}
		for (BlockPos secondPos : second.allAdjacent()) {
			if (isPortalTrackBoundToNode(level, secondPos, first.getDimension(), first)) {
				return true;
			}
		}

		return false;
	}

	public static void logPortalEdgeMarked(String source, TrackNodeLocation first, TrackNodeLocation second) {
		String key = toEdgeKey(first, second);
		if (LOGGED_PORTAL_EDGES.add(key)) {
			CreateteleportersMod.LOGGER.info("Create train same-dimension portal edge enabled by {}: {} <-> {}", source, first, second);
		}
	}

	private static boolean isPortalTrackBoundToNode(ServerLevel level, BlockPos pos, ResourceKey<Level> expectedDimension, TrackNodeLocation expectedNode) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(CreateteleportersModBlocks.SAME_DIMENSION_PORTAL_TRACK.get())
				|| !(state.getBlock() instanceof TrackBlock)
				|| !state.hasProperty(TrackBlock.SHAPE)
				|| !state.hasProperty(TrackBlock.HAS_BE)) {
			return false;
		}
		if (!state.getValue(TrackBlock.HAS_BE)) {
			return false;
		}

		TrackShape shape = state.getValue(TrackBlock.SHAPE);
		if (!shape.isPortal()) {
			return false;
		}
		if (!(level.getBlockEntity(pos) instanceof TrackBlockEntity trackBlockEntity)) {
			return false;
		}

		Pair<ResourceKey<Level>, BlockPos> boundLocation = trackBlockEntity.boundLocation;
		return boundLocation != null
			&& expectedDimension.equals(boundLocation.getFirst())
			&& isNodeAtTrackBlock(expectedNode, boundLocation.getSecond());
	}

	private static boolean isNodeAtTrackBlock(TrackNodeLocation node, BlockPos trackPos) {
		if (node.allAdjacent().contains(trackPos)) {
			return true;
		}

		return node.getLocation().distanceTo(Vec3.atBottomCenterOf(trackPos)) <= 1.25D;
	}

	private static String toEdgeKey(TrackNodeLocation first, TrackNodeLocation second) {
		String firstKey = first.getDimension().location() + "|" + first.getX() + "," + first.getY() + "," + first.getZ() + "|" + first.yOffsetPixels;
		String secondKey = second.getDimension().location() + "|" + second.getX() + "," + second.getY() + "," + second.getZ() + "|" + second.yOffsetPixels;
		return firstKey.compareTo(secondKey) <= 0 ? firstKey + "<->" + secondKey : secondKey + "<->" + firstKey;
	}
}
