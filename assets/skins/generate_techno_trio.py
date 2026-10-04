#!/usr/bin/env python3
"""Procedural generator for the prisma, biolum and quantum skin pairs.

Same recipe as generate_nebula.py (which this file imports for its SDF/noise
helpers): everything is drawn analytically so the originals in
assets/skins/originals/ are reproducible byte-for-byte. Runtime exports are
produced with tools/PrepareSkinAsset.java:

    java tools/PrepareSkinAsset.java originals/board_<id>.png \
        app/src/main/res/drawable-nodpi/board_<id>.png 1024 512
    java tools/PrepareSkinAsset.java originals/board_<id>.png \
        app/src/main/res/drawable-nodpi/board_<id>_preview.png 384 192
    java tools/PrepareSkinAsset.java originals/domino_<id>.png \
        app/src/main/res/drawable-nodpi/domino_<id>.png 256 512
    java tools/PrepareSkinAsset.java originals/domino_<id>.png \
        app/src/main/res/drawable-nodpi/domino_<id>_preview.png 96 192

Design rules honoured here (docs/GDD.md + assets/skins/README.md):
  * board artwork is opaque; the centre stays calm so dominoes read clearly;
  * the domino shell is a rounded silhouette on a genuinely transparent canvas;
  * every PCB ships as a pair: board carries the theme, the shell contrasts
    with the shared light-alloy frame + matte black faces recipe, and pips are
    always drawn by the game (never baked into the art);
  * one accent per pair on the divider/rim/bolts: prisma = iridescent
    diffraction spectrum, biolum = abyssal green/teal glow, quantum = magenta
    glitch with chromatic split.
"""

from dataclasses import dataclass, field as dc_field
from pathlib import Path
from typing import Callable, Tuple

import numpy as np
from PIL import Image, ImageDraw

import generate_nebula as ref

# Shared helpers (byte-compatible with the nebula generator).
smoothstep = ref.smoothstep
screen = ref.screen
blur = ref.blur
fbm = ref.fbm
sd_rrect = ref.sd_rrect
cover = ref.cover
shrink = ref.shrink
metal_band = ref.metal_band
glow_line = ref.glow_line
matte_face = ref.matte_face

SEED_BASE = 2026
SC = 2  # originals render at 2x the runtime size

HERE = Path(__file__).resolve().parent
ORIGINALS = HERE / "originals"

# Shared shell recipe colors (same alloy as the nebula pair).
ALLOY_DARK = ref.ALLOY_DARK
ALLOY_LIGHT = ref.ALLOY_LIGHT
FACE_TOP = ref.FACE_TOP
FACE_BOT = ref.FACE_BOT


# Small helpers --------------------------------------------------------------
def col(c):
    """(3,) color -> broadcastable (1, 1, 3)."""
    return np.asarray(c, float).reshape(1, 1, 3)


def hsv_rgb(h, s, v):
    """Vectorised HSV -> RGB (h in 0..1, scalar or array; returns 0..255)."""
    i = np.floor(h * 6.0)
    f = h * 6.0 - i
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    i = i.astype(int) % 6
    r = np.select([i == k for k in range(6)], [v, q, p, p, t, v])
    g = np.select([i == k for k in range(6)], [t, v, v, q, p, p])
    b = np.select([i == k for k in range(6)], [p, p, t, v, v, q])
    return np.stack([r, g, b], axis=-1) * 255.0


def spectrum_map(n, phase=0.0, sat=0.78, val=1.0, cycles=1.0):
    """Iridescent rim/divider color varying along X: (1, n, 3)."""
    hue = (np.arange(n, dtype=float) / n * cycles + phase) % 1.0
    return hsv_rgb(hue[None, :], sat, val)


