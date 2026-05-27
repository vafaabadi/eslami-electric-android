#!/usr/bin/env python3
"""Generate Play Store portrait screenshots (phone + 7\" / 10\" tablet)."""
from __future__ import annotations

import sys
from dataclasses import dataclass
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("Install Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
ICON = ROOT / "play-store" / "icon-512.png"
WEB_ICON = ROOT.parent / "cursor-my-web-app" / "public" / "icons" / "icon-512.png"
OUT_ROOT = ROOT / "play-store" / "screenshots"

SLATE_900 = (15, 23, 42)
SLATE_800 = (30, 41, 59)
SLATE_700 = (51, 65, 85)
SLATE_500 = (100, 116, 139)
SLATE_200 = (226, 232, 240)
SLATE_100 = (241, 245, 249)
SLATE_50 = (248, 250, 252)
AMBER_500 = (245, 158, 11)
AMBER_600 = (217, 119, 6)
WHITE = (255, 255, 255)

PRODUCTS = [
    ("LED Panel 60×60", "Lighting", "$24.50"),
    ("MCB 32A 1P", "Circuit breakers", "$8.90"),
    ("Twin & Earth 2.5mm", "Cable", "$1.85/m"),
    ("Socket 13A White", "Switches & sockets", "$3.20"),
    ("Junction Box IP55", "Enclosures", "$6.40"),
    ("Cable Gland M20", "Accessories", "$1.10"),
    ("Downlight 7W", "Lighting", "$5.75"),
    ("RCD 40A 30mA", "Circuit breakers", "$42.00"),
    ("Flex 3C 1.5mm", "Cable", "$2.10/m"),
    ("Dimmer Switch", "Switches & sockets", "$12.30"),
    ("Conduit 20mm", "Conduits", "$3.60/m"),
    ("Fuse 13A", "Accessories", "$0.85"),
]

BASKET_ITEMS = [
    ("MCB 32A 1P", "Circuit breakers", "$8.90", 2),
    ("Socket 13A White", "Switches & sockets", "$3.20", 4),
    ("LED Panel 60×60", "Lighting", "$24.50", 1),
]


@dataclass(frozen=True)
class Device:
    name: str
    width: int
    height: int
    folder: str
    grid_cols: int
    scale: float


DEVICES = [
    Device("phone", 1080, 2400, "phone", 2, 1.0),
    Device("tablet-7", 1200, 1920, "tablet-7", 3, 1.11),
    Device("tablet-10", 1600, 2560, "tablet-10", 3, 1.48),
]

SCREENS = [
    ("01-home", "Home"),
    ("02-products", "Products"),
    ("03-basket", "Basket"),
    ("04-account", "Account"),
]


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates: list[str] = []
    if sys.platform == "win32":
        candidates.extend(
            [
                "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
            ]
        )
    else:
        candidates.append(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
            if bold
            else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        )
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def s(device: Device, px: float) -> int:
    return max(1, int(px * device.scale))


def draw_rounded_rect(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    radius: int,
    fill: tuple[int, ...],
    outline: tuple[int, ...] | None = None,
) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline)


def draw_status_bar(img: Image.Image, device: Device) -> int:
    draw = ImageDraw.Draw(img)
    h = s(device, 48)
    draw.rectangle([0, 0, device.width, h], fill=SLATE_900)
    font = load_font(s(device, 26))
    draw.text((s(device, 36), s(device, 10)), "9:41", font=font, fill=SLATE_200)
    return h


