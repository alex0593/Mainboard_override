#!/usr/bin/env python3
"""Baked seed-42 preview overlays for the free-mode selection cards.

Replaces PuzzlePreview.kt's runtime bitmap generation: each scenario gets a
static 640x400 PNG with transparency (grid, in-game port and buff sprites,
firewalls, daemon badge) that the UI layers over the equipped board-skin
preview. Ports, buffs and their labels mirror Board.kt (same artwork, offsets
and text), so the card matches what the board looks like in-game.

Geometry comes from game-domain/src/test/resources/overlay-geometry-seed42.json,
kept in sync with LevelGenerator by PreviewOverlayGeometryTest. Hidden honeypots
and solutions are absent by construction: the dump never contains them.

Draws at 4x for anti-aliasing, downsamples premultiplied (LANCZOS) to the 2x
original in originals/, then export the runtime PNG with:

    java tools/PrepareSkinAsset.java originals/overlay_<id>.png \
        app/src/main/res/drawable-nodpi/scenario_<id>_overlay.png 640 400
"""

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent            # assets/scenarios
ROOT = HERE.parent.parent                          # repository root
GEOMETRY = ROOT / "game-domain/src/test/resources/overlay-geometry-seed42.json"
ORIGINALS = HERE / "originals"
FIREWALL = ROOT / "app/src/main/res/drawable-nodpi/board_firewall.png"
PORT_S0 = ROOT / "app/src/main/res/drawable-nodpi/board_port_s0_v1.png"
PORT_X6 = ROOT / "app/src/main/res/drawable-nodpi/board_port_x6_v1.png"
BUFFS = {
    "RAM_RESERVE": ROOT / "app/src/main/res/drawable-nodpi/board_ram_reserve_v1.png",
    "TRACE_COOLER": ROOT / "app/src/main/res/drawable-nodpi/board_trace_cooler_v1.png",
}
# FontFamily.Monospace + FontWeight.Black stand-in (same weight/width class).
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"

W, H = 640, 400   # runtime canvas, same as PuzzlePreviewCache.render
SS = 4            # supersampling factor while drawing

GRID = (100, 175, 185, 70)   # Color.argb(70, 100, 175, 185)
CYAN = (57, 231, 224)        # Color(0xFF39E7E0)
DANGER = (255, 65, 85)       # Color(0xFFFF4155)


def downscale(img, size):
    """LANCZOS resize that treats alpha correctly (premultiplied in between)."""
    arr = np.asarray(img, np.float32) / 255.0
    alpha = arr[..., 3]
    pm = Image.fromarray((arr[..., :3] * alpha[..., None] * 255).astype(np.uint8), "RGB")
    a = Image.fromarray((alpha * 255).astype(np.uint8), "L")
    pm_r = np.asarray(pm.resize(size, Image.LANCZOS), np.float32) / 255.0
    a_r = np.asarray(a.resize(size, Image.LANCZOS), np.float32) / 255.0
    rgb = np.clip(pm_r / np.maximum(a_r[..., None], 1e-6), 0.0, 1.0)
    out = np.dstack([rgb, a_r[..., None]])
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


