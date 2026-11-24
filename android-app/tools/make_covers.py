"""Generates lock-screen / notification artwork for every episode (512x512 PNG).

The Compose UI draws its covers procedurally; these PNGs mirror the same palette and
monogram so the notification artwork looks like part of the app.
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
FONT = os.path.join(RES, "font", "fraunces_semibold.ttf")

PALETTE = [
    (242, 176, 75),
    (123, 216, 192),
    (232, 131, 107),
    (195, 166, 255),
    (245, 208, 111),
    (143, 184, 255),
    (233, 160, 180),
    (154, 212, 138),
    (217, 166, 107),
    (169, 180, 255),
]

SIZE = 512


def stable_hash(text):
    value = 0
    for ch in text:
        value = (value * 31 + ord(ch)) & 0x7FFFFFFF
    return value


def monogram(title):
    words = [w for w in title.replace("’", "'").split() if w[:1].isalpha()]
    letters = "".join(w[0] for w in words[:2]).upper()
    return letters or title[:1].upper()


def build(episode, index):
    accent = PALETTE[stable_hash(episode["id"]) % len(PALETTE)]
    img = Image.new("RGB", (SIZE, SIZE), (12, 10, 11))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            d = min(1.0, math.hypot(x - SIZE * 0.28, y - SIZE * 0.18) / (SIZE * 1.05)) ** 1.35
            base = (26, 20, 20)
            px[x, y] = tuple(
                int(accent[i] * 0.22 * (1 - d) + base[i] * (1 - d * 0.25)) for i in range(3)
            )

    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [SIZE * 0.06, SIZE * 0.06, SIZE * 0.94, SIZE * 0.94],
        fill=accent + (34,),
    )
    img = Image.alpha_composite(img.convert("RGBA"), glow.filter(ImageFilter.GaussianBlur(70)))

    arcs = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ad = ImageDraw.Draw(arcs)
    for r in (0.30, 0.36, 0.42):
        rr = SIZE * r
        ad.arc(
            [SIZE / 2 - rr, SIZE / 2 - rr, SIZE / 2 + rr, SIZE / 2 + rr],
            start=-52,
            end=52,
            fill=accent + (40,),
            width=2,
        )
    img = Image.alpha_composite(img, arcs)

    font = ImageFont.truetype(FONT, int(SIZE * 0.46))
    text = f"{index + 1:02d}"
    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    box = d.textbbox((0, 0), text, font=font)
    tw, th = box[2] - box[0], box[3] - box[1]
    pos = ((SIZE - tw) / 2 - box[0], (SIZE - th) / 2 - box[1] - SIZE * 0.02)
    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).text(pos, text, font=font, fill=255)

    grad = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    for y in range(SIZE):
        t = y / SIZE
        gd.line(
            [(0, y), (SIZE, y)],
            fill=(int(accent[0] + (255 - accent[0]) * 0.45 * t),
                  int(accent[1] + (255 - accent[1]) * 0.35 * t),
                  int(accent[2] + (255 - accent[2]) * 0.30 * t),
                  255),
        )
    grad.putalpha(mask)
    img = Image.alpha_composite(img, grad)
    return img.convert("RGB")


def main():
    episodes = json.load(open(os.path.join(ASSETS, "content", "episodes.json"), encoding="utf-8"))
    rank = {"beginner": 0, "intermediate": 1, "advanced": 2}
    episodes.sort(key=lambda e: (rank.get(e["level"].lower(), 1), e["duration"]))
    out_dir = os.path.join(ASSETS, "covers")
    os.makedirs(out_dir, exist_ok=True)
    for index, episode in enumerate(episodes):
        build(episode, index).save(os.path.join(out_dir, f"{episode['id']}.png"), optimize=True)
    print(f"wrote {len(episodes)} covers")


if __name__ == "__main__":
    main()
