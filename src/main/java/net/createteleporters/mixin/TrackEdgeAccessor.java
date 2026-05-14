package net.createteleporters.mixin;

import com.simibubi.create.content.trains.graph.TrackEdge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TrackEdge.class, remap = false)
public interface TrackEdgeAccessor {
	@Accessor("interDimensional")
	void createteleporters$setInterDimensional(boolean interDimensional);
}