def render(scenario):
    w, h = scenario["width"], scenario["height"]
    cell = min(540.0 / w, 300.0 / h)
    left = (W - cell * w) / 2
    top = (H - cell * h) / 2

    def bounds(p):
        x, y = p
        return (left + x * cell, top + y * cell,
                left + (x + 1) * cell, top + (y + 1) * cell)

    def sbox(p, inset=0.0):
        x0, y0, x1, y1 = bounds(p)
        return [(x0 + inset) * SS, (y0 + inset) * SS,
                (x1 - inset) * SS, (y1 - inset) * SS]

    canvas = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    grid = ImageDraw.Draw(canvas)

    # Cell grid: one rect per cell, like the original Canvas.drawRect loop.
    for y in range(h):
        for x in range(w):
            grid.rectangle(sbox((x, y)), outline=GRID, width=SS)
    # Shared cell edges were drawn twice by adjacent rects on Android (alpha
    # blended twice); emulate with a second composited pass over interior lines.
    internal = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    di = ImageDraw.Draw(internal)
    for i in range(1, w):
        gx = (left + i * cell) * SS
        di.line([(gx, top * SS), (gx, (top + h * cell) * SS)], fill=GRID, width=SS)
    for j in range(1, h):
        gy = (top + j * cell) * SS
        di.line([(left * SS, gy), ((left + w * cell) * SS, gy)], fill=GRID, width=SS)
    canvas = Image.alpha_composite(canvas, internal)
    draw = ImageDraw.Draw(canvas)

    def outer_box(outer_col, row):
        """Cell box in Board.kt's (width + 2) board coordinates, supersampled."""
        x0 = (left + (outer_col - 1) * cell) * SS
        y0 = (top + row * cell) * SS
        size = cell * SS
        return [x0, y0, x0 + size, y0 + size]

    def fit_content(path, box):
        """Sprite cropped to its opaque content and fitted into box (SS px)."""
        sprite = Image.open(path).convert("RGBA")
        content = sprite.getchannel("A").point(lambda a: 255 if a > 16 else 0)
        sprite = sprite.crop(content.getbbox())
        bw, bh = box[2] - box[0], box[3] - box[1]
        scale = min(bw / sprite.width, bh / sprite.height)
        sprite = sprite.resize(
            (max(1, round(sprite.width * scale)), max(1, round(sprite.height * scale))),
            Image.LANCZOS)
        x = round(box[0] + (bw - sprite.width) / 2)
        y = round(box[1] + (bh - sprite.height) / 2)
        assert 0 <= x and 0 <= y and x + sprite.width <= W * SS and y + sprite.height <= H * SS, path
        return sprite, (x, y)

    def paste(path, box):
        sprite, xy = fit_content(path, box)
        canvas.alpha_composite(sprite, xy)

    # The ports use the in-game artwork at PortSprite's offsets: .18 cell from
    # each board edge, so the connector plugs into the outer grid line.
    paste(PORT_S0, outer_box(.18, scenario["start"][1]))
    paste(PORT_X6, outer_box(w + .82, scenario["extraction"][1]))

    # threatBitmap() trims the transparent export margins before the cell draws
    # it; fit_content does the same, so the diamond fills the cell instead of
    # the 677x369 canvas it ships in.
    for p in scenario["firewalls"]:
        paste(FIREWALL, sbox(p, inset=0.5))

    for buff in scenario["buffs"]:
        paste(BUFFS[buff["k"]], sbox(buff["p"], inset=0.5))

    draw = ImageDraw.Draw(canvas)
    font_cache = {}

    def label(text, pos, size, color, anchor):
        px = max(1, int(round(size * SS)))
        font = font_cache.get(px) or ImageFont.truetype(FONT, px)
        font_cache[px] = font
        draw.text(pos, text, font=font, fill=(*color, 255), anchor=anchor)

    # Port labels float just outside each port, like PortSprite's offset text.
    port = outer_box(.18, scenario["start"][1])
    label("S0", (max(1.0, port[0] - .24 * cell * SS), (port[1] + port[3]) / 2),
          cell * .26, CYAN, "lm")
    port = outer_box(w + .82, scenario["extraction"][1])
    label("X6", (min(W * SS - 1.0, port[2] + .24 * cell * SS), (port[1] + port[3]) / 2),
          cell * .26, CYAN, "rm")

    for buff in scenario["buffs"]:
        x0, y0, x1, _ = bounds(buff["p"])
        label("+1R" if buff["k"] == "RAM_RESERVE" else "\u22128",
              ((x1 - .06) * SS, (y0 + .04) * SS), cell * .30, CYAN, "rt")

    if scenario["daemon"] is not None:
        col, row = scenario["daemon"]
        box = outer_box(col + 1, row)
        cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
        if [col, row] == scenario["extraction"]:
            cx -= .18 * cell * SS  # ride the port artwork, clear of the X6 label
        label("D!", (cx, cy), cell * .36, DANGER, "mm")

    return canvas


def main():
    ORIGINALS.mkdir(parents=True, exist_ok=True)
    scenarios = json.loads(GEOMETRY.read_text())
    for scenario in scenarios:
        overlay = downscale(render(scenario), (W * 2, H * 2))
        out = ORIGINALS / f"overlay_{scenario['id']}.png"
        overlay.save(out)
        print(f"overlay_{scenario['id']}.png {overlay.size}")


if __name__ == "__main__":
    main()
