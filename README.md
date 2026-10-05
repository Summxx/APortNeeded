# A Port Needed

A Forge 1.20.1 mod. It brings back GTNH-era blocks that are missing on newer
versions.

> **Beta.** This is a personal project and a showcase. It has no permission or endorsement from the original authors.
> All borrowed content is used under the MIT licence and credited in [CREDITS.md](CREDITS.md).

## Content

### Architecture (ArchitectureCraft port)
- Over 100 shapes cut at the Sawbench from almost any block, using the GTNH models: roofs, slopes and slope tiles,
  cylinders, spheres, arches, columns and capitals, cornices, balustrades and banisters, glowing variants.
- Smart roofs: ridges, valleys and hip roofs connect to their neighbours.
- Placement works like the GTNH version: right side up on floors, upside down on ceilings, top or bottom half on
  walls, sneak puts the base against the wall, banisters line up on stairs, shapes match the profile next to them.
- Chisel (roof connections, cladding removal), hammer (rotate, change side, flip railings) and cladding for a
  secondary material.
- Textures projected like vanilla blocks, per-face materials and biome tint, shading without seams with or without
  shaders, placement preview.

### Decoration
- 16 Chroma Lamps: coloured lamps that all give light level 15, with neon textures.
- 16 tinted glass blocks.
- Built-in LabPBR textures: emissive lamps, reflective glass.
- CB Microblocks support out of the box.

### Compatibility
- **GregTech CEu**: when present, the tools and the Sawbench use steel parts and GregTech crafting tools, so they are
  early game. Without it, iron recipes are used.
- **CB Microblocks**: lamps and glass are registered as microblock materials, no config needed.
- Works on dedicated servers and in multiplayer.

## Disclaimer
The architecture module started from TridentMC's 1.21 ArchitectureCraft rewrite. AI-assisted coding ("vibe coding")
was used to clean up that port, bring it to Forge 1.20.1 and implement the compatibility. The optimisation work, all
features and how they behave (placement, roofs, tools, shading, decoration blocks, recipes) were done by me, tested in
game and reworked until they matched the GTNH version.

## Building
Requires Java 17.

```sh
./gradlew build
```

The mod jar is written to `build/libs/`. `./gradlew runClient` and `./gradlew runServer` start a development client
or server.

## Contact
Author: **OBS07**. Feedback and bug reports on Discord: **@sum_h**

## Licence and credits
MIT licence, see [LICENSE](LICENSE). Original authors (gcewing, TridentMC, the GT New Horizons team, OneEyeMaker,
riciJak) and the borrowed content are listed in [CREDITS.md](CREDITS.md).
