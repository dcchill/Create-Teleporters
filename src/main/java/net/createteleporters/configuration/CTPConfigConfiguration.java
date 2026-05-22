package net.createteleporters.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CTPConfigConfiguration {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	private static final Pattern LEGACY_COORDINATE_CONFIG_PATTERN = Pattern.compile("\"Custom Portal Bind To Coordinates\\?\"\\s*=\\s*(true|false)", Pattern.CASE_INSENSITIVE);

	public static final ModConfigSpec.ConfigValue<Double> ENTITY_TP_RANGE;
	public static final ModConfigSpec.ConfigValue<Double> ITEM_TP_RANGE;
	public static final ModConfigSpec.BooleanValue IMMERSIVE_PORTALS_COMPAT;
	public static final ModConfigSpec.BooleanValue ALLOW_POCKETDBLOCK_BREAKING;
	public static final boolean LEGACY_CUSTOM_PORTAL_COORDINATE_BINDING_DEFAULT = readLegacyCoordinateBindingFromDisk().orElse(true);
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

	private static Optional<Boolean> readLegacyCoordinateBindingFromDisk() {
		List<Path> candidates = List.of(
			Path.of("config", "createteleportersconfig.toml"),
			Path.of("run", "config", "createteleportersconfig.toml")
		);

		for (Path candidate : candidates) {
			if (!Files.isRegularFile(candidate)) {
				continue;
			}
			try {
				Matcher matcher = LEGACY_COORDINATE_CONFIG_PATTERN.matcher(Files.readString(candidate));
				if (matcher.find()) {
					return Optional.of(Boolean.parseBoolean(matcher.group(1)));
				}
			} catch (IOException ignored) {
			}
		}

		return Optional.empty();
	}
}
