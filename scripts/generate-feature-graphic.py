#!/usr/bin/env python3
"""Generate Play Store feature graphic 1024x500 (PNG)."""
from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("Install Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
ICON = ROOT / "play-store" / "icon-512.png"
WEB_ICON = ROOT.parent / "cursor-my-web-app" / "public" / "icons" / "icon-512.png"
OUT = ROOT / "play-store" / "feature-graphic-1024x500.png"

W, H = 1024, 500
SLATE = (15, 23, 42)       # slate-900
SLATE_MID = (30, 41, 59)   # slate-800 band
AMBER = (245, 158, 11)     # amber-500
WHITE = (248, 250, 252)


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = []
    if sys.platform == "win32":
        candidates.extend(
            [
                "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
            ]
        )
    else:
        candidates.extend(
            [
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
                if bold
                else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            ]
        )
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def main() -> None:
    icon_path = ICON if ICON.exists() else WEB_ICON
    if not icon_path.exists():
        print(f"Missing icon: {ICON} or {WEB_ICON}", file=sys.stderr)
        sys.exit(1)

    img = Image.new("RGB", (W, H), SLATE)
    draw = ImageDraw.Draw(img)

    # Subtle diagonal accent band
    draw.polygon([(0, H), (W * 0.55, 0), (W, 0), (W, H)], fill=SLATE_MID)
    draw.rectangle([0, H - 6, W, H], fill=AMBER)

    icon = Image.open(icon_path).convert("RGBA")
    icon_size = 200
    icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    ix, iy = 72, (H - icon_size) // 2
    img.paste(icon, (ix, iy), icon)

    title_font = load_font(56, bold=True)
    tag_font = load_font(28, bold=False)
    tx = ix + icon_size + 48
    draw.text((tx, 155), "Eslami Electric", font=title_font, fill=WHITE)
    draw.text((tx, 230), "Electrical supplies — order online", font=tag_font, fill=AMBER)
    draw.text((tx, 290), "Zahedan · Quality electrical shop", font=tag_font, fill=(148, 163, 184))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, format="PNG", optimize=True)
    verify = Image.open(OUT)
    if verify.size != (W, H):
        print(f"Wrong size: {verify.size}", file=sys.stderr)
        sys.exit(1)
    print(f"Wrote {OUT} ({W}x{H})")


if __name__ == "__main__":
    main()
