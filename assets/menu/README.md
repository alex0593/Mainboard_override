# Menu artwork

Generated using the built-in image_gen tool (not CLI). Text labels remain native, translated and accessible. Originals are kept here; only reduced exports in app/src/main/res/drawable-nodpi are packaged.

Exports: background 1280×640, module 256×256, main button surfaces 384×128, compact surface 256×128, and a 1280×640 transparent pulse overlay. Button corners are preserved with nine-slice drawing. Motion uses only the cached overlay on a 10-second cycle; it runs only while the menu lifecycle is RESUMED and reduced motion is off. No claim of lower power consumption without on-device profiling.

## v2 menu pass

The v2 surfaces keep the v1 geometry but reduce saturation: graphite and charcoal dominate, teal and amber are reserved for edge details, and the button centers remain quiet for native translated labels. `menu_background_v2` is static; `menu_pulse_overlay_v1` is the only animated layer. The overlay has real alpha and leaves the center transparent.

## v1 transition and shared panel

`menu_shutters_v1` is used as a full-screen transition layer. It contains two matching closed doors with a centered seam; Compose crops each half and slides them horizontally so the destination is revealed only after the close phase. `menu_panel_v1` is used behind dialogs and progression cards. Both are generated with the built-in `image_gen` tool and are packaged as opaque PNGs; all labels remain native Compose text.

## v1 shared chrome surfaces

`menu_header_v1` backs progression and gameplay headers. `menu_card_v1` backs scenario/challenge cards, script cards, the hardware panel and dialogs. Both are opaque RGB exports with an intentionally empty center so Compose remains responsible for translated text, icons and accessibility.
`menu_skin_card_v1` is intentionally separate: its cooler steel-blue frame and small geometric corner accents identify cosmetic skin cards without changing the shared menu language.

### menu_header_v1

```text
Use case: stylized-concept
Asset type: reusable opaque raster game UI header surface
Primary request: wide game window header surface, front orthographic, approximately 3:1 aspect ratio, deep graphite center, thin cyan-blue luminous border, restrained amber corner details, subtle metallic texture and inner shadow. Keep the center empty for native Compose text and controls. No words, icons, symbols, characters, transparency or checkerboard. Border suitable for horizontal stretching.
```

### menu_card_v1

```text
Use case: stylized-concept
Asset type: reusable opaque raster scenario/challenge card and gameplay panel surface
Primary request: front orthographic approximately 4:3 graphite metal panel, thin cyan-blue edge, restrained amber corner accents, subtle metallic texture and empty center for native Compose content. No words, icons, symbols, characters, transparency or checkerboard. Corners remain legible when slightly stretched.
```

### menu_skin_card_v1

```text
Use case: stylized-concept
Asset type: reusable opaque raster card surface dedicated to a skin gallery
Primary request: approximately 4:3 deep graphite panel with a cooler steel-blue brushed metal frame, thin cyan accent line, restrained amber indicators and subtle geometric corner motifs. Keep the center broad and empty for native Compose artwork, translated skin names, prices and buttons. Front orthographic, full bleed, no perspective, transparency, checkerboard, words, icons, logos or watermark.
```

### menu_shutters_v1

```text
Use case: stylized-concept
Asset type: landscape 2:1 opaque mobile game transition shutter texture
Primary request: two closed symmetrical sliding metal doors meeting at a perfectly straight vertical center seam. Front orthographic view, full bleed graphite and charcoal brushed metal, shallow machined bevels, sparse desaturated cyan edge traces and tiny amber indicators. Match a restrained futuristic mainboard menu. Dark quiet surfaces, no perspective, no gaps or transparency anywhere, no text, logos, icons or watermark. Each half will slide horizontally offscreen; center seam exactly halfway across image.
```

### menu_panel_v1

```text
Use case: stylized-concept
Asset type: reusable 3:2 opaque raster panel for mobile game cards and dialogs
Primary request: flat front orthographic rectangular graphite metal panel. Thin machined beveled frame confined to the outer 8 percent, restrained desaturated cyan line along edges and tiny amber corner lights, subtly rounded corners. Central 84 percent quiet nearly black charcoal with subtle brushed texture, entirely empty for native UI content. Straight edges, uniform middle texture suitable for nine-slice rendering. Full bleed panel without exterior margins. Same understated mainboard industrial theme as dark metal game menu. No text, icons, symbols, logos, perspective, watermark or bright bloom.
```

## menu_background_v1

```text
Use case: stylized-concept
Asset type: landscape mobile game menu backdrop, 2:1 wide
Primary request: restrained futuristic mainboard surface in dark graphite with fine brushed metal grain, recessed sparse circuit paths near outer edges, faint cyan reflections and tiny amber lights. Large central 80 percent quiet dark negative space for readable UI overlay. Subtle tactile material and depth, no solid flat color, no busy decoration. Full bleed opaque background. No text, letters, icons, buttons, objects, logos or watermark.
```

## menu_module_v1

```text
Use case: stylized-concept
Asset type: transparent game menu decorative cutout
Primary request: single compact premium processor module, square graphite chip seated on a small circuit plate, subtly beveled hardware, brushed graphite metal and a few cyan light traces with tiny amber indicators. Three-quarter top view, strong simple silhouette, restrained detail readable as a 96dp decoration. Centered entire module with narrow padding. True transparent background, no floor or scene, no text, letters, logos, watermark or extra objects.
```

## menu_button_primary_v1

```text
Use case: stylized-concept
Asset type: reusable raster game menu primary button surface
Primary request: single front-facing horizontal rectangular button plate, 3:1 aspect ratio, dark graphite brushed metal center with subtle teal depth, softly beveled slim corners, thin restrained cyan light trim and one tiny amber accent at a corner. Central 80 percent perfectly clear for live UI text. Symmetric simple construction suitable for nine-slice stretching, straight horizontal edges and uniform middle texture, no central ornament. Fill image edge to edge with the plate, no surrounding margin, no perspective, no text, icons, letters, logos or watermark. Tech elegant, not busy.
```

## menu_button_secondary_v1

```text
Use case: stylized-concept
Asset type: reusable raster game menu secondary button surface
Primary request: single front-facing horizontal rectangular button plate, 3:1 aspect ratio, smoky dark graphite brushed metal with gentle blue reflections and very thin desaturated cyan trim. Low contrast bevels, tiny amber corner accent. Central 80 percent clear for live UI text. Symmetric simple construction suitable for nine-slice stretching, straight horizontal edges and uniform middle texture, no central ornament. Fill image edge to edge with the plate, no surrounding margin, no perspective, no text, icons, letters, logos or watermark. Understated tech, no dense circuitry.
```

## menu_button_compact_v1

```text
Use case: stylized-concept
Asset type: reusable raster compact game menu utility button surface
Primary request: single front-facing horizontal rectangular small button plate, 2:1 aspect ratio. Dark graphite satin metal with quiet material grain and soft edge reflections, slender muted cyan edge, subtly rounded beveled corners. Broad empty center for live UI label. Nine-slice friendly straight edges and uniform middle texture. Fill image edge to edge with plate, no surrounding margin. No perspective, text, letters, icons, logos, watermark, bright bloom or busy circuit decoration.
```
