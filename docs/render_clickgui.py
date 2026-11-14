#!/usr/bin/env python3
"""Render a mockup of the Hachimi ClickGUI using the layout constants from the
source: panel 105x15, module row 103x13, spacing 2*scale, start (15,15), accent
#823CDC, panel #0D1017/93%, body #101418/87%, screen dim #0D1017/73%.
Produces docs/clickgui.png (the README image).
"""
import glob
import os

from PIL import Image, ImageDraw, ImageFont

SS = 2                     # supersampling
W, H = 780, 200            # logical (Minecraft GUI) pixels
SCALE = 1.0

ACCENT = (130, 60, 220)    # ColorsModule default
PANEL = (13, 16, 23, 238)  # 0xEE0D1017
BODY = (16, 20, 24, 221)   # 0xDD101418
DIM = (13, 16, 23, 187)    # 0xBB0D1017
WHITE = (255, 255, 255, 255)
GRAY = (170, 170, 170, 255)
TOOLTIP = (0, 0, 0, 60)


def A(c, a):
    return (c[0], c[1], c[2], max(0, min(255, int(a))))


_fonts = sorted(glob.glob('/usr/share/fonts/**/*.ttf', recursive=True)) or \
    sorted(glob.glob('/usr/share/fonts/**/*.otf', recursive=True))


def font(size):
    px = int(size * SS)
    if _fonts:
        return ImageFont.truetype(_fonts[0], px)
    return ImageFont.load_default(size=px)


F = font(9)
FS = font(8)

img = Image.new('RGBA', (W * SS, H * SS), (26, 29, 34, 255))
overlay = Image.new('RGBA', img.size, DIM)
img = Image.alpha_composite(img, overlay)
d = ImageDraw.Draw(img)


def rect(x, y, w, h, color):
    d.rectangle([x * SS, y * SS, (x + w) * SS - 1, (y + h) * SS - 1], fill=color)


def text(x, y, s, color=WHITE, f=None):
    d.text((x * SS, y * SS), s, font=f or F, fill=color)


PANELS = [
    ('Combat', [
        ('Aura', True, [('slider', 'Range', '4.5'), ('dropdown', 'Target', 'Players'),
                        ('check', 'Rotate', '')]),
        ('AutoCrystal', True, None),
        ('PistonCrystal', False, None),
        ('Surround', True, None),
        ('AutoTotem', True, None),
        ('AutoTrap', False, None),
        ('Criticals', False, None),
    ]),
    ('Miscellaneous', [('Timer', True, None), ('ChestStealer', False, None),
                       ('AutoLogin', True, None), ('BetterChat', False, None),
                       ('Spammer', False, None), ('MiddleClick', False, None)]),
    ('Exploits', [('PacketFly', True, None), ('Phase', False, None), ('Disabler', True, None),
                  ('Backtrack', False, None), ('FakeLatency', False, None)]),
    ('Render', [('ESP', True, None), ('Nametags', True, None), ('Chams', False, None),
                ('Tracers', False, None), ('HoleESP', True, None), ('Fullbright', False, None)]),
    ('Movement', [('Speed', True, None), ('Flight', False, None), ('Step', False, None),
                  ('NoFall', True, None), ('Sprint', False, None)]),
    ('Player', [('Scaffold', False, None), ('AutoTool', True, None), ('AutoEat', False, None),
                ('Freecam', False, None)]),
    ('Client', [('HUD', True, None), ('ClickGui', True, None), ('Colors', False, None),
                ('Font', False, None)]),
]

x = 15.0
Y0 = 14.0
for name, modules in PANELS:
    y = Y0
    hh = 15.0
    rows_h = 0.0
    for _, _, cfgs in modules:
        rows_h += 13.0 + 1.0
        if cfgs:
            rows_h += 3.0 + 13.0 * len(cfgs)
    total = hh + rows_h + 2.0
    body_h = total - hh

    rect(x, y + hh, 105.0, body_h, BODY)          # body first
    rect(x, y, 105.0, hh, PANEL)                  # header on top

    if name == 'Client':                          # search box
        rect(x, y + hh, 105.0, 13.0, A(ACCENT, 40))
        text(x + 4, y + hh + 3.5, 'Search', GRAY, FS)
        body_h -= 13.0
        total -= 13.0

    rect(x, y, 2.0, total, A(ACCENT, 255))                          # accent bar
    rect(x, y + hh - 1.0, 105.0, 1.0, A(ACCENT, 80))                # divider
    text(x + 6.0, y + 4.0, name)

    ry = y + hh + (13.0 if name == 'Client' else 0.0)
    for mname, on, cfgs in modules:
        if on:
            rect(x, ry, 103.0, 13.0, A(ACCENT, 100))
            rect(x, ry, 1.0, 13.0, A(ACCENT, 220))
        else:
            rect(x, ry, 103.0, 13.0, (0, 0, 0, 14))
        text(x + 4.0, ry + 3.5, mname, WHITE if on else GRAY, FS)
        ry += 13.0 + 1.0
        if cfgs:
            ry += 3.0
            for kind, label, value in cfgs:
                rect(x + 6.0, ry, 97.0, 13.0, A(ACCENT, 60))
                if kind == 'slider':
                    rect(x + 6.0, ry, 97.0 * 0.45, 13.0, A(ACCENT, 110))
                text(x + 9.0, ry + 3.5, label, WHITE, FS)
                if value:
                    text(x + 60.0, ry + 3.5, value, A(ACCENT, 255), FS)
                if kind == 'dropdown':
                    text(x + 95.0, ry + 3.5, 'v', GRAY, FS)
                if kind == 'check':
                    rect(x + 90.0, ry + 4.0, 6.0, 6.0, A(ACCENT, 255))
                ry += 13.0
    x += 105.0 + 2.0 * SCALE

# tooltip, as ClickGuiScreen draws it: offset +10/-8, black at 20-43% alpha
mouse = (556.0, 132.0)
tip = 'The range in blocks'
tw = d.textlength(tip, font=F) / SS
rect(mouse[0] + 10.0, mouse[1] - 8.0, tw + 4.0, 11.0, TOOLTIP)
text(mouse[0] + 12.0, mouse[1] - 6.0, tip, (255, 255, 255, 255))

# cursor
cx, cy = mouse[0], mouse[1]
d.polygon([(cx * SS, cy * SS), (cx * SS, (cy + 12) * SS), (cx * SS + 9 * SS, (cy + 9) * SS)],
          fill=(255, 255, 255, 255))

out = img.convert('RGB')
pal = out.convert('P', palette=Image.ADAPTIVE, colors=16)
pal.save('docs/clickgui.png', optimize=True)
print('zapisano docs/clickgui.png', os.path.getsize('docs/clickgui.png'), 'B', out.size)
