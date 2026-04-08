package net.createteleporters.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CTPConfigConfiguration {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<Double> ENTITY_TP_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> ITEM_TP_RANGE;
	public static final ModConfigSpec.BooleanValue IMMERSIVE_PORTALS_COMPAT;
	public static final ModConfigSpec.BooleanValue ALLOW_POCKETDBLOCK_BREAKING;
	public static final ModConfigSpec.BooleanValue FORCE_PORTAL_TO_PORTAL_BINDING;
	static {
		BUILDER.push("Ranges");
		ENTITY_TP_RANGE = BUILDER.comment("Max Range of Entity Teleporter").define("Entity Teleporter Range", (double) 450);
		ITEM_TP_RANGE = BUILDER.comment("Max Range of Item Teleporter").define("Item Teleporter Range", (double) 500);
		BUILDER.pop();

		BUILDER.push("Integration");
		IMMERSIVE_PORTALS_COMPAT = BUILDER.comment("Enable Immersive Portals compatibility. When enabled, quantum portals will use Immersive Portals API instead of vanilla teleportation commands. Requires Immersive Portals mod to be installed.").define("Immersive Portals Compatibility", false);
		BUILDER.pop();

		BUILDER.push("Blocks");
		ALLOW_POCKETDBLOCK_BREAKING = BUILDER.comment("Allow players to break Pocket Dimension Blocks. When disabled, the block becomes unbreakable.").define("Allow Pocket Dimension Block Breaking", false);
		BUILDER.pop();

		BUILDER.push("Portal Binding");
		FORCE_PORTAL_TO_PORTAL_BINDING = BUILDER.comment("When enabled, portals will read teleportation coordinates from the Advanced TP Link item placed inside the portal block. When disabled (default), portals will use linked portal coordinates.\nNOTE: This option is automatically disabled when Immersive Portals Compatibility is enabled.").define("Custom Portal Bind To Coordinates?", true);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

	/**
	 * Checks if coordinate binding is effectively enabled.
	 * Returns false if Immersive Portals Compat is enabled (as it overrides this setting).
	 */
	public static boolean isCoordinateBindingEnabled() {
		if (IMMERSIVE_PORTALS_COMPAT.get()) {
			return false; // Immersive Portals always uses portal-to-portal linking
		}
		return FORCE_PORTAL_TO_PORTAL_BINDING.get();
	}

}