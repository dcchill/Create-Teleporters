package net.createteleporters.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CTPConfigConfiguration {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.ConfigValue<Double> ENTITY_TP_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> ITEM_TP_RANGE;
	static {
		BUILDER.push("Ranges");
		ENTITY_TP_RANGE = BUILDER.comment("Max Range of Entity Teleporter").define("Entity Teleporter Range", (double) 450);
		ITEM_TP_RANGE = BUILDER.comment("Max Range of Item Teleporter").define("Item Teleporter Range", (double) 500);
		BUILDER.pop();

		SPEC = BUILDER.build();
	}

}