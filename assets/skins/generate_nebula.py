#!/usr/bin/env python3
"""Procedural generator for the Nebula skin pair (board + domino shell).

Everything is drawn analytically (signed distance fields + value noise) so the
originals in assets/skins/originals/ are reproducible byte-for-byte from this
file. Runtime exports are produced with tools/PrepareSkinAsset.java:

    java tools/PrepareSkinAsset.java originals/board_nebula.png \
        app/src/main/res/drawable-nodpi/board_nebula.png 1024 512
    java tools/PrepareSkinAsset.java originals/board_nebula.png \
        app/src/main/res/drawable-nodpi/board_nebula_preview.png 384 192
    java tools/PrepareSkinAsset.java originals/domino_nebula.png \
        app/src/main/res/drawable-nodpi/domino_nebula.png 256 512
    java tools/PrepareSkinAsset.java originals/domino_nebula.png \
        app/src/main/res/drawable-nodpi/domino_nebula_preview.png 96 192

Design rules honoured here (docs/GDD.md + assets/skins/README.md):
  * board artwork is opaque; the centre stays calm so dominoes read clearly;
  * the domino shell is a rounded silhouette on a genuinely transparent canvas;
  * the board carries the nebula theme (space field, cyan rims, violet bolts);
  * the shell contrasts instead of repeating it: light silver-lavender alloy
    frame, matte black faces, a single violet divider accent, so tiles read
    clearly on top of the dark board with starlight pips (0xFFFFF3E0).
"""

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SEED = 1113
SC = 2  # originals render at 2x the runtime size

HERE = Path(__file__).resolve().parent
ORIGINALS = HERE / "originals"

# Palette -------------------------------------------------------------------
DEEP_TOP = np.array([13, 11, 30], float)   # space field, top
DEEP_BOT = np.array([6, 5, 14], float)     # space field, bottom
OUTER_BG = np.array([8, 7, 18], float)     # opaque board corners / shell base
VIOLET = np.array([123, 77, 255], float)
VIOLET_DEEP = np.array([56, 28, 120], float)
CYAN = np.array([56, 225, 255], float)
METAL_DARK = np.array([30, 28, 40], float)
METAL_LIGHT = np.array([122, 116, 152], float)
ALLOY_DARK = np.array([112, 108, 136], float)    # shell frame: light alloy
ALLOY_LIGHT = np.array([240, 237, 254], float)
FACE_TOP = np.array([27, 26, 33], float)         # shell faces: matte black
FACE_BOT = np.array([13, 13, 17], float)
STARLIGHT = np.array([255, 243, 224], float)  # pip color 0xFFFFF3E0
STAR_VIOLET = np.array([214, 208, 255], float)


# Small math helpers --------------------------------------------------------
def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def screen(base, top):
    """Photographic screen blend, both float 0..255 arrays."""
    return 255.0 - (255.0 - base) * (255.0 - np.clip(top, 0, 255)) / 255.0


def blur(arr, radius):
    im = Image.fromarray(np.clip(arr * 255.0, 0, 255).astype(np.uint8))
    return np.asarray(im.filter(ImageFilter.GaussianBlur(radius)), np.float32) / 255.0


def fbm(w, h, octaves, seed, base=3):
    """Multi-octave value noise in 0..1, upscaled smoothly from tiny grids."""
    rng = np.random.default_rng(seed)
    total = np.zeros((h, w), np.float32)
    amp, norm = 1.0, 0.0
    for o in range(octaves):
        res = base * (2 ** o)
        grid = (rng.random((res + 1, res + 1)) * 255).astype(np.uint8)
        upscaled = Image.fromarray(grid).resize((w, h), Image.BICUBIC)
        total += amp * (np.asarray(upscaled, np.float32) / 255.0)
        norm += amp
        amp *= 0.55
    return total / norm


