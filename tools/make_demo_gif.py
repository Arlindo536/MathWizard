#!/usr/bin/env python3
"""Assemble the frames rendered by ScreenshotTest.demoFrames into docs/demo.gif.

Usage: ./gradlew recordRoborazziDebug --tests '*demoFrames' && python3 tools/make_demo_gif.py
(needs Pillow). Frames are scaled to 360 px wide and share one palette to keep the file small.
"""
import glob
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
frames = sorted(glob.glob(str(ROOT / "app/build/demo/frame_*.png")))
if not frames:
    sys.exit("No frames: run ./gradlew recordRoborazziDebug --tests '*demoFrames' first")
width = 360
images = []
for f in frames:
    im = Image.open(f).convert("RGB")
    im = im.resize((width, round(im.height * width / im.width)), Image.NEAREST)
    images.append(im)
palette = images[len(images) // 2].quantize(colors=128, method=Image.Quantize.MEDIANCUT)
quantised = [im.quantize(palette=palette, dither=Image.Dither.NONE) for im in images]
out = ROOT / "docs/demo.gif"
quantised[0].save(out, save_all=True, append_images=quantised[1:], duration=90, loop=0, optimize=True)
print(f"{len(frames)} frames -> {out} ({out.stat().st_size // 1024} KB)")
