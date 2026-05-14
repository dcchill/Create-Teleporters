package net.createteleporters.init;

import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;

import net.minecraft.resources.ResourceLocation;

import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.SameDimensionPortalTrackBlock;

public final class CreateteleportersTrackMaterials {
	public static final TrackMaterial SAME_DIMENSION_PORTAL = TrackMaterialFactory
			.make(ResourceLocation.fromNamespaceAndPath(CreateteleportersMod.MODID, "same_dimension_portal_track"))
			.lang("Same-Dimension Portal")
			.block(() -> () -> (SameDimensionPortalTrackBlock) CreateteleportersModBlocks.SAME_DIMENSION_PORTAL_TRACK.get())
			.particle(ResourceLocation.fromNamespaceAndPath("create", "block/palettes/stone_types/polished/andesite_cut_polished"))
			.defaultModels()
			.build();

	private CreateteleportersTrackMaterials() {
	}
}