def trace_layer(rng, W, H, box, inset, count, width):
    """Border-following polyline layer (circuit traces / organic veins)."""
    layer = Image.new("L", (W, H), 0)
    drawer = ImageDraw.Draw(layer)
    dirs = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]
    x0, y0, x1, y1 = (c + inset for c in box)
    for i in range(count):
        side = i % 4
        if side == 0:
            px, py, di = rng.uniform(x0, x1), y0, 4
        elif side == 1:
            px, py, di = x1, rng.uniform(y0, y1), 6
        elif side == 2:
            px, py, di = rng.uniform(x0, x1), y1, 0
        else:
            px, py, di = x0, rng.uniform(y0, y1), 2
        pts = [(px, py)]
        for _ in range(int(rng.integers(2, 6))):
            dx, dy = dirs[di]
            step = float(rng.integers(3, 8)) * 6 * SC
            nx, ny = px + dx * step, py + dy * step
            if not (x0 <= nx <= x1 and y0 <= ny <= y1):
                break
            px, py = nx, ny
            pts.append((px, py))
            di = (di + int(rng.integers(-1, 2))) % 8
        if len(pts) < 2:
            continue
        drawer.line(pts, fill=210, width=width, joint="curve")
        drawer.ellipse([pts[-1][0] - 3 * SC, pts[-1][1] - 3 * SC,
                        pts[-1][0] + 3 * SC, pts[-1][1] + 3 * SC], fill=185)
    return layer


def particles(rgb, X, Y, box, radius, seed, count, palette,
              edge_bias=None, gain=1.0, halo_gain=0.35):
    """Sparse glowing specks (stars, plankton, hot pixels) that survive 2x downscale."""
    H, W = X.shape
    rng = np.random.default_rng(seed)
    xs = rng.integers(int(box[0]), int(box[2]), count)
    ys = rng.integers(int(box[1]), int(box[3]), count)
    keep = sd_rrect(xs.astype(float), ys.astype(float), *box, radius) < -3.0 * SC
    xs, ys = xs[keep], ys[keep]
    if edge_bias is not None and xs.size:
        m = rng.random(xs.size) < np.clip(edge_bias[ys, xs], 0.0, 1.0)
        xs, ys = xs[m], ys[m]
    if xs.size == 0:
        return rgb
    bright = rng.random(xs.size) ** 2.6 * 0.9 + 0.12
    pick = rng.integers(0, len(palette), xs.size)
    cores = [np.zeros((H, W), np.float32) for _ in palette]
    for j, core in enumerate(cores):
        m = pick == j
        np.add.at(core, (ys[m], xs[m]), bright[m])
        for dy, dx, w in ((-1, 0, .38), (1, 0, .38), (0, -1, .38), (0, 1, .38)):
            np.add.at(core, (ys[m] + dy, xs[m] + dx), bright[m] * w)
    acc = np.zeros((H, W, 3), np.float32)
    for core, color in zip(cores, palette):
        acc += core[..., None] * np.asarray(color, float)[None, None, :]
    big = bright > 0.72
    halo = np.zeros((H, W), np.float32)
    np.add.at(halo, (ys[big], xs[big]), bright[big])
    halo = blur(halo, 3.0 * SC)
    rgb = screen(rgb, acc * gain)
    rgb = screen(rgb, halo[..., None] * np.array([255, 255, 255], float)[None, None, :] * halo_gain)
    return rgb


# Theme ----------------------------------------------------------------------
@dataclass
class Theme:
    name: str
    seed: int
    deep_top: Tuple[float, float, float]
    deep_bot: Tuple[float, float, float]
    outer_bg: Tuple[float, float, float]
    a: Tuple[int, int, int]          # rim/trace/bolt accent A
    b: Tuple[int, int, int]          # rim/trace/LED accent B
    bolt_hi: Tuple[int, int, int]    # bolt specular tint
    div_bloom: Tuple[int, int, int]  # shell divider glow
    div_core: Tuple[int, int, int]   # shell divider core line
    field: Callable = dc_field(repr=False)
    rim_spectrum: bool = False       # prisma: rim color sweeps the spectrum
    divider_spectrum: bool = False   # prisma: divider sweeps the spectrum
    rim_scale: float = 1.0


def base_panel(theme, W, H, X, Y, box, radius):
    """Dark field gradient + calm-centre edge factor, shared by all three themes."""
    d = sd_rrect(X, Y, *box, radius)
    cov = cover(d)
    t = (Y - box[1]) / max(box[3] - box[1], 1.0)
    rgb = (np.asarray(theme.deep_top, float)[None, None, :] * (1.0 - t)[..., None]
           + np.asarray(theme.deep_bot, float)[None, None, :] * t[..., None])
    calm_box = shrink(box, 0.46)
    d_calm = sd_rrect(X, Y, *calm_box, radius * 0.5)
    edge = smoothstep(0.0, min(box[2] - box[0], box[3] - box[1]) * 0.45, d_calm)
    return rgb, cov, edge


