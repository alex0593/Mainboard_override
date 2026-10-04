#!/usr/bin/env python3
"""Procedural generator for the premium-skin FX loops and the ambient motes loop.

Every premium skin ships a transparent animated WebP that plays over its static
artwork (board or domino). The single ambient sheet carries neutral motes that
CircuitBackground tints per scenario accent at runtime. All loops are rendered
analytically here so the assets in assets/skins/originals/ are reproducible
byte-for-byte from this file, and each one is also copied to the runtime export
place: app/src/main/res/drawable-nodpi/.

Design rules honoured here (docs/GDD.md + assets/skins/README.md):
  * one loop per premium id, matching SkinFxStyle in ui/PremiumSkinFx.kt;
  * accents mirror dominoPipColor in ui/DominoImage.kt;
  * 14 frames at 80 ms with transparency; every loop tiles seamlessly.
"""

from pathlib import Path

import numpy as np
from PIL import Image

FRAMES = 14
DURATION_MS = 80
SIZE = 512

HERE = Path(__file__).resolve().parent
ORIGINALS = HERE / "originals"
EXPORT = HERE.parent.parent / "app" / "src" / "main" / "res" / "drawable-nodpi"

# id -> (accent RGB, style); mirrors ui/PremiumSkinFx.kt.
SKINS = {
    "copper":   ((255, 209, 161), "sweep"),
    "aurora":   ((255, 193, 7),   "drift"),
    "titanium": ((232, 241, 255), "sweep"),
    "jade":     ((255, 212, 59),  "pulse"),
    "ruby":     ((255, 179, 193), "pulse"),
    "sapphire": ((191, 231, 255), "twinkle"),
    "amber":    ((255, 171, 61),  "sweep"),
    "amethyst": ((217, 179, 255), "twinkle"),
    # Accents mirror the pip colors expected by DominoResourcesTest.
    "biolum":   ((111, 255, 224), "pulse"),
    "prisma":   ((255, 0, 255),   "twinkle"),
    "quantum":  ((158, 181, 255), "drift"),
}


def grid():
    y, x = np.meshgrid(np.arange(SIZE, dtype=float), np.arange(SIZE, dtype=float), indexing="ij")
    return x, y


X, Y = grid()


def alpha_sweep(t, seed):
    rng = np.random.default_rng(seed)
    # Diagonal coordinate over the frame; the band sweeps it end to end.
    angle = np.deg2rad(16.0)
    u = X * np.cos(angle) + Y * np.sin(angle)
    span = SIZE * (abs(np.cos(angle)) + abs(np.sin(angle)))
    center = -0.35 * span + span * 1.7 * t
    band = np.exp(-((u - center) / (SIZE * 0.16)) ** 2) * 0.26
    sparkle = np.clip(rng.random((SIZE, SIZE)) - 0.995, 0, None) * 8.0
    return np.clip(band + sparkle, 0, 1)


def alpha_pulse(t, seed):
    a = 0.08 + 0.15 * (0.5 + 0.5 * np.sin(2 * np.pi * t))
    cx = SIZE / 2 + (np.random.default_rng(seed).random() - 0.5) * 40
    cy = SIZE / 2 + (np.random.default_rng(seed + 1).random() - 0.5) * 40
    falloff = np.exp(-((X - cx) ** 2 + (Y - cy) ** 2) / (2 * (SIZE * 0.42) ** 2))
    return np.clip(a * falloff * 2.4, 0, 1)


def alpha_drift(t, seed):
    field = np.zeros((SIZE, SIZE), np.float32)
    for i in range(2):
        cy = ((t + i * 0.5) % 1.0) * SIZE * 1.5 - SIZE * 0.25
        field += np.exp(-((Y - cy) / (SIZE * 0.13)) ** 2) * 0.17
    return np.clip(field, 0, 1)