def draw_top_bar(
    img: Image.Image,
    device: Device,
    title: str,
    y: int,
    icon: Image.Image | None = None,
) -> int:
    draw = ImageDraw.Draw(img)
    h = s(device, 112)
    draw.rectangle([0, y, device.width, y + h], fill=SLATE_900)
    draw.rectangle([0, y + h - 4, device.width, y + h], fill=AMBER_500)
    if icon is not None:
        sz = s(device, 56)
        ic = icon.resize((sz, sz), Image.Resampling.LANCZOS)
        img.paste(ic, (s(device, 32), y + (h - sz) // 2), ic)
        tx = s(device, 32) + sz + s(device, 16)
    else:
        tx = s(device, 32)
    title_font = load_font(s(device, 40), bold=True)
    draw.text((tx, y + s(device, 32)), title, font=title_font, fill=WHITE)
    return y + h


def draw_bottom_nav(
    img: Image.Image,
    device: Device,
    active: str,
    basket_count: int = 0,
) -> None:
    draw = ImageDraw.Draw(img)
    h = s(device, 120)
    y = device.height - h
    draw.rectangle([0, y, device.width, device.height], fill=WHITE)
    draw.line([0, y, device.width, y], fill=SLATE_200, width=2)
    tabs = [("Home", "H"), ("Products", "P"), ("Basket", "B"), ("Account", "A")]
    tab_w = device.width // 4
    label_font = load_font(s(device, 24))
    icon_font = load_font(s(device, 32), bold=True)
    for i, (label, glyph) in enumerate(tabs):
        cx = i * tab_w + tab_w // 2
        color = AMBER_600 if label == active else SLATE_500
        gtw = draw.textlength(glyph, font=icon_font)
        draw.text((cx - gtw / 2, y + s(device, 16)), glyph, font=icon_font, fill=color)
        tw = draw.textlength(label, font=label_font)
        draw.text((cx - tw / 2, y + s(device, 64)), label, font=label_font, fill=color)
        if label == "Basket" and basket_count > 0:
            badge_r = s(device, 18)
            bx = cx + s(device, 20)
            by = y + s(device, 10)
            draw.ellipse(
                [bx - badge_r, by - badge_r, bx + badge_r, by + badge_r],
                fill=AMBER_500,
            )
            bf = load_font(s(device, 20), bold=True)
            bt = str(basket_count)
            btw = draw.textlength(bt, font=bf)
            draw.text((bx - btw / 2, by - s(device, 12)), bt, font=bf, fill=WHITE)


def draw_search_bar(draw: ImageDraw.ImageDraw, device: Device, y: int) -> int:
    pad = s(device, 24)
    h = s(device, 72)
    draw_rounded_rect(
        draw,
        (pad, y, device.width - pad, y + h),
        s(device, 12),
        fill=SLATE_100,
        outline=SLATE_200,
    )
    font = load_font(s(device, 28))
    draw.text((pad + s(device, 24), y + s(device, 20)), "Search products…", font=font, fill=SLATE_500)
    return y + h + s(device, 16)


def draw_chip_row(draw: ImageDraw.ImageDraw, device: Device, y: int) -> int:
    x = s(device, 24)
    chip_h = s(device, 56)
    font = load_font(s(device, 26))
    for i, label in enumerate(["All", "Lighting", "Cable", "Breakers"]):
        tw = draw.textlength(label, font=font)
        w = int(tw) + s(device, 48)
        fill = AMBER_500 if i == 0 else SLATE_100
        text_color = WHITE if i == 0 else SLATE_700
        outline = None if i == 0 else SLATE_200
        draw_rounded_rect(draw, (x, y, x + w, y + chip_h), s(device, 28), fill=fill, outline=outline)
        draw.text((x + s(device, 24), y + s(device, 12)), label, font=font, fill=text_color)
        x += w + s(device, 12)
    return y + chip_h + s(device, 16)


def draw_product_card(
    img: Image.Image,
    device: Device,
    x: int,
    y: int,
    w: int,
    name: str,
    category: str,
    price: str,
) -> int:
    draw = ImageDraw.Draw(img)
    card_h = s(device, 320)
    draw_rounded_rect(draw, (x, y, x + w, y + card_h), s(device, 16), fill=WHITE, outline=SLATE_200)
    img_h = int(w * 0.72)
    draw.rectangle([x + 1, y + 1, x + w - 1, y + img_h], fill=SLATE_100)
    letter_font = load_font(s(device, 48), bold=True)
    letter = name[0].upper()
    lw = draw.textlength(letter, font=letter_font)
    draw.text((x + (w - lw) / 2, y + img_h // 2 - s(device, 24)), letter, font=letter_font, fill=SLATE_500)
    name_font = load_font(s(device, 26), bold=True)
    cat_font = load_font(s(device, 22))
    price_font = load_font(s(device, 28), bold=True)
    ty = y + img_h + s(device, 12)
    draw.text((x + s(device, 12), ty), name[:22], font=name_font, fill=SLATE_900)
    draw.text((x + s(device, 12), ty + s(device, 32)), category, font=cat_font, fill=SLATE_500)
    draw.text((x + s(device, 12), ty + s(device, 60)), price, font=price_font, fill=AMBER_600)
    btn_y = ty + s(device, 96)
    btn_h = s(device, 44)
    draw_rounded_rect(
        draw,
        (x + s(device, 12), btn_y, x + w - s(device, 12), btn_y + btn_h),
        s(device, 8),
        fill=AMBER_500,
    )
    btn_font = load_font(s(device, 22), bold=True)
    bt = "Add to basket"
    btw = draw.textlength(bt, font=btn_font)
    draw.text((x + (w - btw) / 2, btn_y + s(device, 8)), bt, font=btn_font, fill=WHITE)
    return card_h


def draw_product_grid(img: Image.Image, device: Device, y: int, bottom: int, count: int, cols: int) -> None:
    pad = s(device, 24)
    gap = s(device, 16)
    usable_w = device.width - pad * 2
    card_w = (usable_w - gap * (cols - 1)) // cols
    col = 0
    cy = y
    card_h = s(device, 320)
    for i in range(min(count, len(PRODUCTS))):
        name, category, price = PRODUCTS[i]
        cx = pad + col * (card_w + gap)
        card_h = draw_product_card(img, device, cx, cy, card_w, name, category, price)
        col += 1
        if col >= cols:
            col = 0
            cy += card_h + gap
            if cy + card_h > bottom:
                break


def content_bottom(device: Device) -> int:
    return device.height - s(device, 120)


def render_home(device: Device, icon: Image.Image | None) -> Image.Image:
    img = Image.new("RGB", (device.width, device.height), SLATE_50)
    y = draw_status_bar(img, device)
    y = draw_top_bar(img, device, "Eslami Electric", y, icon)
    draw = ImageDraw.Draw(img)
    hero_h = s(device, 200)
    draw_rounded_rect(
        draw,
        (s(device, 24), y + s(device, 24), device.width - s(device, 24), y + s(device, 24) + hero_h),
        s(device, 16),
        fill=SLATE_800,
    )
    hf = load_font(s(device, 36), bold=True)
    sf = load_font(s(device, 26))
    hx, hy = s(device, 48), y + s(device, 56)
    draw.text((hx, hy), "Electrical supplies in Zahedan", font=hf, fill=WHITE)
    draw.text((hx, hy + s(device, 52)), "Browse · Order · Pay securely", font=sf, fill=AMBER_500)
    sec_y = y + s(device, 24) + hero_h + s(device, 32)
    sec_font = load_font(s(device, 32), bold=True)
    draw.text((s(device, 24), sec_y), "Featured products", font=sec_font, fill=SLATE_900)
    btn_font = load_font(s(device, 26))
    va = "View all →"
    draw.text((device.width - s(device, 24) - draw.textlength(va, font=btn_font), sec_y + s(device, 4)), va, font=btn_font, fill=AMBER_600)
    draw_product_grid(img, device, sec_y + s(device, 48), content_bottom(device), 6, device.grid_cols)
    draw_bottom_nav(img, device, "Home", basket_count=7)
    return img


def render_products(device: Device, icon: Image.Image | None) -> Image.Image:
    img = Image.new("RGB", (device.width, device.height), SLATE_50)
    y = draw_status_bar(img, device)
    y = draw_top_bar(img, device, "Products", y, icon)
    draw = ImageDraw.Draw(img)
    y = draw_search_bar(draw, device, y + s(device, 16))
    y = draw_chip_row(draw, device, y)
    draw_product_grid(img, device, y, content_bottom(device), 12, device.grid_cols)
    draw_bottom_nav(img, device, "Products", basket_count=7)
    return img


def render_basket(device: Device, icon: Image.Image | None) -> Image.Image:
    img = Image.new("RGB", (device.width, device.height), SLATE_50)
    y = draw_status_bar(img, device)
    y = draw_top_bar(img, device, "Basket", y, icon)
    draw = ImageDraw.Draw(img)
    pad = s(device, 24)
    cy = y + s(device, 24)
    for name, category, price, qty in BASKET_ITEMS:
        row_h = s(device, 140)
        draw_rounded_rect(draw, (pad, cy, device.width - pad, cy + row_h), s(device, 12), fill=WHITE, outline=SLATE_200)
        thumb = s(device, 96)
        draw.rectangle([pad + s(device, 16), cy + s(device, 22), pad + s(device, 16) + thumb, cy + s(device, 22) + thumb], fill=SLATE_100)
        lf = load_font(s(device, 36), bold=True)
        draw.text((pad + s(device, 40), cy + s(device, 48)), name[0], font=lf, fill=SLATE_500)
        nf, cf, pf = load_font(s(device, 28), bold=True), load_font(s(device, 24)), load_font(s(device, 28), bold=True)
        tx = pad + s(device, 16) + thumb + s(device, 20)
        draw.text((tx, cy + s(device, 24)), name, font=nf, fill=SLATE_900)
        draw.text((tx, cy + s(device, 58)), category, font=cf, fill=SLATE_500)
        draw.text((tx, cy + s(device, 92)), price, font=pf, fill=AMBER_600)
        qf = load_font(s(device, 26))
        qty_label = f"Qty {qty}"
        draw.text((device.width - pad - s(device, 16) - draw.textlength(qty_label, font=qf), cy + s(device, 52)), qty_label, font=qf, fill=SLATE_700)
        cy += row_h + s(device, 16)
    subtotal = sum(float(p.replace("$", "").replace("/m", "")) * q for _, _, p, q in BASKET_ITEMS)
    summary_y = cy + s(device, 16)
    tf = load_font(s(device, 30), bold=True)
    draw.text((pad, summary_y), "Subtotal (USD)", font=load_font(s(device, 28)), fill=SLATE_700)
    st = f"${subtotal:.2f}"
    draw.text((device.width - pad - draw.textlength(st, font=tf), summary_y), st, font=tf, fill=SLATE_900)
    btn_y = device.height - s(device, 120) - s(device, 96)
    btn_h = s(device, 72)
    draw_rounded_rect(draw, (pad, btn_y, device.width - pad, btn_y + btn_h), s(device, 12), fill=AMBER_500)
    bf = load_font(s(device, 32), bold=True)
    bt = "Proceed to checkout"
    draw.text(((device.width - draw.textlength(bt, font=bf)) / 2, btn_y + s(device, 18)), bt, font=bf, fill=WHITE)
    draw_bottom_nav(img, device, "Basket", basket_count=7)
    return img


def render_account(device: Device, icon: Image.Image | None) -> Image.Image:
    img = Image.new("RGB", (device.width, device.height), SLATE_50)
    y = draw_status_bar(img, device)
    y = draw_top_bar(img, device, "Account", y, icon)
    draw = ImageDraw.Draw(img)
    pad = s(device, 24)
    cy = y + s(device, 32)
    avatar_r = s(device, 56)
    cx = device.width // 2
    draw.ellipse([cx - avatar_r, cy - avatar_r, cx + avatar_r, cy + avatar_r], fill=SLATE_800)
    af = load_font(s(device, 40), bold=True)
    draw.text((cx - s(device, 12), cy - s(device, 22)), "A", font=af, fill=WHITE)
    nf, ef = load_font(s(device, 34), bold=True), load_font(s(device, 26))
    name, email = "Ali Ahmadi", "ali@example.com"
    cy += avatar_r + s(device, 24)
    draw.text((cx - draw.textlength(name, font=nf) / 2, cy), name, font=nf, fill=SLATE_900)
    draw.text((cx - draw.textlength(email, font=ef) / 2, cy + s(device, 44)), email, font=ef, fill=SLATE_500)
    menu_y = cy + s(device, 120)
    for title, subtitle in [
        ("My orders", "View order history"),
        ("Profile", "Edit name, email & phone"),
        ("Track guest order", "Email + order number"),
        ("Language", "English · فارسی"),
    ]:
        row_h = s(device, 100)
        draw_rounded_rect(draw, (pad, menu_y, device.width - pad, menu_y + row_h), s(device, 12), fill=WHITE, outline=SLATE_200)
        tf, sf = load_font(s(device, 28), bold=True), load_font(s(device, 24))
        draw.text((pad + s(device, 20), menu_y + s(device, 20)), title, font=tf, fill=SLATE_900)
        draw.text((pad + s(device, 20), menu_y + s(device, 56)), subtitle, font=sf, fill=SLATE_500)
        menu_y += row_h + s(device, 12)
    draw_bottom_nav(img, device, "Account", basket_count=7)
    return img


RENDERERS = {
    "01-home": render_home,
    "02-products": render_products,
    "03-basket": render_basket,
    "04-account": render_account,
}


def load_icon() -> Image.Image | None:
    icon_path = ICON if ICON.exists() else WEB_ICON
    if not icon_path.exists():
        print(f"Warning: no icon at {ICON} or {WEB_ICON}", file=sys.stderr)
        return None
    return Image.open(icon_path).convert("RGBA")


def generate_all() -> list[tuple[str, int, int]]:
    icon = load_icon()
    manifest: list[tuple[str, int, int]] = []
    for device in DEVICES:
        out_dir = OUT_ROOT / device.folder
        out_dir.mkdir(parents=True, exist_ok=True)
        for stem, _label in SCREENS:
            img = RENDERERS[stem](device, icon)
            out_path = out_dir / f"{stem}.png"
            img.save(out_path, format="PNG", optimize=True)
            manifest.append((str(out_path.relative_to(ROOT)).replace("\\", "/"), device.width, device.height))
            print(f"Wrote {out_path} ({device.width}x{device.height})")
    return manifest


def main() -> None:
    manifest = generate_all()
    print("\nGenerated files:")
    for path, w, h in manifest:
        print(f"  {path}  {w}x{h}")


if __name__ == "__main__":
    main()
