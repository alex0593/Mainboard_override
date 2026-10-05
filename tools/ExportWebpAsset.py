#!/usr/bin/env python3
"""Lossy WebP export; mirrors tools/PrepareSkinAsset.java but writes WebP.

Usage: ExportWebpAsset.py source destination width height [quality]
"""
import sys
from pathlib import Path

from PIL import Image


def main() -> None:
    if len(sys.argv) not in (5, 6):
        raise SystemExit("source destination width height [quality]")
    source, destination = Path(sys.argv[1]), Path(sys.argv[2])
    width, height = int(sys.argv[3]), int(sys.argv[4])
    quality = int(sys.argv[5]) if len(sys.argv) == 6 else 82
    with Image.open(source) as img:
        img.load()
        src = img.convert("RGBA") if "A" in img.getbands() else img.convert("RGB")
        out = src.resize((width, height), Image.LANCZOS)
        destination.parent.mkdir(parents=True, exist_ok=True)
        out.save(destination, "WEBP", quality=quality, method=6)
    print(
        f"{destination}: {width}x{height}, {destination.stat().st_size:,} bytes"
    )


if __name__ == "__main__":
    main()
