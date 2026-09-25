"""Draws Tik's app icons as Android adaptive icons: the same six styles as the iOS app.

Each style gets a background layer (a gradient with two soft glows) and a foreground layer (a
glass plate with the check mark), written as vector drawables. All styles share one monochrome
layer, which Android 13 and newer use for themed icons.

The shapes come from the iOS script (Scripts/make-icons.swift in tik-todo-ios), which draws on a
1024 x 1024 grid. Here that grid is placed on the 72 dp area an adaptive icon always shows.

Usage (from the repo root):  python3 scripts/make-icons.py
"""

import os

RES = "app/src/main/res"

STYLES = [
    # name, top, bottom, glow A, glow B, plate alpha, check color
    ("blue", "62D0FF", "0A5BFF", "B8F1FF", "7B4DFF", 0.20, "FFFFFF"),
    ("midnight", "1C1F2E", "050508", "2F6BFF", "8A3DFF", 0.09, "FFFFFF"),
    ("light", "FFFFFF", "DDE5F2", "FFFFFF", "9CC3FF", 0.55, "0A6CFF"),
    ("mint", "7CF5C4", "00A88E", "E0FFF4", "00B7D4", 0.20, "FFFFFF"),
    ("purple", "C58BFF", "4B18D6", "F2DEFF", "FF5DB1", 0.20, "FFFFFF"),
    ("sunset", "FFC26B", "F2395F", "FFF1C9", "B5179E", 0.20, "FFFFFF"),
]

SCALE = 72 / 1024
OFFSET = 18


def x(value):
    return round(OFFSET + value * SCALE, 2)


def y(value_from_bottom):
    """The iOS script draws with y going up; Android's y goes down."""
    return round(OFFSET + (1024 - value_from_bottom) * SCALE, 2)


def size(value):
    return round(value * SCALE, 2)


def color(hex_rgb, alpha=1.0):
    return "#%02X%s" % (round(alpha * 255), hex_rgb)


def rounded_rect(left, top, right, bottom, radius):
    def n(value):
        return f"{value:.2f}".rstrip("0").rstrip(".")

    return (
        f"M{n(left + radius)},{n(top)} H{n(right - radius)} "
        f"A{n(radius)},{n(radius)} 0 0 1 {n(right)},{n(top + radius)} V{n(bottom - radius)} "
        f"A{n(radius)},{n(radius)} 0 0 1 {n(right - radius)},{n(bottom)} H{n(left + radius)} "
        f"A{n(radius)},{n(radius)} 0 0 1 {n(left)},{n(bottom - radius)} V{n(top + radius)} "
        f"A{n(radius)},{n(radius)} 0 0 1 {n(left + radius)},{n(top)} Z"
    )


HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Made by scripts/make-icons.py -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
"""

FULL = "M0,0 H108 V108 H0 Z"


def background(style):
    name, top, bottom, glow_a, glow_b, _, _ = style
    parts = [HEADER]
    parts.append(f"""    <path android:pathData="{FULL}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear"
                android:startX="{x(0)}" android:startY="{y(1024)}"
                android:endX="{x(1024)}" android:endY="{y(0)}"
                android:startColor="{color(top)}" android:endColor="{color(bottom)}" />
        </aapt:attr>
    </path>
""")
    for (cx, cy, radius, glow, alpha) in [(190, 880, 640, glow_a, 0.55), (900, 110, 660, glow_b, 0.50)]:
        parts.append(f"""    <path android:pathData="{FULL}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial"
                android:centerX="{x(cx)}" android:centerY="{y(cy)}"
                android:gradientRadius="{size(radius)}">
                <item android:offset="0" android:color="{color(glow, alpha)}" />
                <item android:offset="1" android:color="{color(glow, 0)}" />
            </gradient>
        </aapt:attr>
    </path>
""")
    parts.append("</vector>\n")
    return "".join(parts)


PLATE = (212, 212, 812, 812, 172)
CHECK = [(366, 520), (462, 424), (662, 624)]


def check_path(dy=0.0):
    points = [(x(px), y(py) + dy) for px, py in CHECK]
    return "M{},{} L{},{} L{},{}".format(*[round(v, 2) for point in points for v in point])


def foreground(style):
    _, _, _, _, _, plate_alpha, check = style
    left, top, right, bottom, radius = x(PLATE[0]), y(PLATE[3]), x(PLATE[2]), y(PLATE[1]), size(PLATE[4])
    parts = [HEADER]

    # A soft shadow under the plate, made of a few see-through layers (vectors can't blur).
    for grow, drop, alpha in [(4.5, 3.2, 0.03), (3.0, 2.6, 0.04), (1.5, 2.0, 0.05)]:
        path = rounded_rect(left - grow, top - grow + drop, right + grow, bottom + grow + drop, radius + grow)
        parts.append(f'    <path android:pathData="{path}" android:fillColor="{color("000000", alpha)}" />\n')

    plate = rounded_rect(left, top, right, bottom, radius)
    parts.append(f'    <path android:pathData="{plate}" android:fillColor="{color("FFFFFF", plate_alpha)}" />\n')

    # The light on the top half of the plate.
    parts.append(f"""    <path android:pathData="{plate}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear"
                android:startX="0" android:startY="{top}"
                android:endX="0" android:endY="{y(512 + 40)}"
                android:startColor="{color('FFFFFF', 0.40)}" android:endColor="{color('FFFFFF', 0)}" />
        </aapt:attr>
    </path>
""")

    # The thin bright rim.
    parts.append(f"""    <path android:pathData="{plate}" android:strokeWidth="{size(7)}">
        <aapt:attr name="android:strokeColor">
            <gradient android:type="linear"
                android:startX="{left}" android:startY="{top}"
                android:endX="{right}" android:endY="{bottom}">
                <item android:offset="0" android:color="{color('FFFFFF', 0.85)}" />
                <item android:offset="0.55" android:color="{color('FFFFFF', 0.10)}" />
                <item android:offset="1" android:color="{color('FFFFFF', 0.50)}" />
            </gradient>
        </aapt:attr>
    </path>
""")

    # The check mark and its shadow.
    parts.append(
        f'    <path android:pathData="{check_path(size(10))}" android:strokeWidth="{size(96)}" '
        f'android:strokeColor="{color("000000", 0.12)}" android:strokeLineCap="round" android:strokeLineJoin="round" />\n'
    )
    parts.append(
        f'    <path android:pathData="{check_path()}" android:strokeWidth="{size(86)}" '
        f'android:strokeColor="{color(check)}" android:strokeLineCap="round" android:strokeLineJoin="round" />\n'
    )
    parts.append("</vector>\n")
    return "".join(parts)


def monochrome():
    return HEADER + (
        f'    <path android:pathData="{check_path()}" android:strokeWidth="{size(86)}" '
        f'android:strokeColor="#FFFFFFFF" android:strokeLineCap="round" android:strokeLineJoin="round" />\n'
        "</vector>\n"
    )


def adaptive(name):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Made by scripts/make-icons.py -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_{name}_background" />
    <foreground android:drawable="@drawable/ic_launcher_{name}_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as file:
        file.write(text)


if __name__ == "__main__":
    write(f"{RES}/drawable/ic_launcher_monochrome.xml", monochrome())
    for style in STYLES:
        name = style[0]
        write(f"{RES}/drawable/ic_launcher_{name}_background.xml", background(style))
        write(f"{RES}/drawable/ic_launcher_{name}_foreground.xml", foreground(style))
        write(f"{RES}/mipmap-anydpi/ic_launcher_{name}.xml", adaptive(name))
        print("Wrote", name)
