# Lightweight technological skins

Generated with the integrated `imagegen` tool, one call per asset. The selected original PNGs are in `originals/`; only reduced runtime exports in `app/src/main/res/drawable-nodpi/` are packaged. Both pairs are free.

Exports: domino shells 256 × 512 (512 KiB decoded ARGB), domino previews 96 × 192 (72 KiB); boards 1024 × 512 (2 MiB), board previews 384 × 192 (288 KiB). Transparency is preserved. Real pip values are drawn by the game, never generated as part of the shell. The circuit skin uses a 256 × 512 sibling export instead of decoding its 887 × 1774 original (about 6 MiB).

Reproduce an export with a full JDK: `java -Djava.awt.headless=true tools/PrepareSkinAsset.java SOURCE DESTINATION WIDTH HEIGHT`. This performs offline resizing and PNG encoding; it is not an image-generation API client. Original artwork remains untouched.

## Final prompts

### domino_obsidian

Use case: stylized-concept
Asset type: lightweight mobile domino puzzle game skin
Primary request: A single obsidian black domino shell with subtle cyan circuit accents along the outer rim. Vertical 1:2 silhouette, top-down orthographic, symmetric top and bottom. Two perfectly blank dark matte playing faces separated by a thin central divider. Entire object visible tightly framed, genuinely transparent background. No dots, pips, numbers, text, shadows outside the object, logos or watermark.
Style: clean polished raster game asset, precise silhouette and restrained technological ornament.

### domino_ceramic

Use case: stylized-concept
Asset type: lightweight mobile domino puzzle game skin
Primary request: A single ivory ceramic domino shell with subtle deep green circuit accents along the outer rim. Vertical 1:2 silhouette, top-down orthographic, symmetric top and bottom. Two perfectly blank ivory matte playing faces separated by a thin central divider. Entire object visible tightly framed, genuinely transparent background. No dots, pips, numbers, text, shadows outside the object, logos or watermark.
Style: clean polished raster game asset, precise silhouette and restrained technological ornament.

### board_obsidian

Use case: stylized-concept
Asset type: lightweight mobile domino puzzle game skin
Primary request: A landscape 2:1 top-down orthographic game board background, obsidian black matte electronics panel with restrained cyan circuit traces and small hardware details confined to its outer edges. Central 85 percent is nearly uniform dark matte charcoal, clear for gameplay overlays. Artwork fills canvas, opaque background. Static baked lighting, subtle texture, excellent readability at small size. No grid, tiles, pips, numbers, letters, text, logos or watermark.
Style: clean polished raster game asset, precise silhouette and restrained technological ornament.

### board_ceramic

Use case: stylized-concept
Asset type: lightweight mobile domino puzzle game skin
Primary request: A landscape 2:1 top-down orthographic game board background, ivory ceramic electronics panel with restrained deep green circuit traces and small hardware details confined to its outer edges. Central 85 percent is a uniform dark desaturated green recessed playing surface, clear for gameplay overlays. Artwork fills canvas, opaque background. Static baked lighting, subtle texture, excellent readability at small size. No grid, tiles, pips, numbers, letters, text, logos or watermark.
Style: clean polished raster game asset, precise silhouette and restrained technological ornament.