def sd_rrect(X, Y, x0, y0, x1, y1, r):
    """Signed distance to an axis-aligned rounded rectangle (negative inside)."""
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    hx, hy = (x1 - x0) / 2.0 - r, (y1 - y0) / 2.0 - r
    qx, qy = np.abs(X - cx) - hx, np.abs(Y - cy) - hy
    outside = np.hypot(np.maximum(qx, 0.0), np.maximum(qy, 0.0))
    inside = np.minimum(np.maximum(qx, qy), 0.0)
    return outside + inside - r


def cover(d):
    """Signed distance -> 0..1 anti-aliased coverage."""
    return np.clip(0.5 - d, 0.0, 1.0)


def shrink(box, factor):
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    hw, hh = (x1 - x0) * factor / 2.0, (y1 - y0) * factor / 2.0
    return cx - hw, cy - hh, cx + hw, cy + hh


# Shared space material -----------------------------------------------------
def space_field(W, H, X, Y, box, radius, seed, nebula_gain, star_count, calm):
    """Dark space panel: gradient, nebula kept out of the calm centre, stars.

    Returns (rgb, coverage, edge) where edge is 0 in the calm centre and 1
    near the panel border; traces and brighter nebula follow it.
    """
    d = sd_rrect(X, Y, *box, radius)
    cov = cover(d)

    t = (Y - box[1]) / max(box[3] - box[1], 1.0)
    rgb = DEEP_TOP[None, None, :] * (1.0 - t)[..., None] + DEEP_BOT[None, None, :] * t[..., None]

    # Nebula clouds: dense near the border, almost absent in the calm centre.
    calm_box = shrink(box, calm)
    d_calm = sd_rrect(X, Y, *calm_box, radius * 0.5)
    edge = smoothstep(0.0, min(box[2] - box[0], box[3] - box[1]) * 0.45, d_calm)
    dens = fbm(W, H, 6, seed)
    hue = fbm(W, H, 5, seed + 1)
    neb = smoothstep(0.50, 0.86, dens) * (0.06 + 0.94 * edge) * nebula_gain
    violet_mix = VIOLET_DEEP + (VIOLET - VIOLET_DEEP) * hue[..., None]
    cyan_mix = np.clip((hue - 0.62) * 3.2, 0.0, 1.0)[..., None]
    neb_color = violet_mix * (1.0 - cyan_mix) + CYAN * cyan_mix
    rgb = screen(rgb, neb[..., None] * neb_color)

    # Starlight: warm white and violet-white points, bright ones get halos.
    rng = np.random.default_rng(seed + 2)
    xs = rng.integers(int(box[0]), int(box[2]), star_count)
    ys = rng.integers(int(box[1]), int(box[3]), star_count)
    keep = (sd_rrect(xs.astype(float), ys.astype(float), *box, radius) < -3.0 * SC)
    xs, ys = xs[keep], ys[keep]
    bright = rng.random(xs.size) ** 2.6 * 0.95 + 0.10
    warm = rng.random(xs.size) < 0.55

    core_w = np.zeros((H, W), np.float32)
    core_v = np.zeros((H, W), np.float32)
    halo = np.zeros((H, W), np.float32)
    np.add.at(core_w, (ys[warm], xs[warm]), bright[warm])
    np.add.at(core_v, (ys[~warm], xs[~warm]), bright[~warm])
    # One-pixel cross so stars survive the 2x downscale with a soft edge.
    for dy, dx, w in ((-1, 0, .38), (1, 0, .38), (0, -1, .38), (0, 1, .38)):
        np.add.at(core_w, (ys[warm] + dy, xs[warm] + dx), bright[warm] * w)
        np.add.at(core_v, (ys[~warm] + dy, xs[~warm] + dx), bright[~warm] * w)
    big = bright > 0.72
    np.add.at(halo, (ys[big], xs[big]), bright[big])
    halo = blur(halo, 3.0 * SC)

    stars = core_w[..., None] * STARLIGHT + core_v[..., None] * STAR_VIOLET
    stars = screen(stars, halo[..., None] * (STARLIGHT * 0.55))
    rgb = screen(rgb, stars)

    # Fine sensor grain keeps the panel from reading as a flat gradient.
    rgb += (rng.random((H, W, 1)) - 0.5) * 5.0

    return rgb, cov, edge


