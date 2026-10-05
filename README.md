<h1 style="text-align: center;">Thank you for 4 Million Downloads!</h1>

# Create Teleporters Remastered

___**Warning: Upgrading your old world with the new Remastered Version may cause issues with the Custom Portal.  On upgrade, your Custom Portal will be replaced with the placeholder block.  Just mine that to get the new Custom Portal Base.**___

# <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">R</span><span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">equires Crea</span><span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">te</span>

<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">A complete rebuild of the original <em>Create Teleporters</em> mod.&nbsp; Redesigned to fit in more with the new aesthetics of the Create Mod and to be simpler to use. This remastered version fixes old bugs and refines the mechanics.</span>

# <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Whats changed?<br></span>

*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">No more Teleporter Receivers.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Removed Gravitity Stabilizer</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Pets no longer teleport with you.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Cave gen is fixed.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Pocket Dimensions won't be overridden.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">The Custom Portal is now a multiblock.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Different sized Entity Teleporters.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">No more GeckoLib</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Simplified aesthetic and models.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Connected Textures for the Quantum Casing</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">New GUI's that are inline with the Create mods look.</span>
*   <span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">TP Links (Not Advanced TP Links) now have a set range, which is tweakable in the config</span>

# __<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Entity Teleporters</span>__

<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;"><img src="https://media.forgecdn.net/attachments/description/null/description_1d4e2f05-2d4c-46ac-89d0-d19a5f04ecd3.png"></span>

# __<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Item Teleporter</span>__

<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;"><img src="https://media.forgecdn.net/attachments/description/null/description_21cea649-2396-4ada-bdec-37df61f6fc9f.png" width="279" height="297"></span>

# __<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Custom Portal</span>__

<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;">Made of a Custom Portal Base and a frame of Quantum Casings.</span>

## ![](https://media.forgecdn.net/attachments/description/null/description_c56999ea-8ae6-40fd-9b37-1e4e6e0a15fa.png)

<span style="font-size: 24px; font-family: arial, helvetica, sans-serif;"><img src="https://media.forgecdn.net/attachments/description/null/description_73d9c7b6-8844-4ee4-95d1-c9b8d908ba72.png" width="319" height="285"></span>


## Development regression checks

Train portal endpoints must be placed at both linked portals in the corresponding
lane and height. Compatible flat tracks are converted and bound together; an empty
or incompatible exit leaves the waiting track intact without generating a rail.

Use JDK 21. Train checks run with `./gradlew.bat -PtrainPortalTests runGameTestServer`.
To exercise the optional Immersive Portals integration, put its runtime dependencies
(such as the NeoForge Cloth Config jar) in `build/immersivePortalTestDependencies`, then run:

```powershell
.\gradlew.bat "-PimmersivePortalTestJar=path/to/immersive-portals.jar" runGameTestServer
```

Use the upstream NeoForge 1.21.1 Immersive Portals jar.
The property also works with `runClient` for visual checks. Test sources and these
runtime jars are excluded from a normal build. The Immersive checks cover all 16
rotation combinations, scaled floor and reciprocal transforms, neighboring pairs,
version-1 migration, NBT reload, missing entities, deactivation, destruction, and
rollback when cluster completion fails. The test world is `build/ip-gametest`.

Verified server regressions with Immersive Portals 6.0.7, Cloth Config 15.0.140,
NeoForge 21.1.228, Create 6.0.10, and Minecraft 1.21.1. All 16 rotation cases and
the lifecycle checks passed. Client rendering was not visually checked; inspect
both sides of east/west and north/south frames against marked destination floors.
Moving Sable sublevels are outside these stationary-frame checks.
