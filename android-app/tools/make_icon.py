"""Generates the Espresso English launcher icon set (adaptive + legacy PNGs)."""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res")
FONT = os.path.join(BASE, "font", "fraunces_semibold.ttf")
S = 1024


def radial_bg(size, inner, outer):
    img = Image.new("RGB", (size, size), outer)
    px = img.load()
    cx, cy = size * 0.30, size * 0.24
    maxd = math.hypot(size, size) * 0.92
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - cx, y - cy) / maxd
            d = min(1.0, d) ** 1.25
            px[x, y] = tuple(int(inner[i] + (outer[i] - inner[i]) * d) for i in range(3))
    return img


def draw_waves(draw, size, color):
    cx, cy = size * 0.5, size * 0.5
    for i, r in enumerate((0.30, 0.352, 0.404)):
        rr = size * r
        bbox = [cx - rr, cy - rr, cx + rr, cy + rr]
        draw.arc(bbox, start=-52, end=52, fill=color, width=max(2, int(size * 0.006)))


def build_master():
    bg = radial_bg(S, (58, 33, 21), (11, 9, 10)).convert("RGBA")

    # soft warm glow behind the monogram
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([S * 0.10, S * 0.12, S * 0.90, S * 0.92], fill=(242, 176, 75, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.11))
    bg = Image.alpha_composite(bg, glow)

    waves = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw_waves(ImageDraw.Draw(waves), S, (242, 176, 75, 46))
    bg = Image.alpha_composite(bg, waves)

    # monogram
    font = ImageFont.truetype(FONT, int(S * 0.655))
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    text = "e"
    box = d.textbbox((0, 0), text, font=font)
    tw, th = box[2] - box[0], box[3] - box[1]
    pos = ((S - tw) / 2 - box[0], (S - th) / 2 - box[1] - S * 0.015)

    # gradient fill for the glyph
    grad = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gdr = ImageDraw.Draw(grad)
    for y in range(S):
        t = y / S
        col = (
            int(255 - 30 * t),
            int(206 - 70 * t),
            int(120 - 40 * t),
            255,
        )
        gdr.line([(0, y), (S, y)], fill=col)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).text(pos, text, font=font, fill=255)
    grad.putalpha(mask)
    layer = Image.alpha_composite(layer, grad)

    # shadow of the glyph
    shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sm = mask.filter(ImageFilter.GaussianBlur(S * 0.02))
    shadow.paste((0, 0, 0, 150), (0, int(S * 0.012)), sm)
    bg = Image.alpha_composite(bg, shadow)
    bg = Image.alpha_composite(bg, layer)

    # crema dot accent
    dot = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dd = ImageDraw.Draw(dot)
    r = S * 0.028
    cx, cy = S * 0.735, S * 0.268
    dd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(123, 216, 192, 235))
    bg = Image.alpha_composite(bg, dot)
    return bg


def round_mask(size, radius_ratio=0.5):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).ellipse([0, 0, size, size], fill=255)
    return m


def squircle_mask(size, radius_ratio=0.22):
    m = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(m).rounded_rectangle(
        [0, 0, size * 4 - 1, size * 4 - 1], radius=int(size * 4 * radius_ratio), fill=255
    )
    return m.resize((size, size), Image.LANCZOS)


def main():
    master = build_master()
    master.save(os.path.join(BASE, "drawable-nodpi", "brand_mark.png"))

    for dpi, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        out = master.resize((px, px), Image.LANCZOS)
        out.convert("RGB").save(os.path.join(BASE, f"mipmap-{dpi}", "ic_launcher.png"), optimize=True)
        rnd = out.copy()
        rnd.putalpha(round_mask(px))
        rnd.save(os.path.join(BASE, f"mipmap-{dpi}", "ic_launcher_round.png"), optimize=True)

    # adaptive foreground: 108dp canvas, art occupies the inner 72dp (66%)
    fg_size = 432
    inner = int(fg_size * 0.72)
    fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    art = master.resize((inner, inner), Image.LANCZOS)
    # strip the background: keep only the glyph + waves by keying out the flat backdrop
    bg_only = radial_bg(inner, (58, 33, 21), (11, 9, 10)).convert("RGBA")
    diff = Image.new("RGBA", (inner, inner), (0, 0, 0, 0))
    ap, bp, dp = art.load(), bg_only.load(), diff.load()
    for y in range(inner):
        for x in range(inner):
            a = ap[x, y]
            b = bp[x, y]
            delta = max(abs(a[0] - b[0]), abs(a[1] - b[1]), abs(a[2] - b[2]))
            alpha = min(255, int(delta * 2.6))
            if alpha > 6:
                dp[x, y] = (a[0], a[1], a[2], alpha)
    fg.alpha_composite(diff, ((fg_size - inner) // 2, (fg_size - inner) // 2))
    fg.save(os.path.join(BASE, "drawable-nodpi", "ic_launcher_fg.png"), optimize=True)
    print("icons written")


if __name__ == "__main__":
    main()