def grain(rgb, seed, amount=5.0):
    rng = np.random.default_rng(seed)
    return rgb + (rng.random((rgb.shape[0], rgb.shape[1], 1)) - 0.5) * amount


# Field materials ------------------------------------------------------------
def prisma_field(theme, W, H, X, Y, box, radius):
    """Diffraction sheen: rainbow bands sweeping diagonally, hugging the border."""
    rgb, cov, edge = base_panel(theme, W, H, X, Y, box, radius)
    ang = np.deg2rad(24.0)
    s = (X * np.cos(ang) + Y * np.sin(ang)) / (240.0 * SC)
    warp = fbm(W, H, 4, theme.seed + 11) * 0.55
    sheen = hsv_rgb((s + warp) % 1.0, 0.72, 1.0)
    dens = fbm(W, H, 5, theme.seed + 12)
    mask = smoothstep(0.54, 0.88, dens) * (0.04 + 0.96 * edge)
    rgb = screen(rgb, mask[..., None] * sheen * 0.40)
    rgb = particles(rgb, X, Y, box, radius, theme.seed + 13, 2600,
                    [(255, 255, 255), (255, 214, 160), (255, 170, 225)],
                    edge_bias=edge)
    return grain(rgb, theme.seed + 14), cov, edge


def biolum_field(theme, W, H, X, Y, box, radius):
    """Abyssal veins: glowing organic walks with node tips + drifting plankton."""
    rgb, cov, edge = base_panel(theme, W, H, X, Y, box, radius)
    veins_a = trace_layer(np.random.default_rng(theme.seed + 21), W, H, box,
                          10 * SC, 26, max(2, int(2.4 * SC)))
    veins_b = trace_layer(np.random.default_rng(theme.seed + 22), W, H, box,
                          10 * SC, 26, max(2, int(2.4 * SC)))
    edge_mod = (0.06 + 0.94 * edge)[..., None]
    for layer, color in ((veins_a, theme.a), (veins_b, theme.b)):
        core = np.asarray(layer, np.float32) / 255.0
        halo = blur(core, 6.0 * SC)
        rgb = screen(rgb, (core * 0.72 + halo * 0.95)[..., None] * col(color) * edge_mod)
    rgb = particles(rgb, X, Y, box, radius, theme.seed + 23, 1400,
                    [(80, 255, 210), (110, 255, 170), (210, 255, 245)],
                    edge_bias=np.clip(edge * 1.15, 0.0, 1.0), halo_gain=0.45)
    return grain(rgb, theme.seed + 24), cov, edge


