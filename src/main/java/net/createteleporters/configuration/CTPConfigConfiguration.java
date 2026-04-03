package net.createteleporters.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CTPConfigConfiguration {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<Double> ENTITY_TP_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> ITEM_TP_RANGE;
	public static final ModConfigSpec.ConfigValue<Boolean> IMMERSIVE_PORTALS_COMPAT;
	public static final ModConfigSpec.ConfigValue<Boolean> ALLOW_POCKETDBLOCK_BREAKING;
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

		SPEC = BUILDER.build();
	}

}