#!/usr/bin/env python3
"""Render every sprite defined in Sprites.kt to one PNG sheet, for checking art by eye.

Usage: python3 tools/preview_sprites.py sheet.png   (needs Pillow)
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw

SOURCE = Path(__file__).resolve().parent.parent / "app/src/main/java/com/arlindodishi/mathwizard/ui/pixel/Sprites.kt"
SLOTS = {"outline": "K", "base": "1", "light": "2", "shadow": "3", "eye": "E", "pupil": "P", "accent": "A", "white": "W"}
DEFAULTS = {"P": 0xFF1A0D10, "W": 0xFFFFF6E0}


def argb(value):
    v = int(value, 16) if isinstance(value, str) else value
    return "#%06x" % (v & 0xFFFFFF)


def parse(text):
    templates = {name: [l.strip() for l in body.strip().splitlines() if l.strip()]
                 for name, body in re.findall(r'private val (\w+) = template\(\s*"""(.*?)"""', text, re.S)}
    sprites = []
    for label, tpl, args in re.findall(r'"(\w+)" to sprite\((\w+), Palette\(([^)]*)\)\)', text):
        colors = dict(DEFAULTS)
        for key, value in re.findall(r'(\w+) = (0x[0-9A-Fa-f]+)', args):
            colors[SLOTS[key]] = int(value, 16)
        sprites.append((label, templates[tpl], colors))
    for label, tpl, args in re.findall(r'(?:val |")(\w+)"? (?:=|to) icon\((\w+), ([^)]*)\)', text):
        positional = [a.strip() for a in args.split(",") if "=" not in a and a.strip()]
        named = dict(re.findall(r'(\w+) = (0x[0-9A-Fa-f]+)', args))
        colors = {"K": 0xFF1A0D10, "1": int(positional[0], 16), "2": int(positional[1], 16), "3": int(positional[2], 16),
                  "E": int(named.get("eye", "0xFFFFF6E0"), 16), "A": int(named.get("accent", "0xFFFDE68A"), 16), **DEFAULTS}
        sprites.append((label, templates[tpl], colors))
    wizard = re.search(r'val wizard: PixelSprite = sprite\(\s*WIZARD,\s*Palette\(([^)]*)\)', text, re.S)
    if wizard:
        colors = dict(DEFAULTS)
        for key, value in re.findall(r'(\w+) = (0x[0-9A-Fa-f]+)', wizard.group(1)):
            colors[SLOTS[key]] = int(value, 16)
        sprites.append(("wizard", templates["WIZARD"], colors))
    return sprites


def main(out):
    sprites = parse(SOURCE.read_text())
    scale, cols = 6, 8
    cell = 24 * scale + 24
    rows = (len(sprites) + cols - 1) // cols
    img = Image.new("RGB", (cols * cell, rows * (cell + 12)), "#120b24")
    draw = ImageDraw.Draw(img)
    for n, (label, grid, colors) in enumerate(sprites):
        size = len(grid)
        ox = (n % cols) * cell + 12 + (24 - size) * scale // 2
        oy = (n // cols) * (cell + 12) + 12 + (24 - size) * scale // 2
        for y, row in enumerate(grid):
            for x, c in enumerate(row):
                if c != ".":
                    draw.rectangle([ox + x * scale, oy + y * scale, ox + (x + 1) * scale - 1, oy + (y + 1) * scale - 1], fill=argb(colors[c]))
        draw.text(((n % cols) * cell + 6, (n // cols) * (cell + 12) + cell - 6), label, fill="#c9c2ff")
    img.save(out)
    print(f"{len(sprites)} sprites -> {out}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "sprites.png")