def metal_band(X, Y, d_out, d_in, rng, perforate=True, dark=METAL_DARK, light=METAL_LIGHT):
    """Beveled metal between two signed distances (d_out outer, d_in inner)."""
    thick = d_in - d_out
    valid = thick > 1e-6
    u = np.clip(-d_out / np.maximum(thick, 1e-6), 0.0, 1.0)
    shade = 0.30 + 0.70 * np.sin(np.pi * u) ** 0.75
    metal = dark[None, None, :] + (light - dark)[None, None, :] * shade[..., None]
    metal += (rng.random((X.shape[0], X.shape[1], 1)) - 0.5) * 9.0

    if perforate:
        # Honeycomb dot texture on the thick parts of the band.
        pitch = 9.0 * SC
        dot = ((X % pitch - pitch / 2) ** 2 + (Y % pitch - pitch / 2) ** 2) < (2.0 * SC) ** 2
        metal *= np.where(dot & (thick > 18.0 * SC), 0.55, 1.0)[..., None]

    # Inner shadow where the band meets the field.
    shadow = np.exp(-np.clip(d_in, 0.0, None) / (6.0 * SC)) * 0.42
    metal *= (1.0 - shadow * (d_in > 0))[..., None]
    return np.where(valid[..., None], metal, 0.0), valid


def glow_line(d, offset, width, gain):
    """Gaussian glow profile following a signed-distance contour."""
    return np.exp(-((d - offset) / width) ** 2) * gain


def matte_face(W, H, X, Y, box, radius, seed):
    """Matte black tile face: quiet gradient, fine grain, recessed edge.

    Deliberately carries no nebula and no stars - the shell contrasts with
    the board instead of repeating its space texture.
    """
    d = sd_rrect(X, Y, *box, radius)
    t = (Y - box[1]) / max(box[3] - box[1], 1.0)
    rgb = FACE_TOP[None, None, :] * (1.0 - t)[..., None] + FACE_BOT[None, None, :] * t[..., None]
    rng = np.random.default_rng(seed)
    rgb += (rng.random((H, W, 1)) - 0.5) * 6.0
    inner = np.clip(-d, 0.0, None)
    rgb *= (1.0 - np.exp(-inner / (6.0 * SC)) * 0.32)[..., None]
    return rgb


