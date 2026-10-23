#!/usr/bin/env python3
"""Generate the game's chiptune sound effects and music loop as 16-bit mono WAV files.

All sounds are synthesised here (square, triangle and noise waves), so they are original to
this project and free of third-party rights. Usage: python3 tools/gen_sounds.py [out_dir]
(default: app/src/main/res/raw). Standard library only.
"""
import math
import random
import struct
import sys
import wave
from pathlib import Path

RATE = 22050
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def freq(note):
    """'A4' style note name to Hz (sharps with #)."""
    names = {"C": -9, "C#": -8, "D": -7, "D#": -6, "E": -5, "F": -4, "F#": -3, "G": -2, "G#": -1, "A": 0, "A#": 1, "B": 2}
    name, octave = note[:-1], int(note[-1])
    return 440.0 * 2 ** ((names[name] + (octave - 4) * 12) / 12)


def square(f, t, duty=0.5):
    return 1.0 if (f * t) % 1.0 < duty else -1.0


def triangle(f, t):
    p = (f * t) % 1.0
    return 4 * p - 1 if p < 0.5 else 3 - 4 * p


_noise = random.Random(1)


def noise(_f, _t):
    return _noise.uniform(-1, 1)


def tone(f_start, f_end, dur, wave_fn=square, vol=0.5, attack=0.005, release=0.04, duty=0.5):
    n = int(RATE * dur)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = f_start + (f_end - f_start) * (i / n)
        phase += f / RATE
        if wave_fn is square:
            s = 1.0 if phase % 1.0 < duty else -1.0
        elif wave_fn is triangle:
            p = phase % 1.0
            s = 4 * p - 1 if p < 0.5 else 3 - 4 * p
        else:
            s = wave_fn(f, t)
        env = min(1.0, t / attack) if attack > 0 else 1.0
        env *= min(1.0, (dur - t) / release) if release > 0 else 1.0
        out.append(s * vol * max(0.0, env))
    return out


def seq(notes, step, **kw):
    out = []
    for n in notes:
        if n is None:
            out += [0.0] * int(RATE * step)
        else:
            out += tone(freq(n), freq(n), step, **kw)
    return out


def mix(*tracks):
    length = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(length)]


def write(name, samples, peak=0.8):
    top = max(1e-9, max(abs(s) for s in samples))
    scale = peak / top if top > peak else 1.0
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * scale)) * 32767)) for s in samples))
    print(f"{name}.wav  {len(samples) / RATE:.2f}s")


def main():
    write("sfx_click", tone(880, 990, 0.035, vol=0.35, duty=0.25))
    write("sfx_hit", seq(["C6", "E6", "G6"], 0.045, vol=0.45, duty=0.25))
    write("sfx_critical", mix(seq(["E6", "G6", "B6", "E7"], 0.04, vol=0.45, duty=0.125), [0.0] * int(RATE * 0.08) + tone(2600, 3400, 0.09, vol=0.15)))
    write("sfx_miss", mix(tone(392, 131, 0.24, vol=0.5, duty=0.5), tone(0, 0, 0.12, wave_fn=noise, vol=0.25, release=0.1)))
    write("sfx_shield", seq(["A5", "E5"], 0.06, vol=0.4, duty=0.5))
    write("sfx_floor", seq(["G5", "C6", "E6", "G6", None, "C7"], 0.07, vol=0.45, duty=0.25))
    write("sfx_boss", mix(seq(["C3", "C3", "F#2"], 0.18, wave_fn=square, vol=0.4, duty=0.5), seq(["C2", "C2", "F#1"], 0.18, wave_fn=triangle, vol=0.6)))
    write("sfx_relic", seq(["E6", "B6", "E7"], 0.07, wave_fn=triangle, vol=0.6))
    write("sfx_buy", seq(["B5", "E6"], 0.06, vol=0.4, duty=0.125) + tone(freq("E6"), freq("E6"), 0.12, wave_fn=triangle, vol=0.4, release=0.1))
    write("sfx_gameover", seq(["G4", "F4", "D#4", "D4", "C4"], 0.16, vol=0.45, duty=0.5, release=0.06) + tone(freq("C3"), freq("C3"), 0.4, wave_fn=triangle, vol=0.5, release=0.3))

    # Music: 8 bars in C minor at 112 bpm, eighth-note arpeggio bass + a simple square lead.
    step = 60 / 112 / 2
    chords = [["C3", "G3", "D#4", "G3"], ["G#2", "D#3", "C4", "D#3"], ["D#3", "A#3", "G4", "A#3"], ["A#2", "F3", "D4", "F3"]]
    bass = []
    for bar in range(8):
        bass += seq(chords[bar % 4] * 2, step, wave_fn=triangle, vol=0.55, release=0.03)
    lead_notes = [
        "G4", None, "D#5", "D5", "C5", None, "G4", None,
        "G#4", None, "C5", "D#5", "D5", None, "C5", None,
        "D#5", None, "G5", "F5", "D#5", None, "D5", None,
        "D5", "D#5", "F5", None, "D5", None, "A#4", None,
        "G4", None, "D#5", "D5", "C5", None, "D#5", "F5",
        "G5", None, "G#5", "G5", "F5", None, "D#5", None,
        "D#5", "D5", "C5", None, "D5", "D#5", "F5", None,
        "D5", None, "A#4", None, "C5", None, None, None,
    ]
    lead = seq(lead_notes, step, vol=0.22, duty=0.25, release=0.05)
    hats = []
    for i in range(64):
        hats += tone(0, 0, 0.02, wave_fn=noise, vol=0.08 if i % 2 else 0.14, release=0.015) + [0.0] * (int(RATE * step) - int(RATE * 0.02))
    write("music_spire", mix(bass, lead, hats), peak=0.6)


if __name__ == "__main__":
    main()
