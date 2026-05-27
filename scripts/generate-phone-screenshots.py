#!/usr/bin/env python3
"""Generate Play Store phone screenshots (1080x1920 PNG) when no adb device is available."""
from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("Install Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "play-store" / "screenshots" / "phone"

W, H = 1080, 1920
STATUS_H = 48
NAV_H = 88

SLATE_900 = (15, 23, 42)
SLATE_800 = (30, 41, 59)
SLATE_100 = (241, 245, 249)
SLATE_200 = (226, 232, 240)
SLATE_400 = (148, 163, 184)
SLATE_600 = (71, 85, 105)
WHITE = (255, 255, 255)
AMBER = (245, 158, 11)
AMBER_DARK = (180, 83, 9)
ON_PRIMARY = SLATE_900
PRIMARY_CONTAINER = (254, 243, 199)
SURFACE_VARIANT = (226, 232, 240)


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


def text_size(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont) -> tuple[int, int]:
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0], box[3] - box[1]


def draw_status_bar(draw: ImageDraw.ImageDraw) -> int:
    draw.rectangle([0, 0, W, STATUS_H], fill=SLATE_900)
    draw.text((36, 12), "9:41", font=load_font(22), fill=WHITE)
    return STATUS_H


def draw_bottom_nav(draw: ImageDraw.ImageDraw, y_top: int, selected: int, basket_badge: int | None = None) -> None:
    labels = ["Home", "Products", "Basket", "Account"]
    draw.rectangle([0, y_top, W, H], fill=WHITE)
    draw.line([0, y_top, W, y_top], fill=SLATE_200, width=2)
    slot_w = W // 4
    icon_font = load_font(28)
    label_font = load_font(22)
    icons = ["⌂", "▣", "🛒", "👤"]
    for i, (label, icon) in enumerate(zip(labels, icons)):
        cx = slot_w * i + slot_w // 2
        selected_here = i == selected
        if selected_here:
            draw.ellipse([cx - 36, y_top + 10, cx + 36, y_top + 74], fill=PRIMARY_CONTAINER)
        color = AMBER_DARK if selected_here else SLATE_600
        iw, _ = text_size(draw, icon, icon_font)
        draw.text((cx - iw // 2, y_top + 18), icon, font=icon_font, fill=color)
        lw, _ = text_size(draw, label, label_font)
        draw.text((cx - lw // 2, y_top + 52), label, font=label_font, fill=color)
        if i == 2 and basket_badge and basket_badge > 0:
            badge = str(basket_badge)
            bf = load_font(18, bold=True)
            bw, bh = text_size(draw, badge, bf)
            bx, by = cx + 14, y_top + 8
            draw.ellipse([bx - 4, by - 4, bx + bw + 12, by + bh + 8], fill=AMBER)
            draw.text((bx + 4, by), badge, font=bf, fill=ON_PRIMARY)


def rounded_rect(draw, xy, radius, fill, outline=None):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=1 if outline else 0)


def draw_product_card(draw, x, y, cw, ch, name, category, price, letter):
    rounded_rect(draw, (x, y, x + cw, y + ch), 12, WHITE, SLATE_200)
    img_h = int(cw * 0.72)
    rounded_rect(draw, (x + 8, y + 8, x + cw - 8, y + 8 + img_h), 8, SURFACE_VARIANT)
    lf = load_font(48, bold=True)
    lw, lh = text_size(draw, letter, lf)
    draw.text((x + (cw - lw) // 2, y + 8 + (img_h - lh) // 2), letter, font=lf, fill=SLATE_600)
    ty = y + 8 + img_h + 14
    draw.text((x + 14, ty), name, font=load_font(26, bold=True), fill=SLATE_900)
    draw.text((x + 14, ty + 32), category, font=load_font(20), fill=SLATE_600)
    draw.text((x + 14, ty + 58), price, font=load_font(28, bold=True), fill=SLATE_900)
    btn_y = ty + 92
    rounded_rect(draw, (x + 14, btn_y, x + cw - 14, btn_y + 44), 22, AMBER)
    btn_text = "Add to basket"
    bf = load_font(22, bold=True)
    btw, _ = text_size(draw, btn_text, bf)
    draw.text((x + (cw - btw) // 2, btn_y + 10), btn_text, font=bf, fill=ON_PRIMARY)


def screen_home(draw, content_top, content_bottom):
    draw.text((40, content_top + 16), "Featured products", font=load_font(40, bold=True), fill=SLATE_900)
    draw.text((W - 280, content_top + 24), "View all products", font=load_font(28), fill=AMBER_DARK)
    products = [
        ("LED Panel 60×60", "Lighting", "$24.99", "L"),
        ("MCB 32A Type C", "Protection", "$18.50", "M"),
        ("Twin & Earth 2.5mm", "Cable", "$89.00", "T"),
        ("32A Industrial Socket", "Accessories", "$42.00", "S"),
        ("Emergency Exit Sign", "Lighting", "$31.25", "E"),
        ("Cable Tray 3m", "Accessories", "$15.99", "C"),
    ]
    pad, gap, cw, ch = 28, 20, (W - 28 * 2 - 20) // 2, 420
    y0 = content_top + 72
    for idx, p in enumerate(products):
        x = pad + (idx % 2) * (cw + gap)
        y = y0 + (idx // 2) * (ch + gap)
        if y + ch > content_bottom - 8:
            break
        draw_product_card(draw, x, y, cw, ch, *p)


def screen_products(draw, content_top, content_bottom):
    field_y = content_top + 12
    rounded_rect(draw, (32, field_y, W - 32, field_y + 56), 12, WHITE, SLATE_200)
    draw.text((52, field_y + 14), "Search products…", font=load_font(28), fill=SLATE_400)
    chips = ["All", "Lighting", "Cable", "Protection", "Accessories"]
    cx, cy = 32, field_y + 72
    for i, chip in enumerate(chips):
        tw, _ = text_size(draw, chip, load_font(24))
        cw = tw + 36
        fill = AMBER if i == 2 else WHITE
        rounded_rect(draw, (cx, cy, cx + cw, cy + 44), 22, fill, None if i == 2 else SLATE_200)
        draw.text((cx + 18, cy + 10), chip, font=load_font(24), fill=ON_PRIMARY if i == 2 else SLATE_600)
        cx += cw + 12
        if cx > W - 120:
            break
    products = [
        ("LED Downlight 12W", "Lighting", "$8.99", "D"),
        ("RCD 40A 30mA", "Protection", "$56.00", "R"),
        ("Flex 3G1.5mm 100m", "Cable", "$124.00", "F"),
        ("Surface Mount Box", "Accessories", "$3.20", "B"),
    ]
    pad, gap, cw, ch = 28, 20, (W - 28 * 2 - 20) // 2, 400
    y0 = cy + 60
    for idx, p in enumerate(products):
        draw_product_card(draw, pad + (idx % 2) * (cw + gap), y0 + (idx // 2) * (ch + gap), cw, ch, *p)


def screen_basket(draw, content_top, content_bottom):
    items = [
        ("MCB 32A Type C", "$18.50", "$37.00", "M", 2),
        ("LED Panel 60×60", "$24.99", "$24.99", "L", 1),
        ("Twin & Earth 2.5mm", "$89.00", "$89.00", "T", 1),
    ]
    y = content_top + 20
    for name, unit, line, letter, qty in items:
        card_h = 200
        rounded_rect(draw, (32, y, W - 32, y + card_h), 12, WHITE, SLATE_200)
        rounded_rect(draw, (48, y + 24, 120, y + 120), 8, SURFACE_VARIANT)
        lf = load_font(36, bold=True)
        lw, lh = text_size(draw, letter, lf)
        draw.text((84 - lw // 2, y + 52), letter, font=lf, fill=SLATE_600)
        draw.text((140, y + 20), name, font=load_font(30, bold=True), fill=SLATE_900)
        draw.text((140, y + 56), unit, font=load_font(26), fill=SLATE_600)
        draw.text((140, y + 88), line, font=load_font(28, bold=True), fill=SLATE_900)
        draw.text((140, y + 130), f"−  {qty}  +", font=load_font(26, bold=True), fill=SLATE_800)
        draw.text((W - 160, y + 130), "Remove", font=load_font(26), fill=AMBER_DARK)
        y += card_h + 16
    draw.line([32, content_bottom - 160, W - 32, content_bottom - 160], fill=SLATE_200, width=2)
    draw.text((48, content_bottom - 130), "Total", font=load_font(32, bold=True), fill=SLATE_900)
    draw.text((W - 200, content_bottom - 130), "$150.99", font=load_font(36, bold=True), fill=SLATE_900)
    btn_y = content_bottom - 88
    rounded_rect(draw, (48, btn_y, W - 48, btn_y + 56), 28, AMBER)
    bt = "Proceed to checkout"
    bf = load_font(30, bold=True)
    btw, _ = text_size(draw, bt, bf)
    draw.text(((W - btw) // 2, btn_y + 12), bt, font=bf, fill=ON_PRIMARY)


def screen_account(draw, content_top, content_bottom):
    y = content_top + 80
    draw.text((48, y), "Language", font=load_font(26), fill=SLATE_600)
    y += 40
    bf = load_font(30, bold=True)
    rounded_rect(draw, (48, y, 280, y + 48), 24, AMBER)
    draw.text((88, y + 10), "English", font=bf, fill=ON_PRIMARY)
    rounded_rect(draw, (300, y, 520, y + 48), 24, WHITE, SLATE_200)
    draw.text((360, y + 10), "فارسی", font=bf, fill=SLATE_600)
    y += 120
    draw.text((80, y), "Sign in to manage your profile and orders", font=load_font(28), fill=SLATE_600)
    y += 80
    for label, filled in [("Track guest order", False), ("Log in", True), ("Sign up", False)]:
        h = 56
        if filled:
            rounded_rect(draw, (48, y, W - 48, y + h), 28, AMBER)
            draw.text((80, y + 12), label, font=bf, fill=ON_PRIMARY)
        else:
            rounded_rect(draw, (48, y, W - 48, y + h), 28, WHITE, SLATE_200)
            draw.text((80, y + 12), label, font=bf, fill=SLATE_800)
        y += h + 16


SCREENS = [
    ("01-home-featured.png", screen_home, 0, None),
    ("02-products-search.png", screen_products, 1, None),
    ("03-basket-items.png", screen_basket, 2, 3),
    ("04-account-sign-in.png", screen_account, 3, None),
]


def render_screen(filename, painter, nav_index, badge):
    img = Image.new("RGB", (W, H), SLATE_100)
    draw = ImageDraw.Draw(img)
    top = draw_status_bar(draw)
    nav_top = H - NAV_H
    painter(draw, top, nav_top)
    draw_bottom_nav(draw, nav_top, nav_index, badge)
    out = OUT_DIR / filename
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, format="PNG", optimize=True)
    if Image.open(out).size != (W, H):
        raise SystemExit(f"Wrong size for {out}")
    return out


def main():
    for filename, painter, nav_idx, badge in SCREENS:
        p = render_screen(filename, painter, nav_idx, badge)
        print(f"  {p} ({W}x{H})")


if __name__ == "__main__":
    main()