# Board ---------------------------------------------------------------------
def make_board():
    W, H = 1024 * SC, 512 * SC
    rng = np.random.default_rng(SEED)
    X, Y = np.meshgrid(np.arange(W, dtype=float), np.arange(H, dtype=float))

    OUTER = (4 * SC, 4 * SC, 1020 * SC, 508 * SC)
    FIELD = (44 * SC, 34 * SC, 980 * SC, 478 * SC)
    d_out = sd_rrect(X, Y, *OUTER, 42 * SC)
    d_fld = sd_rrect(X, Y, *FIELD, 28 * SC)

    rgb = np.empty((H, W, 3), float)
    rgb[:] = OUTER_BG

    field, field_cov, edge = space_field(W, H, X, Y, FIELD, 28 * SC, SEED, 1.0, 5200, 0.46)
    rgb = rgb * (1 - field_cov[..., None]) + field * field_cov[..., None]

    # Circuit traces near the field border, never crossing the calm centre.
    trace_v = Image.new("L", (W, H), 0)
    trace_c = Image.new("L", (W, H), 0)
    dv, dc = ImageDraw.Draw(trace_v), ImageDraw.Draw(trace_c)
    dirs = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]
    x0, y0, x1, y1 = (c + 7 * SC for c in FIELD)
    for i in range(30):
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
        drawer = dv if i % 2 == 0 else dc
        drawer.line(pts, fill=210, width=max(2, int(1.5 * SC)), joint="curve")
        drawer.ellipse([pts[-1][0] - 3 * SC, pts[-1][1] - 3 * SC,
                        pts[-1][0] + 3 * SC, pts[-1][1] + 3 * SC], fill=185)
    edge_mod = (0.12 + 0.88 * edge)[..., None]
    for layer, color in ((trace_v, VIOLET), (trace_c, CYAN)):
        core = np.asarray(layer, np.float32) / 255.0
        halo = blur(core, 4.0 * SC)
        rgb = screen(rgb, (core * 0.55 + halo * 0.50)[..., None] * color[None, None, :] * edge_mod)

    # Metal frame band.
    band, band_valid = metal_band(X, Y, d_out, d_fld, rng)
    band_cov = cover(d_out) * (1.0 - cover(d_fld))
    rgb = rgb * (1 - band_cov[..., None]) + band * band_cov[..., None]

    # Rims: violet outside, cyan hugging the field.
    rgb = screen(rgb, glow_line(d_out, -3 * SC, 3 * SC, 0.55)[:, :, None] * VIOLET[None, None, :])
    rgb = screen(rgb, glow_line(d_out, 4 * SC, 5 * SC, 0.22)[:, :, None] * VIOLET[None, None, :])
    rgb = screen(rgb, glow_line(d_fld, -2 * SC, 2.2 * SC, 0.85)[:, :, None] * CYAN[None, None, :])
    rgb = screen(rgb, glow_line(d_fld, -6 * SC, 6 * SC, 0.35)[:, :, None] * CYAN[None, None, :])

    # LED strips on the frame (top, bottom, left, right).
    for sx0, sy0, sx1, sy1 in ((470, 6, 554, 22), (470, 490, 554, 506),
                               (8, 206, 24, 306), (1000, 206, 1016, 306)):
        box = (sx0 * SC, sy0 * SC, sx1 * SC, sy1 * SC)
        housing = cover(sd_rrect(X, Y, *box, 5 * SC))
        rgb = rgb * (1 - housing[..., None] * 0.8) + METAL_DARK[None, None, :] * housing[..., None] * 0.8
        core_box = (box[0] + 4 * SC, box[1] + 4 * SC, box[2] - 4 * SC, box[3] - 4 * SC)
        core = cover(sd_rrect(X, Y, *core_box, 3 * SC))
        rgb = screen(rgb, blur(core, 3.0 * SC)[:, :, None] * CYAN[None, None, :] * 0.75)
        rgb = screen(rgb, core[:, :, None] * CYAN[None, None, :] * 1.25)

    # Corner bolts: dark metal cap, glowing violet ring, specular arc.
    for bx, by in ((33, 33), (991, 33), (33, 479), (991, 479)):
        dist = np.hypot(X - bx * SC, Y - by * SC)
        R = 23 * SC
        cap = cover(dist - R)  # signed distance: positive outside the bolt
        t = np.clip(dist / R, 0.0, 1.0)
        shade = 0.25 + 0.60 * np.sin(np.pi * t) ** 0.6
        shade *= np.where(t < 0.52, 0.72, 1.0)  # recessed centre button
        bolt = METAL_DARK[None, None, :] + (METAL_LIGHT - METAL_DARK)[None, None, :] * shade[..., None]
        bolt += (rng.random((H, W, 1)) - 0.5) * 8.0
        rgb = rgb * (1 - cap[..., None]) + bolt * cap[..., None]
        ring = glow_line(dist, 0.74 * R, 1.7 * SC, 1.0) * cap
        rgb = screen(rgb, blur(ring, 2.0 * SC)[:, :, None] * VIOLET[None, None, :] * 0.7)
        rgb = screen(rgb, ring[:, :, None] * np.array([214, 180, 255])[None, None, :])
        angle = np.arctan2(Y - by * SC, X - bx * SC)
        spec = cap * (t > 0.55) * (t < 0.92) * np.exp(-((angle + 2.4) / 0.5) ** 2) * 0.5
        rgb = screen(rgb, spec[:, :, None] * np.array([255, 255, 255])[None, None, :])

    # Opaque canvas: keep the outside of the rounded frame calm and dark.
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8)).convert("RGBA")