def alpha_twinkle(t, seed):
    rng = np.random.default_rng(seed)
    points = [(0.2, 0.3), (0.75, 0.2), (0.6, 0.62), (0.3, 0.8), (0.85, 0.75), (0.52, 0.45)]
    field = np.zeros((SIZE, SIZE), np.float32)
    for k, (px, py) in enumerate(points):
        local = (t + k * 0.37) % 1.0
        a = (local / 0.25) if local < 0.25 else max(0.0, 1.0 - (local - 0.25) / 0.75)
        a = a ** 2
        x0, y0 = px * SIZE, py * SIZE
        r = 2.0 + 4.0 * a
        dx, dy = X - x0, Y - y0
        dot = np.exp(-(dx * dx + dy * dy) / (2 * (r * 0.6) ** 2)) * a
        cross = (np.exp(-(dy * dy) / 1.2) * np.exp(-(dx * dx) / (2 * (r * 1.6) ** 2)) +
                 np.exp(-(dx * dx) / 1.2) * np.exp(-(dy * dy) / (2 * (r * 1.6) ** 2))) * a * 0.6
        field += dot + cross
    return np.clip(field, 0, 1)


def alpha_motes(t, seed):
    rng = np.random.default_rng(seed)
    field = np.zeros((SIZE, SIZE), np.float32)
    for k in range(42):
        x0 = rng.random() * SIZE
        y0 = rng.random() * SIZE
        speed = 0.10 + rng.random() * 0.16
        sway = 8.0 + rng.random() * 20.0
        r = 1.2 + rng.random() * 2.6
        base = 0.10 + rng.random() * 0.22
        y = (y0 - t * speed * SIZE) % SIZE
        x = (x0 + sway * np.sin(2 * np.pi * t + k)) % SIZE
        dx, dy = X - x, Y - y
        tw = 0.5 + 0.5 * np.sin(2 * np.pi * (t * 1.0) + k * 1.7)
        field += np.exp(-(dx * dx + dy * dy) / (2 * r * r)) * base * tw
    return np.clip(field, 0, 1)


ALPHA = {"sweep": alpha_sweep, "pulse": alpha_pulse, "drift": alpha_drift, "twinkle": alpha_twinkle}


def render_loop(accent, style, seed):
    frames = []
    for i in range(FRAMES):
        t = i / FRAMES
        a = ALPHA[style](t, seed)
        rgba = np.zeros((SIZE, SIZE, 4), np.uint8)
        rgba[..., 0], rgba[..., 1], rgba[..., 2] = accent
        rgba[..., 3] = (a * 255).astype(np.uint8)
        frames.append(Image.fromarray(rgba, "RGBA"))
    return frames


def render_motes_loop():
    frames = []
    for i in range(FRAMES):
        t = i / FRAMES
        a = alpha_motes(t, seed=99)
        rgba = np.zeros((SIZE, SIZE, 4), np.uint8)
        rgba[..., 0], rgba[..., 1], rgba[..., 2] = (220, 240, 255)
        rgba[..., 3] = (a * 255).astype(np.uint8)
        frames.append(Image.fromarray(rgba, "RGBA"))
    return frames


def main():
    ORIGINALS.mkdir(parents=True, exist_ok=True)
    EXPORT.mkdir(parents=True, exist_ok=True)
    outputs = []
    for skin, (accent, style) in SKINS.items():
        # Stable seed per skin (hash() is salted per process, so it would not be reproducible).
        seed = sum(ord(c) for c in skin) * 13
        frames = render_loop(accent, style, seed=seed)
        path = ORIGINALS / f"fx_{skin}.webp"
        frames[0].save(path, save_all=True, append_images=frames[1:],
                       duration=DURATION_MS, loop=0, quality=80, method=6)
        export = EXPORT / f"fx_{skin}.webp"
        export.write_bytes(path.read_bytes())
        outputs.append(export)
    frames = render_motes_loop()
    path = ORIGINALS / "ambient_motes.webp"
    frames[0].save(path, save_all=True, append_images=frames[1:],
                   duration=DURATION_MS, loop=0, quality=80, method=6)
    export = EXPORT / "ambient_motes.webp"
    export.write_bytes(path.read_bytes())
    outputs.append(export)
    for export in outputs:
        print(f"{export.name}: {export.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
