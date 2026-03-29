from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1200, 630
img = Image.new('RGB', (W, H), '#0f0f0f')
draw = ImageDraw.Draw(img)

# Background gradient (dark navy)
for y in range(H):
    r = int(15 + (26 - 15) * y / H)
    g = int(15 + (26 - 15) * y / H)
    b = int(15 + (46 - 15) * y / H)
    draw.line([(0, y), (W, y)], fill=(r, g, b))

# Dot pattern
for x in range(0, W, 30):
    for y in range(0, H, 30):
        draw.ellipse([x-1, y-1, x+1, y+1], fill=(255, 255, 255, 8))

# Corner lines
draw.line([(30, 30), (230, 30)], fill=(34, 211, 238, 40), width=2)
draw.line([(30, 30), (30, 230)], fill=(34, 211, 238, 40), width=2)
draw.line([(W-30, H-30), (W-230, H-30)], fill=(34, 211, 238, 40), width=2)
draw.line([(W-30, H-30), (W-30, H-230)], fill=(34, 211, 238, 40), width=2)

# Glow effects (simplified as semi-transparent circles)
glow_layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
glow_draw = ImageDraw.Draw(glow_layer)

# Cyan glow top-left
for r in range(200, 0, -2):
    alpha = int(60 * (1 - r / 200))
    glow_draw.ellipse([50 - r, 100 - r, 50 + r, 100 + r], fill=(34, 211, 238, alpha))

# Purple glow bottom-right
for r in range(180, 0, -2):
    alpha = int(60 * (1 - r / 180))
    glow_draw.ellipse([W - 50 - r, H - 100 - r, W - 50 + r, H - 100 + r], fill=(139, 92, 246, alpha))

img = Image.alpha_composite(img.convert('RGBA'), glow_layer).convert('RGB')
draw = ImageDraw.Draw(img)

# Try to load fonts
def get_font(size, bold=False):
    paths = [
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeui.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for p in paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()

font_huge = get_font(52, bold=True)
font_large = get_font(44, bold=True)
font_medium = get_font(24)
font_small = get_font(18)
font_tiny = get_font(15)
font_badge = get_font(16, bold=True)
font_btn = get_font(20, bold=True)
font_logo = get_font(32, bold=True)
font_app = get_font(26, bold=True)

# Logo circle
logo_x, logo_y = W // 2 - 100, 95
for r in range(42, 0, -1):
    ratio = r / 42
    cr = int(34 + (139 - 34) * (1 - ratio))
    cg = int(211 + (92 - 211) * (1 - ratio))
    cb = int(238 + (246 - 238) * (1 - ratio))
    draw.ellipse([logo_x - r, logo_y - r, logo_x + r, logo_y + r], fill=(cr, cg, cb))

draw.text((logo_x, logo_y), "AI", fill='white', font=font_logo, anchor='mm')

# App name
draw.text((logo_x + 60, logo_y), "AI AutoResponder", fill=(255, 255, 255, 230), font=font_app, anchor='lm')

# Main headline
line1 = "Never Miss a Message."
draw.text((W // 2, 190), line1, fill='white', font=font_huge, anchor='mm')

# Gradient text (cyan to purple)
line2 = "Let AI Reply For You."
bbox = draw.textbbox((0, 0), line2, font=font_huge)
tw = bbox[2] - bbox[0]
tx = (W - tw) // 2
for i, char in enumerate(line2):
    ratio = i / max(len(line2) - 1, 1)
    r = int(34 + (139 - 34) * ratio)
    g = int(211 + (92 - 211) * ratio)
    b = int(238 + (246 - 238) * ratio)
    char_bbox = draw.textbbox((0, 0), line2[:i], font=font_huge)
    cx = tx + (char_bbox[2] - char_bbox[0])
    draw.text((cx, 250), char, fill=(r, g, b), font=font_huge, anchor='mt')

# Subtitle
subtitle = "Automatically respond to WhatsApp, Messenger, Telegram & more"
draw.text((W // 2, 330), subtitle, fill=(255, 255, 255, 150), font=font_medium, anchor='mm')

# Platform badges
platforms = ["WhatsApp", "Messenger", "Telegram", "Instagram"]
badge_w = 130
badge_h = 38
total_w = len(platforms) * badge_w + (len(platforms) - 1) * 16
start_x = (W - total_w) // 2
badge_y = 380

for i, name in enumerate(platforms):
    bx = start_x + i * (badge_w + 16)
    draw.rounded_rectangle([bx, badge_y, bx + badge_w, badge_y + badge_h], radius=19,
                           fill=(255, 255, 255, 25), outline=(255, 255, 255, 40))
    draw.text((bx + badge_w // 2, badge_y + badge_h // 2), name,
              fill=(255, 255, 255, 200), font=font_tiny, anchor='mm')

# Download button
btn_text = "Download Free APK"
btn_bbox = draw.textbbox((0, 0), btn_text, font=font_btn)
btn_tw = btn_bbox[2] - btn_bbox[0]
btn_w = btn_tw + 60
btn_h = 50
btn_x = (W - btn_w) // 2
btn_y = 460

draw.rounded_rectangle([btn_x, btn_y, btn_x + btn_w, btn_y + btn_h], radius=25, fill='white')
draw.text((btn_x + btn_w // 2, btn_y + btn_h // 2), btn_text, fill='#1a1a1a', font=font_btn, anchor='mm')

# Bottom text
draw.text((W // 2, 550), "AI AutoResponder", fill=(255, 255, 255, 80), font=font_small, anchor='mm')
draw.text((W // 2, 580), "Version 1.0.0  •  Android 8.0+", fill=(255, 255, 255, 60), font=font_tiny, anchor='mm')

img.save('og-banner.png', 'PNG', quality=95)
img.save('og-banner.jpg', 'JPEG', quality=95)
print("Banner saved as og-banner.png and og-banner.jpg")
