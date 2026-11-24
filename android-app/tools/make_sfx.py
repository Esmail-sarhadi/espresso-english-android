"""Generate a tiny set of royalty-free UI sounds for the app.

Pure standard library: short sine/triangle tones with an exponential envelope.
The files are deliberately quiet and short so they never mask spoken audio.
"""
import math
import os
import struct
import wave

BASE = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(BASE, "app", "src", "main", "assets", "sfx")
RATE = 22050


def envelope(i, n):
    t = i / n
    return math.sin(math.pi * min(1.0, t * 8)) * ((1.0 - t) ** 2.2)


def tone(name, notes, duration=0.20, volume=0.22):
    path = os.path.join(OUT, name + ".wav")
    n = int(RATE * duration)
    frames = bytearray()
    for i in range(n):
        t = i / RATE
        value = 0.0
        for freq, delay, gain in notes:
            local = t - delay
            if 0 <= local <= duration - delay:
                # A soft triangle-ish wave: fewer harmonics than a square wave.
                phase = (freq * local) % 1.0
                wave_value = 2.0 * abs(2.0 * phase - 1.0) - 1.0
                value += wave_value * gain * envelope(int(local * RATE), max(1, n - int(delay * RATE)))
        sample = int(max(-1.0, min(1.0, value * volume)) * 32767)
        frames += struct.pack("<h", sample)
    with wave.open(path, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(frames)
    print(name, len(frames) // 2, "samples")


os.makedirs(OUT, exist_ok=True)
tone("correct", [(880.0, 0.0, 1.0), (1320.0, 0.07, 0.65)], 0.24)
tone("wrong", [(220.0, 0.0, 1.0), (196.0, 0.06, 0.7)], 0.24, 0.18)
tone("tap", [(660.0, 0.0, 1.0)], 0.09, 0.12)
tone("save", [(784.0, 0.0, 1.0), (1046.0, 0.05, 0.7)], 0.20)
tone("complete", [(523.0, 0.0, 1.0), (659.0, 0.10, 1.0), (784.0, 0.20, 1.0), (1046.0, 0.30, 1.0)], 0.58, 0.19)
print("done")