# Domino shell --------------------------------------------------------------
def make_shell():
    W, H = 256 * SC, 512 * SC
    rng = np.random.default_rng(SEED + 7)
    X, Y = np.meshgrid(np.arange(W, dtype=float), np.arange(H, dtype=float))

    OUTER = (2 * SC, 2 * SC, 254 * SC, 510 * SC)
    FACE_T = (18 * SC, 18 * SC, 238 * SC, 240 * SC)
    FACE_B = (18 * SC, 272 * SC, 238 * SC, 494 * SC)
    d_out = sd_rrect(X, Y, *OUTER, 34 * SC)
    d_top = sd_rrect(X, Y, *FACE_T, 26 * SC)
    d_bot = sd_rrect(X, Y, *FACE_B, 26 * SC)

    rgb = np.empty((H, W, 3), float)
    rgb[:] = OUTER_BG

    # Light alloy frame: the contrast against the dark nebula board is the point.
    d_faces = np.minimum(d_top, d_bot)
    band, _ = metal_band(X, Y, d_out, d_faces, rng, perforate=False,
                         dark=ALLOY_DARK, light=ALLOY_LIGHT)
    band_cov = cover(d_out) * (1.0 - cover(d_faces))
    rgb = rgb * (1 - band_cov[..., None]) + band * band_cov[..., None]

    # Divider bar: same alloy, carrying the only violet accent of the shell.
    div = cover(d_out) * ((Y > 239 * SC) & (Y < 273 * SC))
    u = np.clip((Y - 240 * SC) / (32 * SC), 0.0, 1.0)
    shade = 0.24 + 0.58 * np.sin(np.pi * u) ** 0.7
    bar = ALLOY_DARK[None, None, :] + (ALLOY_LIGHT - ALLOY_DARK)[None, None, :] * shade[..., None]
    bar += (rng.random((H, W, 1)) - 0.5) * 7.0
    rgb = rgb * (1 - div[..., None]) + bar * div[..., None]
    sep = np.exp(-((Y - 241 * SC) / (2.0 * SC)) ** 2) + np.exp(-((Y - 271 * SC) / (2.0 * SC)) ** 2)
    rgb *= (1 - sep * 0.5 * cover(d_out))[..., None]
    core = np.exp(-((Y - 256 * SC) / (1.8 * SC)) ** 2) * cover(d_out)
    rgb = screen(rgb, blur(core, 2.5 * SC)[:, :, None] * VIOLET[None, None, :] * 0.55)
    rgb = screen(rgb, core[:, :, None] * np.array([196, 156, 255])[None, None, :])

    # Matte black faces on top: no nebula, no stars, no glow - pips stay king.
    for face, d_face, seed in ((FACE_T, d_top, SEED + 7), (FACE_B, d_bot, SEED + 21)):
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

    # Outline: recessed dark edge plus a faint violet hairline to tie the pair.
    rgb *= (1 - glow_line(d_out, -3 * SC, 3.0 * SC, 0.50))[..., None]
    rgb = screen(rgb, glow_line(d_out, 0.0, 2.2 * SC, 0.30)[:, :, None] * VIOLET[None, None, :])

    alpha = cover(d_out)
    out = np.dstack([np.clip(rgb, 0, 255), np.clip(alpha * 255.0, 0, 255)]).astype(np.uint8)
    return Image.fromarray(out)


def main():
    ORIGINALS.mkdir(parents=True, exist_ok=True)
    board = make_board()
    shell = make_shell()
    board.save(ORIGINALS / "board_nebula.png")
    shell.save(ORIGINALS / "domino_nebula.png")
    print(f"board_nebula.png {board.size}  domino_nebula.png {shell.size}")


if __name__ == "__main__":
    main()