def quantum_field(theme, W, H, X, Y, box, radius):
    """Glitch segments with chromatic split, blocks and hot pixels."""
    rgb, cov, edge = base_panel(theme, W, H, X, Y, box, radius)
    rng = np.random.default_rng(theme.seed + 31)
    mag = Image.new("L", (W, H), 0)
    cyan = Image.new("L", (W, H), 0)
    white = Image.new("L", (W, H), 0)
    dm, dc, dw = ImageDraw.Draw(mag), ImageDraw.Draw(cyan), ImageDraw.Draw(white)
    x0, y0, x1, y1 = box
    for _ in range(52):
        # Most segments hug the top/bottom borders to keep the centre calm.
        if rng.random() < 0.72:
            third = (y1 - y0) * 0.3
            sy = rng.uniform(y0, y0 + third) if rng.random() < 0.5 else rng.uniform(y1 - third, y1)
        else:
            sy = rng.uniform(y0, y1)
        ln = float(rng.integers(18, 130)) * SC
        sx = rng.uniform(x0, max(x0 + 1.0, x1 - ln))
        ln = min(ln, x1 - sx)
        if ln <= 4 * SC:
            continue
        th = max(1, int(rng.integers(1, 4) * SC * 0.6))
        split = float(rng.integers(2, 6)) * SC
        dm.rectangle([sx, sy, sx + ln, sy + th], fill=210)
        dc.rectangle([sx + split, sy, sx + split + ln, sy + th], fill=210)
        if rng.random() < 0.45:
            dw.rectangle([sx + split * .5, sy, sx + split * .5 + ln, sy + max(1, th // 2)], fill=120)
    for _ in range(12):
        bx = rng.uniform(x0, x1 - 40 * SC)
        by = rng.uniform(y0, y1 - 40 * SC)
        bs = float(rng.integers(8, 22)) * SC
        dw.rectangle([bx, by, bx + bs, by + bs * rng.uniform(0.3, 1.0)], fill=70)
    edge_mod = (0.05 + 0.95 * edge)[..., None]
    for layer, color, gain in ((mag, theme.a, 1.0), (cyan, theme.b, 1.0),
                               (white, (245, 245, 255), 0.8)):
        core = np.asarray(layer, np.float32) / 255.0
        halo = blur(core, 3.5 * SC)
        rgb = screen(rgb, (core * 0.95 + halo * 0.55)[..., None] * col(color) * gain * edge_mod)
    rgb = particles(rgb, X, Y, box, radius, theme.seed + 33, 900,
                    [(255, 62, 200), (57, 240, 255), (255, 255, 255)],
                    edge_bias=edge)
    return grain(rgb, theme.seed + 34), cov, edge


# Board ----------------------------------------------------------------------
def make_board(theme):
    W, H = 1024 * SC, 512 * SC
    rng = np.random.default_rng(theme.seed)
    X, Y = np.meshgrid(np.arange(W, dtype=float), np.arange(H, dtype=float))

    OUTER = (4 * SC, 4 * SC, 1020 * SC, 508 * SC)
    FIELD = (44 * SC, 34 * SC, 980 * SC, 478 * SC)
    d_out = sd_rrect(X, Y, *OUTER, 42 * SC)
    d_fld = sd_rrect(X, Y, *FIELD, 28 * SC)

    rgb = np.empty((H, W, 3), float)
    rgb[:] = theme.outer_bg

    field, field_cov, edge = theme.field(theme, W, H, X, Y, FIELD, 28 * SC)
    rgb = rgb * (1 - field_cov[..., None]) + field * field_cov[..., None]

    # Circuit traces near the field border, never crossing the calm centre.
    trace_v = trace_layer(rng, W, H, FIELD, 7 * SC, 15, max(2, int(1.5 * SC)))
    trace_c = trace_layer(rng, W, H, FIELD, 7 * SC, 15, max(2, int(1.5 * SC)))
    edge_mod = (0.12 + 0.88 * edge)[..., None]
    for layer, color in ((trace_v, theme.a), (trace_c, theme.b)):
        core = np.asarray(layer, np.float32) / 255.0
        halo = blur(core, 4.0 * SC)
        rgb = screen(rgb, (core * 0.55 + halo * 0.50)[..., None] * col(color) * edge_mod)

    # Metal frame band.
    band, _ = metal_band(X, Y, d_out, d_fld, rng)
    band_cov = cover(d_out) * (1.0 - cover(d_fld))
    rgb = rgb * (1 - band_cov[..., None]) + band * band_cov[..., None]

    # Rims: accent A outside, accent B hugging the field (prisma sweeps both).
    if theme.rim_spectrum:
        rim_a = spectrum_map(W, 0.0, sat=0.72)
        rim_b = spectrum_map(W, 0.5, sat=0.72)
    else:
        rim_a, rim_b = col(theme.a), col(theme.b)
    g = theme.rim_scale
    rgb = screen(rgb, glow_line(d_out, -3 * SC, 3 * SC, 0.55 * g)[:, :, None] * rim_a)
    rgb = screen(rgb, glow_line(d_out, 4 * SC, 5 * SC, 0.22 * g)[:, :, None] * rim_a)
    rgb = screen(rgb, glow_line(d_fld, -2 * SC, 2.2 * SC, 0.85 * g)[:, :, None] * rim_b)
    rgb = screen(rgb, glow_line(d_fld, -6 * SC, 6 * SC, 0.35 * g)[:, :, None] * rim_b)

    # LED strips on the frame (top, bottom, left, right).
    led = col(theme.b)
    for sx0, sy0, sx1, sy1 in ((470, 6, 554, 22), (470, 490, 554, 506),
                               (8, 206, 24, 306), (1000, 206, 1016, 306)):
        box = (sx0 * SC, sy0 * SC, sx1 * SC, sy1 * SC)
        housing = cover(sd_rrect(X, Y, *box, 5 * SC))
        rgb = rgb * (1 - housing[..., None] * 0.8) + ref.METAL_DARK[None, None, :] * housing[..., None] * 0.8
        core_box = (box[0] + 4 * SC, box[1] + 4 * SC, box[2] - 4 * SC, box[3] - 4 * SC)
        core = cover(sd_rrect(X, Y, *core_box, 3 * SC))
        rgb = screen(rgb, blur(core, 3.0 * SC)[:, :, None] * led * 0.75)
        rgb = screen(rgb, core[:, :, None] * led * 1.25)

    # Corner bolts: dark metal cap, glowing accent ring, specular arc.
    for bx, by in ((33, 33), (991, 33), (33, 479), (991, 479)):
        dist = np.hypot(X - bx * SC, Y - by * SC)
        R = 23 * SC
        cap = cover(dist - R)
        t = np.clip(dist / R, 0.0, 1.0)
        shade = 0.25 + 0.60 * np.sin(np.pi * t) ** 0.6
        shade *= np.where(t < 0.52, 0.72, 1.0)
        bolt = (ref.METAL_DARK[None, None, :]
                + (ref.METAL_LIGHT - ref.METAL_DARK)[None, None, :] * shade[..., None])
        bolt += (rng.random((H, W, 1)) - 0.5) * 8.0
        rgb = rgb * (1 - cap[..., None]) + bolt * cap[..., None]
        ring = glow_line(dist, 0.74 * R, 1.7 * SC, 1.0) * cap
        rgb = screen(rgb, blur(ring, 2.0 * SC)[:, :, None] * col(theme.a) * 0.7)
        rgb = screen(rgb, ring[:, :, None] * col(theme.bolt_hi))
        angle = np.arctan2(Y - by * SC, X - bx * SC)
        spec = cap * (t > 0.55) * (t < 0.92) * np.exp(-((angle + 2.4) / 0.5) ** 2) * 0.5
        rgb = screen(rgb, spec[:, :, None] * np.array([255, 255, 255])[None, None, :])

    # Opaque canvas: keep the outside of the rounded frame calm and dark.
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8)).convert("RGBA")


# Domino shell ---------------------------------------------------------------
def make_shell(theme):
    """Shared light-alloy recipe: the contrast against the dark board is the point."""
    W, H = 256 * SC, 512 * SC
    rng = np.random.default_rng(theme.seed + 7)
    X, Y = np.meshgrid(np.arange(W, dtype=float), np.arange(H, dtype=float))

    OUTER = (2 * SC, 2 * SC, 254 * SC, 510 * SC)
    FACE_T = (18 * SC, 18 * SC, 238 * SC, 240 * SC)
    FACE_B = (18 * SC, 272 * SC, 238 * SC, 494 * SC)
    d_out = sd_rrect(X, Y, *OUTER, 34 * SC)
    d_top = sd_rrect(X, Y, *FACE_T, 26 * SC)
    d_bot = sd_rrect(X, Y, *FACE_B, 26 * SC)

    rgb = np.empty((H, W, 3), float)
    rgb[:] = ref.OUTER_BG

    d_faces = np.minimum(d_top, d_bot)
    band, _ = metal_band(X, Y, d_out, d_faces, rng, perforate=False,
                         dark=ALLOY_DARK, light=ALLOY_LIGHT)
    band_cov = cover(d_out) * (1.0 - cover(d_faces))
    rgb = rgb * (1 - band_cov[..., None]) + band * band_cov[..., None]

    # Divider bar: same alloy, carrying the pair's only accent color.
    div = cover(d_out) * ((Y > 239 * SC) & (Y < 273 * SC))
    u = np.clip((Y - 240 * SC) / (32 * SC), 0.0, 1.0)
    shade = 0.24 + 0.58 * np.sin(np.pi * u) ** 0.7
    bar = ALLOY_DARK[None, None, :] + (ALLOY_LIGHT - ALLOY_DARK)[None, None, :] * shade[..., None]
    bar += (rng.random((H, W, 1)) - 0.5) * 7.0
    rgb = rgb * (1 - div[..., None]) + bar * div[..., None]
    sep = np.exp(-((Y - 241 * SC) / (2.0 * SC)) ** 2) + np.exp(-((Y - 271 * SC) / (2.0 * SC)) ** 2)
    rgb *= (1 - sep * 0.5 * cover(d_out))[..., None]
    if theme.divider_spectrum:
        bloom_map = spectrum_map(W, 0.1, sat=0.75)
        core_map = spectrum_map(W, 0.1, sat=0.45)
    else:
        bloom_map, core_map = col(theme.div_bloom), col(theme.div_core)
    core = np.exp(-((Y - 256 * SC) / (1.8 * SC)) ** 2) * cover(d_out)
    rgb = screen(rgb, blur(core, 2.5 * SC)[:, :, None] * bloom_map * 0.55)
    rgb = screen(rgb, core[:, :, None] * core_map)

    # Matte black faces on top: no theme texture, no glow - pips stay king.
    for face, d_face, seed in ((FACE_T, d_top, theme.seed + 7), (FACE_B, d_bot, theme.seed + 21)):
        f_rgb = matte_face(W, H, X, Y, face, 26 * SC, seed)
        f_cov = cover(d_face)
        rgb = rgb * (1 - f_cov[..., None]) + f_rgb * f_cov[..., None]

    # Armour panel lines along the long edges of the alloy frame.
    cut = Image.new("L", (W, H), 0)
    dc = ImageDraw.Draw(cut)
    for side_x in (3 * SC, 253 * SC):
        for y in (86, 158, 356, 428):
            yc = y * SC
            dc.line([(side_x, yc - 14 * SC), (side_x + 13 * SC, yc),
                     (side_x, yc + 14 * SC)], fill=255, width=max(2, int(2.0 * SC)), joint="curve")
    cut_a = np.asarray(cut, np.float32) / 255.0 * cover(d_out) * (1.0 - cover(d_faces))
    rgb *= (1 - blur(cut_a, 1.5 * SC) * 0.70)[..., None]
    rgb = screen(rgb, blur(cut_a, 3.0 * SC)[:, :, None] * np.array([255, 255, 255])[None, None, :] * 0.28)

    # Outline: recessed dark edge plus a faint accent hairline to tie the pair.
    rgb *= (1 - glow_line(d_out, -3 * SC, 3.0 * SC, 0.50))[..., None]
    rgb = screen(rgb, glow_line(d_out, 0.0, 2.2 * SC, 0.30)[:, :, None] * col(theme.a))

    alpha = cover(d_out)
    out = np.dstack([np.clip(rgb, 0, 255), np.clip(alpha * 255.0, 0, 255)]).astype(np.uint8)
    return Image.fromarray(out)


# Catalogue ------------------------------------------------------------------
THEMES = [
    Theme(
        name="prisma", seed=SEED_BASE + 1,
        deep_top=(18, 15, 24), deep_bot=(8, 6, 12), outer_bg=(9, 8, 14),
        a=(255, 197, 122), b=(154, 214, 255), bolt_hi=(255, 238, 210),
        div_bloom=(255, 205, 150), div_core=(255, 255, 255),
        field=prisma_field, rim_spectrum=True, divider_spectrum=True, rim_scale=0.85,
    ),
    Theme(
        name="biolum", seed=SEED_BASE + 2,
        deep_top=(6, 16, 18), deep_bot=(3, 8, 10), outer_bg=(4, 9, 11),
        a=(61, 255, 160), b=(47, 232, 216), bolt_hi=(214, 255, 240),
        div_bloom=(47, 232, 216), div_core=(200, 255, 240),
        field=biolum_field,
    ),
    Theme(
        name="quantum", seed=SEED_BASE + 3,
        deep_top=(12, 10, 14), deep_bot=(5, 4, 7), outer_bg=(7, 6, 9),
        a=(255, 62, 200), b=(57, 240, 255), bolt_hi=(255, 220, 248),
        div_bloom=(255, 62, 200), div_core=(255, 190, 240),
        field=quantum_field,
    ),
]


def main():
    ORIGINALS.mkdir(parents=True, exist_ok=True)
    for theme in THEMES:
        board = make_board(theme)
        shell = make_shell(theme)
        board.save(ORIGINALS / f"board_{theme.name}.png")
        shell.save(ORIGINALS / f"domino_{theme.name}.png")
        print(f"board_{theme.name}.png {board.size}  domino_{theme.name}.png {shell.size}")


if __name__ == "__main__":
    main()
