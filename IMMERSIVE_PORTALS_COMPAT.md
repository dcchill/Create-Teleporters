# Immersive Portals Compatibility

This mod now includes **optional compatibility** with [Immersive Portals](https://www.cursemaven.com/immersive-portals) mod. When enabled, your quantum portals will use Immersive Portals' seamless portal rendering and teleportation system instead of placing quantum portal blocks.

## Features

- **Seamless Portal Rendering**: See through portals without the opaque quantum portal blocks
- **Smooth Teleportation**: Entities teleport seamlessly without loading screens
- **Configurable**: Enable/disable via mod configuration
- **Automatic Detection**: Only activates when Immersive Portals is installed
- **Command-Based**: Uses Immersive Portals commands for reliable portal creation

## Requirements

- **Immersive Portals for Forge** (version compatible with your Minecraft version)
  - For Minecraft 1.21.1: Immersive Portals 3.0+ recommended
- **Cloth Config API** (required by Immersive Portals)
  - Download from: https://modrinth.com/mod/cloth-config

## Setup

### For Players

1. Install **Immersive Portals** mod alongside this mod
2. Install **Cloth Config API** (required by Immersive Portals)
3. Launch Minecraft and open your world
4. Go to **Mods** → **Create Teleporters** → **Configuration**
5. Enable **"Immersive Portals Compatibility"** in the Integration section
6. Restart your world for changes to take effect

### For Developers (Building with the mod)

If you want to include Immersive Portals during development, uncomment the following lines in `custom.gradle`:

```gradle
dependencies {
    // Immersive Portals compatibility (optional - requires mod to be installed at runtime)
    compileOnly fg.deobf("curse.maven:immersive-portals-for-forge-495466:5025474")
    runtimeOnly fg.deobf("curse.maven:immersive-portals-for-forge-495466:5025474")
}
```

**Note**: Replace the file ID `5025474` with the latest version compatible with your Minecraft version.

## How It Works

### Portal Activation

When Immersive Portals compatibility is enabled:

1. **Portal Detection**: The mod detects your quantum casing ring structure (same as vanilla)
2. **Portal Creation**: Uses Immersive Portals `/portal make_portal` command to create a see-through portal entity
3. **Sizing**: Automatically sets the portal size to match your ring interior using `/portal set_portal_size`
4. **Linking**: Sets the destination using `/portal set_portal_destination` to match your linked portal
5. **Fluid Consumption**: Quantum Fluid is still consumed to maintain the portal (4 mB per tick)

### Commands Used

The integration uses these Immersive Portals commands internally:

```
/portal make_portal <width> <height> <dimension> <x> <y> <z>
/portal set_portal_size <width> <height>
/portal set_portal_destination <dimension> <x> <y> <z>
/portal set_portal_nbt {teleportable:false}
/portal delete_portal
```

### Portal Deactivation

When the portal frame is broken or fluid runs out:
- The Immersive Portals portal entity is automatically removed using `/portal delete_portal`
- No quantum portal blocks need to be cleared

## Configuration

### Config File Location

`.minecraft/config/createteleporters.toml`

### Available Options

```toml
[Integration]
# Enable Immersive Portals compatibility. When enabled, quantum portals will use 
# Immersive Portals API instead of vanilla teleportation commands. 
# Requires Immersive Portals mod to be installed.
ImmersivePortalsCompatibility = false
```

## Technical Details

### NBT Data

The portal base block stores additional NBT data when using Immersive Portals:

- `immersivePortalCreated` (boolean): Tracks whether an IP portal has been created
- All existing NBT data (linked coordinates, rotation, dimensions) remains unchanged

### Portal Dimensions

The Immersive Portals portal matches your quantum casing ring:
- **Minimum**: 5×4 (3×2 interior)
- **Maximum**: 23×23 (21×21 interior)
- **Orientation**: Respects the rotation of your portal base

### Create Mod Integration

Trains from the Create mod will continue to work with Immersive Portals enabled. The integration automatically detects which mode (vanilla or IP) is active and handles teleportation accordingly.

## Troubleshooting

### Portal doesn't appear

1. Ensure Immersive Portals is installed and loaded
2. Check that "Immersive Portals Compatibility" is enabled in config
3. Verify the portal frame is valid (quantum casing ring)
4. Ensure the base has 8000+ mB Quantum Fluid
5. Check logs for error messages

### Portal appears but doesn't teleport

1. Verify the portal is linked using an Advanced TP Link
2. Check that both portals are active and have fluid
3. Ensure the target dimension exists and is accessible

### Performance issues

Immersive Portals can be more demanding than vanilla portals. Try:
- Reducing the number of active portals
- Lowering Immersive Portals render distance in its config
- Using smaller portal sizes

## Commands

When debugging, you can use Immersive Portals commands:

- `/portal list` - List all active portals
- `/portal tpme <dim> <x> <y> <z>` - Teleport without loading screen
- `/kill @e[type=immersive_portals:portal]` - Remove all portal entities (use carefully!)

## Credits

- **Immersive Portals Mod**: qouteall and the iPortalTeam
- **Integration**: Created for Create Teleporters mod

## License

This integration follows the same license as the base Create Teleporters mod.
