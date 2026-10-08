# A Port Needed

A Forge 1.20.1 mod. It brings back GTNH-era blocks that are missing on newer
versions.

**[Join the Discord](https://discord.gg/ZgXeQqePy3)** for updates, the roadmap, feedback and bug reports.

## ⬇️ Download

**[Download the latest version here](https://github.com/Summxx/APortNeeded/releases)**

1. Open the link above. The newest version is at the top.
2. Under **Assets**, click the `apn-x.x.x.jar` file to download it (not "Source code").
3. Put the jar in the `mods` folder of your Forge 1.20.1 instance. On a server, install it on the server **and** on every client.

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
- Built-in LabPBR textures: emissive lamps, smooth non-metallic glass. The built-in **APN: Classic Tinted Glass**
  resource pack (Options > Resource Packs) brings back the older reflective glass look.
- CB Microblocks support out of the box.

### Compatibility
- **Recipes**: simple vanilla recipes (iron, sticks, planks). Modpack makers can replace them with KubeJS or a
  datapack to fit their progression.
- **CB Microblocks**: lamps and glass are registered as microblock materials, no config needed.
- Works on dedicated servers and in multiplayer.

## Guide

### Sawbench
Put a block in the Sawbench, pick a page (Roofing, Rounded, Classical, Arches, Railings, Other) and a shape, then take
the result. Each shape costs a set number of blocks and gives a set number of items. Almost any full block works as a
material. Glowing variants (light level 15) and the Cladding sheet are on the Other page.

### Placing shapes
- **Floor or ceiling:** aim at the top of a block for a shape sitting upright, at the bottom of a block for an
  upside-down one.
- **Walls:** a shape placed against a wall still sits on the floor: aim at the top half of the wall for upside down,
  the bottom half for upright.
- **Sneak to place against the wall:** sneaking puts the base of the shape against the face you aim at. Use it for
  vertical or sideways slopes, slope tiles and roofs.
- **Rotation:** the shape turns towards the edge (or corner) of the face nearest to where you aim.
- **Next to another shape:** a shape placed against another shape copies its orientation so roofs, cornices and
  architraves line up. Sneak to ignore the neighbour.
- **Banisters:** placed on stairs (vanilla or shapes), they follow the stairs. Railings go to the edge of the block
  nearest to where you aim.

### Tools
- **Hammer:** right click turns the shape a quarter. On railings it first switches them to the other edge. Sneak +
  right click moves the shape to its next side.
- **Chisel:** right click near the edge of a roof breaks or restores the connection with the roof on that side. Right
  click in the centre takes the cladding off. It also breaks glass, glass panes, glowstone and ice and gives them back.
- **Cladding:** cut a block into Cladding at the Sawbench, then right click a roof, slope or shape with secondary faces
  to cover its sloped faces with that material. Sides and base keep the main material.

### Roofs
Roof tiles, corners, ridges and valleys join their neighbours automatically, including hip roofs. Use the chisel to
disconnect two roofs that should stay separate.

### Decoration
- **Chroma Lamps:** glowstone dust, smooth stone and a dye give 4 lamps. Surround a dye with 8 lamps of any colour to
  recolour them.
- **Tinted glass:** 8 glass around an amethyst shard give 8 tinted glass. Use a stonecutter to switch between the 16
  variants.

### Resource pack and shaders
- The tinted glass specular maps only use smoothness, so it looks like plain glass with any shader pack.
- For the older reflective, slightly glowing glass, enable the built-in **APN: Classic Tinted Glass** pack in
  Options > Resource Packs.
- Shapes are lit per vertex from the light around them, so shadows and torch light blend between neighbouring blocks,
  with or without shaders.

### Recipes for modpacks
Recipes are plain vanilla. Replace them with KubeJS or a datapack: `apn:sawbench`, `apn:sawblade`,
`apn:large_pulley`, `apn:hammer`, `apn:chisel`, `apn:chroma_lamp_<colour>`, `apn:tinted_glass_<0-15>`.

## Roadmap
Planned, by priority:
1. CurseForge release
2. Catwalks port (awaiting permission from the original developer)
3. Strips and posts for slopes, at the same angle as each slope

Progress is tracked on the [Discord](https://discord.gg/ZgXeQqePy3).

## Changelog

### 0.1.6
- Fixed dark slope faces with normal maps under shaders.

### 0.1.5
- Shapes keep their material when placed with Effortless Building or pasted with the Building Gadgets 2 Copy-Paste Gadget.
- Tinted glass and chroma lamps can be used as FramedBlocks camos.

### 0.1.4
- Fixed the sawbench menu on dedicated servers: picking a shape on some pages jumped back to the Cylinder.

### 0.1.3
- Fixed inverted shadows on full faces of shapes, such as the base of slopes placed against a wall.
- Sloped and curved faces are now lit per vertex from the light around them: shadows blend between blocks, follow
  overhangs and update with torches.
- Smooth shading on cylinders, spheres and other curved shapes without shaders.
- Fixed triangle faces looking darker with shader packs.
- Fixed hairline gaps between B slope tiles.
- Tinted glass specular maps now only use smoothness, so the glass no longer looks metallic or mirror-like with
  shaders. The older look is available as the built-in **APN: Classic Tinted Glass** resource pack.
- Shader pack detection fixed, faster chunk building for shapes without sloped faces, lighter chunk loading and
  smaller saves on servers.

### 0.1.2
- Recipes are now plain vanilla so the mod fits any modpack. Pack makers can change them with KubeJS or a datapack.

### 0.1.1
- Fixed a crash on dedicated servers at startup.

### 0.1.0
- First public beta.

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
Author: **OBS07**. Feedback and bug reports on the [A Port Needed Discord](https://discord.gg/ZgXeQqePy3).

## Licence and credits
MIT licence, see [LICENSE](LICENSE). Original authors (gcewing, TridentMC, the GT New Horizons team, OneEyeMaker,
riciJak) and the borrowed content are listed in [CREDITS.md](CREDITS.md).
