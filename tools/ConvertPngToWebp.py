#!/usr/bin/env python3
"""Convierte a WebP los PNG estáticos de res/, en sitio y sin cambiar dimensiones.

La calidad no se adivina, se mide por imagen:

  1. Se codifica en WebP con pérdida (--quality, por defecto 92).
  2. Se compara con el original por PSNR enmascarado por alpha. Los píxeles totalmente
     transparentes quedan fuera de la medida: ahí el codificador escribe lo que quiere y
     ensuciaría el número.
     - PSNR >= --min-psnr (por defecto 38 dB) -> se acepta la codificación con pérdida.
     - Si no, se reintenta sin pérdida: en arte con degradados oscuros la fidelidad manda
       sobre el tamaño.
  3. Si ni así el WebP es sensiblemente más pequeño que el PNG (--min-saving), el PNG se queda.

Nunca toca lo que no se puede tocar: nine-patch (`.9.png`) ni imágenes animadas.

Los PNG son ficheros versionados, así que la conversión es reversible con
`git checkout -- <ruta>`; no hace falta duplicar originales.

Uso:
    python3 tools/ConvertPngToWebp.py --dry-run     # solo informa
    python3 tools/ConvertPngToWebp.py               # convierte en sitio
"""
from __future__ import annotations

import argparse
import io
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

Image.MAX_IMAGE_PIXELS = None

ROOT = Path(__file__).resolve().parent.parent
DIRS = (ROOT / "app/src/main/res/drawable", ROOT / "app/src/main/res/drawable-nodpi")


def masked_psnr(orig: np.ndarray, dec: np.ndarray, mask: np.ndarray) -> float:
    a, b = orig[mask].astype(np.float64), dec[mask].astype(np.float64)
    if a.size == 0:
        return float("inf")
    mse = float(np.mean((a - b) ** 2))
    return float("inf") if mse == 0 else 10 * math.log10(255.0 * 255.0 / mse)


def encode(im: Image.Image, **kw) -> bytes:
    buf = io.BytesIO()
    im.save(buf, "WEBP", **kw)
    return buf.getvalue()


def decode(data: bytes, mode: str) -> np.ndarray:
    return np.array(Image.open(io.BytesIO(data)).convert(mode))


def load_static_png(path: Path) -> Image.Image | None:
    """None si la imagen no es un PNG estático convertible (animado, nine-patch)."""
    if ".9." in path.name:
        return None
    im = Image.open(path)
    if getattr(im, "is_animated", False):
        return None
    # Se normaliza la paleta y el mapa de bits para que Pillow guarde WebP sin sorpresas.
    if im.mode in ("P", "PA", "LA") or "transparency" in im.info:
        return im.convert("RGBA")
    if im.mode == "1":
        return im.convert("L")
    return im


def convert(path: Path, quality: int, min_psnr: float, min_saving: float) -> dict:
    """Devuelve el veredicto para un PNG, sin escribir nada."""
    png_bytes = path.stat().st_size
    im = load_static_png(path)
    if im is None:
        return {"path": path, "action": "skip", "why": "no es PNG estático", "png": png_bytes, "webp": png_bytes}

    arr = np.array(im)
    rgb, has_alpha = arr[..., :3], im.mode == "RGBA"
    mask = arr[..., 3] > 0 if has_alpha else np.ones(arr.shape[:2], dtype=bool)

    lossy = encode(im, quality=quality, method=6)
    psnr = masked_psnr(rgb, decode(lossy, im.mode)[..., :3], mask)

    if psnr >= min_psnr:
        best, why = lossy, f"q{quality} ({psnr:.1f} dB)"
    else:
        # Umbral no superado: se prueba sin pérdida, que en WebP todavía gana mucho a PNG.
        lossless = encode(im, lossless=True, method=4)
        if len(lossless) < png_bytes:
            best, why = lossless, f"sin pérdida (q{quality} daba {psnr:.1f} dB)"
        else:
            best, why = lossy, f"q{quality} ({psnr:.1f} dB, el menor posible)"

    if len(best) >= png_bytes * (1.0 - min_saving):
        return {"path": path, "action": "keep", "why": "el WebP no compensa", "png": png_bytes, "webp": len(best)}

    return {"path": path, "action": "convert", "why": why, "png": png_bytes, "webp": len(best), "data": best}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dry-run", action="store_true", help="informa sin escribir nada")
    ap.add_argument("--quality", type=int, default=92, help="calidad WebP con pérdida (def. 92)")
    ap.add_argument("--min-psnr", type=float, default=38.0, help="PSNR mínimo aceptable en dB (def. 38)")
    ap.add_argument("--min-saving", type=float, default=0.05, help="ahorro mínimo exigido, 0-1 (def. 0.05)")
    args = ap.parse_args()

    targets = sorted(
        p for d in DIRS if d.is_dir() for p in d.glob("*.png")
    )
    if not targets:
        print("No hay PNG que convertir.")
        return 0

    done, kept, skipped, failed = [], [], [], []
    for path in targets:
        try:
            r = convert(path, args.quality, args.min_psnr, args.min_saving)
        except Exception as exc:  # un fichero raro no puede abortar la conversión
            failed.append((path, exc))
            continue
        if r["action"] == "convert":
            if not args.dry_run:
                path.with_suffix(".webp").write_bytes(r["data"])
                path.unlink()
            done.append(r)
        elif r["action"] == "keep":
            kept.append(r)
        else:
            skipped.append(r)
        print(f"  {r['action']:<7} {path.name:<44} {r['png']/1048576:6.2f} -> {r['webp']/1048576:6.2f} MB  {r['why']}")

    saved = sum(r["png"] - r["webp"] for r in done)
    png_total = sum(r["png"] for r in done) + sum(r["png"] for r in kept) + sum(r["png"] for r in skipped)
    print()
    print(f"{'[dry-run] ' if args.dry_run else ''}convertidos: {len(done)}   se quedan PNG: {len(kept)}   omitidos: {len(skipped)}")
    if failed:
        print(f"fallos: {len(failed)}")
        for path, exc in failed:
            print(f"  {path.name}: {exc}")
    if done:
        print(f"ahorro: {saved/1048576:.1f} MB  ({saved/png_total*100:.0f}% de los {png_total/1048576:.1f} MB revisados)")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
