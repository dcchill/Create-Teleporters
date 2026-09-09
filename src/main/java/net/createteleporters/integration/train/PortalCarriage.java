package net.createteleporters.integration.train;

import net.minecraft.nbt.CompoundTag;
import com.simibubi.create.content.trains.entity.Carriage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.Map;

public interface PortalCarriage {
	Map<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> ctp$entities();
	Map<Integer, CompoundTag> ctp$passengers();
	PortalCarriageState ctp$state();
}
