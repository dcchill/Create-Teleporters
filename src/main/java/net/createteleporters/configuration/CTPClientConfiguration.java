package net.createteleporters.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CTPClientConfiguration {
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue LIQUID_CUSTOM_PORTAL;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		LIQUID_CUSTOM_PORTAL = builder
			.define("Liquid Custom Portal", true);
		SPEC = builder.build();
	}

	private CTPClientConfiguration() {
	}
}
