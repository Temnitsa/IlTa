#!/usr/bin/env python3
"""Deterministic generator for the IlTa preposition illustrations.

Renders 39 "positional" and "directional" preposition scenes in a consistent
isometric 3D style (glossy blue cubes + glossy red spheres + red arrows,
soft shadows, transparent background) with Pillow, at 4x supersampling,
downsampled with LANCZOS to 512x512 RGBA PNGs.

Outputs:
  app/src/main/res/drawable-nodpi/prep_<key>.png   (39 scenes)
  docs/prep-art/preview.png                        (contact sheet)

No network access, deterministic output (no randomness).
"""
import argparse
import math
import os
import shutil
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DRAWABLE_DIR = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
DOCS_DIR = os.path.join(ROOT, "docs", "prep-art")

SS = 4                      # supersampling factor
SIZE = 512                  # final PNG size
W = SIZE * SS               # render size

# ----------------------------------------------------------------------------
# palette (matched to the reference art)
# ----------------------------------------------------------------------------
TOP_C0 = (0x7D, 0xA9, 0xF9)
TOP_C1 = (0x3A, 0x63, 0xD2)
LEFT_C0 = (0x2C, 0x54, 0xC2)
LEFT_C1 = (0x10, 0x21, 0x60)
RIGHT_C0 = (0x3D, 0x73, 0xE5)
RIGHT_C1 = (0x1B, 0x37, 0x90)
EDGE_C = (0x0A, 0x14, 0x38, 165)

RED = (0xD2, 0x2C, 0x2C)
RED_DARK = (0x83, 0x13, 0x15)
RED_LIGHT = (0xEE, 0x77, 0x6E)

BAND_BLUE = (0x2E, 0x5C, 0xC6)
BAND_DARK = (0x17, 0x2F, 0x7E)
BAND_SHEEN = (0x63, 0x8F, 0xE8)

GLASS_SPHERE_ALPHA = 150


# ----------------------------------------------------------------------------
# small helpers
# ----------------------------------------------------------------------------
def bez(ctrl, n=64):
    """Sample a cubic/quadratic Bezier curve given control points (512-space)."""
    pts = []
    m = len(ctrl) - 1
    for i in range(n + 1):
        t = i / n
        p = [tuple(c) for c in ctrl]
        for _ in range(m):
            p = [((1 - t) * p[j][0] + t * p[j + 1][0],
                  (1 - t) * p[j][1] + t * p[j + 1][1]) for j in range(len(p) - 1)]
        pts.append(p[0])
    return pts


def fill_grad(img, pts, c0, c1, v=(0.0, 1.0)):
    """Fill a polygon with a linear gradient. `pts` in 512-space."""
    pw = [(x * SS, y * SS) for x, y in pts]
    xs = [p[0] for p in pw]
    ys = [p[1] for p in pw]
    x0 = max(0, int(math.floor(min(xs))))
    y0 = max(0, int(math.floor(min(ys))))
    x1 = min(W, int(math.ceil(max(xs))) + 1)
    y1 = min(W, int(math.ceil(max(ys))) + 1)
    if x1 <= x0 or y1 <= y0:
        return
    w, h = x1 - x0, y1 - y0
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).polygon([(p[0] - x0, p[1] - y0) for p in pw], fill=255)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = xx * v[0] + yy * v[1]
    t = (t - t.min()) / (t.max() - t.min() + 1e-9)
    arr = np.empty((h, w, 4), np.uint8)
    for k in range(3):
        arr[..., k] = np.clip(c0[k] * (1 - t) + c1[k] * t, 0, 255).astype(np.uint8)
    arr[..., 3] = 255
    tile = Image.fromarray(arr, "RGBA")
    img.paste(tile, (x0, y0), mask)


_SPHERE_CACHE = {}


def sphere_img(R, alpha=255):
    """Pre-render a glossy red sphere of radius R (512-space)."""
    key = (int(round(R * SS)), alpha)
    if key in _SPHERE_CACHE:
        return _SPHERE_CACHE[key]
    n = max(8, int(round(2 * R * SS)))
    c = n / 2.0
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    dx = (xx - c) / c
    dy = (yy - c) / c
    r2 = dx * dx + dy * dy
    inside = r2 <= 1.0
    nz = np.sqrt(np.clip(1.0 - r2, 0.0, 1.0))

    # diffuse shading with light from upper-left-front
    L = np.array([-0.52, -0.62, 0.59], np.float32)
    L /= np.linalg.norm(L)
    diff = np.clip(dx * L[0] + dy * L[1] + nz * L[2], 0.0, 1.0)

    bright = np.array([0xEF, 0x4A, 0x45], np.float32)
    dark = np.array([0x66, 0x08, 0x0C], np.float32)
    col = dark + (bright - dark) * (diff ** 0.85)[..., None]
    # extra warm core near the lit side
    core = np.clip(dx * -0.55 + dy * -0.6, 0.0, 1.0) ** 2
    col += np.array([0.10, 0.02, 0.015], np.float32) * core[..., None]

    # white specular highlight (upper-left)
    Hh = L + np.array([0.0, 0.0, 1.0], np.float32)
    Hh /= np.linalg.norm(Hh)
    spec = np.clip(dx * Hh[0] + dy * Hh[1] + nz * Hh[2], 0.0, 1.0) ** 34
    col = col * (1.0 - 0.92 * spec[..., None]) + 255.0 * 0.92 * spec[..., None]

    # small secondary reflection lower-right
    R2 = np.array([0.30, 0.36, 0.88], np.float32)
    R2 /= np.linalg.norm(R2)
    spec2 = np.clip(dx * R2[0] + dy * R2[1] + nz * R2[2], 0.0, 1.0) ** 12
    col = col * (1.0 - 0.20 * spec2[..., None]) + \
        np.array([255, 226, 220], np.float32) * 0.20 * spec2[..., None]

    # rim darkening
    rim = (1.0 - nz) ** 2
    col *= (1.0 - 0.45 * rim)[..., None]

    arr = np.empty((n, n, 4), np.uint8)
    arr[..., :3] = np.clip(col, 0, 255).astype(np.uint8)
    arr[..., 3] = (inside * alpha).astype(np.uint8)
    im = Image.fromarray(arr, "RGBA")
    _SPHERE_CACHE[key] = im
    return im


# ----------------------------------------------------------------------------
# scene renderer
# ----------------------------------------------------------------------------
class Scene:
    def __init__(self):
        self.img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def composite(self, lyr, pos=(0, 0)):
        self.img.alpha_composite(lyr, pos)
        self.d = ImageDraw.Draw(self.img)

    # -- soft shadows --------------------------------------------------------
    def soft_ellipse(self, center, rx, ry, alpha=80, blur=12,
                     color=(18, 24, 46), angle=0.0):
        pad = blur * 2.3
        x0 = int(round((center[0] - rx - pad) * SS))
        y0 = int(round((center[1] - ry - pad) * SS))
        w = int(round(2 * (rx + pad) * SS))
        h = int(round(2 * (ry + pad) * SS))
        if w <= 2 or h <= 2:
            return
        lyr = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lyr)
        d.ellipse([pad * SS, pad * SS, (pad + 2 * rx) * SS, (pad + 2 * ry) * SS],
                  fill=color + (alpha,))
        lyr = lyr.filter(ImageFilter.GaussianBlur(blur * SS))
        self.composite(lyr, (x0, y0))

    # -- sphere --------------------------------------------------------------
    def sphere(self, center, R, alpha=255, shadow=False):
        if shadow:
            self.soft_ellipse((center[0] + 6, center[1] + R * 0.94),
                              R * 0.98, R * 0.30, alpha=78, blur=10)
        im = sphere_img(R, alpha)
        n = im.width
        left = int(round(center[0] * SS - n / 2.0))
        top = int(round(center[1] * SS - n / 2.0))
        self.composite(im, (left, top))

    # -- cube ----------------------------------------------------------------
    def cube(self, tc, a=115, b=38, h=100, shadow=True, edge=True):
        cx, cy = tc
        if shadow:
            self.soft_ellipse((cx + 8, cy + b + h - 2), a * 1.03, 14 + b * 0.35,
                              alpha=76, blur=13)
        top = [(cx, cy - b), (cx + a, cy), (cx, cy + b), (cx - a, cy)]
        right = [(cx + a, cy), (cx, cy + b), (cx, cy + b + h), (cx + a, cy + h)]
        left = [(cx - a, cy), (cx, cy + b), (cx, cy + b + h), (cx - a, cy + h)]
        fill_grad(self.img, left, LEFT_C0, LEFT_C1, v=(0.18, 1.0))
        fill_grad(self.img, right, RIGHT_C0, RIGHT_C1, v=(0.0, 1.0))
        fill_grad(self.img, top, TOP_C0, TOP_C1, v=(0.0, 1.0))
        self.d = ImageDraw.Draw(self.img)
        if edge:
            segs = [(top[0], top[1]), (top[1], top[2]), (top[2], top[3]),
                    (top[3], top[0]),
                    (top[1], right[3]), (top[2], right[2]), (top[3], left[3]),
                    (right[3], right[2]), (left[3], right[2])]
            self.edge_lines(segs, width=1.7, color=EDGE_C)
        return {"top": top, "right": right, "left": left,
                "tc": (cx, cy), "a": a, "b": b, "h": h}

    def edge_lines(self, segs, width=1.7, color=EDGE_C):
        xs = [p[0] for s in segs for p in s]
        ys = [p[1] for s in segs for p in s]
        m = 6
        x0 = int(round((min(xs) - m) * SS))
        y0 = int(round((min(ys) - m) * SS))
        w = int(round((max(xs) - min(xs) + 2 * m) * SS))
        h = int(round((max(ys) - min(ys) + 2 * m) * SS))
        if w <= 0 or h <= 0:
            return
        lyr = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(lyr)
        off = (-x0, -y0)
        for p0, p1 in segs:
            d.line([(p0[0] * SS + off[0], p0[1] * SS + off[1]),
                    (p1[0] * SS + off[0], p1[1] * SS + off[1])],
                   fill=color, width=max(1, int(round(width * SS))))
        self.composite(lyr, (x0, y0))

    # -- red rod (capsule) ---------------------------------------------------
    def rod(self, p0, p1, width=15, color=RED, tip_alpha=255):
        x0, y0 = p0[0] * SS, p0[1] * SS
        x1, y1 = p1[0] * SS, p1[1] * SS
        d = self.d
        w = width * SS
        # dark outline
        d.line([(x0, y0), (x1, y1)], fill=RED_DARK + (255,), width=int(w + 6 * SS))
        for px, py in ((x0, y0), (x1, y1)):
            r = (w + 6 * SS) / 2.0
            d.ellipse([px - r, py - r, px + r, py + r], fill=RED_DARK + (255,))
        # body
        d.line([(x0, y0), (x1, y1)], fill=color + (255,), width=int(w))
        for px, py in ((x0, y0), (x1, y1)):
            r = w / 2.0
            d.ellipse([px - r, py - r, px + r, py + r], fill=color + (255,))
        # cylindrical highlight (offset toward the light, upper-left)
        vx, vy = x1 - x0, y1 - y0
        ln = math.hypot(vx, vy) + 1e-9
        nx, ny = -vy / ln, vx / ln
        if ny > 0:
            nx, ny = -nx, -ny
        ox, oy = nx * w * 0.26, ny * w * 0.26
        hw = max(1, int(w * 0.30))
        d.line([(x0 + ox, y0 + oy), (x1 + ox, y1 + oy)],
               fill=(0xF3, 0x8C, 0x82, 255), width=hw)
        for px, py in ((x0 + ox, y0 + oy), (x1 + ox, y1 + oy)):
            r = hw / 2.0
            d.ellipse([px - r, py - r, px + r, py + r], fill=(0xF3, 0x8C, 0x82, 255))

    # -- arrows --------------------------------------------------------------
    def arrow(self, pts, width=13, head_len=44, head_w=42, head=True,
              color=RED, dark=RED_DARK, light=RED_LIGHT):
        path = [(x * SS, y * SS) for x, y in pts]
        if len(path) < 2:
            return
        w = width * SS
        d = self.d
        shaft = list(path)
        if head:
            tip = np.array(path[-1], np.float64)
            j = len(path) - 2
            while j > 0 and np.linalg.norm(tip - np.array(path[j])) < 1e-6:
                j -= 1
            tvec = tip - np.array(path[j], np.float64)
            tvec /= (np.linalg.norm(tvec) + 1e-9)
            norm = np.array([-tvec[1], tvec[0]])
            hl = head_len * SS
            hw = head_w * SS
            # keep the shaft behind the arrow head base
            shaft = [tuple(p) for p in path
                     if float(np.dot(tip - np.array(p, np.float64), tvec)) > hl * 0.62]
            if not shaft:
                shaft = [path[0]]
            shaft.append(tuple(tip - tvec * hl * 0.55))

        # outline pass
        d.line(shaft, fill=dark + (255,), width=int(w + 3.4 * SS), joint="curve")
        for px, py in (shaft[0], shaft[-1]):
            r = (w + 3.4 * SS) / 2.0
            d.ellipse([px - r, py - r, px + r, py + r], fill=dark + (255,))
        # body
        d.line(shaft, fill=color + (255,), width=int(w), joint="curve")
        for px, py in (shaft[0], shaft[-1]):
            r = w / 2.0
            d.ellipse([px - r, py - r, px + r, py + r], fill=color + (255,))
        # glossy centre line
        hl_w = max(1, int(w * 0.30))
        d.line(shaft, fill=(0xE9, 0x6B, 0x62, 255), width=hl_w, joint="curve")

        if head:
            tip_p = tuple(tip)
            base_c = tip - tvec * hl
            p1 = tuple(base_c + norm * hw / 2.0)
            p2 = tuple(base_c - norm * hw / 2.0)
            # dark outline triangle (slightly larger)
            bo = tip - tvec * hl * 1.10
            q1 = tuple(bo + norm * hw * 0.58)
            q2 = tuple(bo - norm * hw * 0.58)
            d.polygon([tip_p, q1, q2], fill=dark + (255,))
            d.polygon([tip_p, p1, p2], fill=color + (255,))

    # -- curved blue band ----------------------------------------------------
    def band(self, ctrl, profile=(0.22, 0.75, 1.0, 0.78, 0.30), width=48,
             color=BAND_BLUE):
        path = bez(ctrl, 56)
        path = [(x * SS, y * SS) for x, y in path]
        n = len(path)
        prof = np.interp(np.linspace(0, 1, n), np.linspace(0, 1, len(profile)),
                         np.array(profile, np.float64)) * width * SS
        self._taper(path, prof + 7 * SS, BAND_DARK)
        self._taper(path, prof, color)
        self._taper(path, prof * 0.36, BAND_SHEEN)

    def _taper(self, path, widths, color):
        d = self.d
        for i in range(len(path) - 1):
            w = (widths[i] + widths[i + 1]) / 2.0
            d.line([path[i], path[i + 1]], fill=color + (255,),
                   width=max(1, int(round(w))), joint="curve")
            r = w / 2.0
            d.ellipse([path[i][0] - r, path[i][1] - r,
                       path[i][0] + r, path[i][1] + r], fill=color + (255,))
        r = widths[-1] / 2.0
        d.ellipse([path[-1][0] - r, path[-1][1] - r,
                   path[-1][0] + r, path[-1][1] + r], fill=color + (255,))

    # -- staircase of little cubes ------------------------------------------
    def stairs(self, x0, ground, heights=(1, 2, 3), a=46, b=15, h=50,
               shadow=True):
        if shadow:
            cx = x0 + (len(heights) - 1) * a / 2.0
            self.soft_ellipse((cx + 10, ground + (len(heights) - 1) * b + 4),
                              (len(heights) - 1) * a + a * 1.05, 22,
                              alpha=74, blur=13)
        tops = []
        for i, k in enumerate(heights):
            tcx = x0 + i * a
            g = ground + i * b
            tcy = g - b - k * h
            for j in range(k - 1, -1, -1):
                self.cube((tcx, tcy + j * h), a=a, b=b, h=h,
                          shadow=False, edge=True)
            tops.append((tcx, tcy))
        return tops

    # -- faint cross (the "not inside" marker) ------------------------------
    def cross_mark(self, center, size=54, color=(0x7E, 0x2F, 0x93),
                   alpha=95, width=6):
        cx, cy = center
        h = size / 2.0
        segs = [((cx - h, cy - h), (cx + h, cy + h)),
                ((cx + h, cy - h), (cx - h, cy + h))]
        bb = (int((size + width * 2) * SS) + 4, ) * 2
        pad = (size + width * 2) * SS / 2.0
        x0 = int(round(cx * SS - pad))
        y0 = int(round(cy * SS - pad))
        lyr = Image.new("RGBA", bb, (0, 0, 0, 0))
        d = ImageDraw.Draw(lyr)
        for p0, p1 in segs:
            d.line([(p0[0] * SS - x0, p0[1] * SS - y0),
                    (p1[0] * SS - x0, p1[1] * SS - y0)],
                   fill=color + (alpha,), width=int(width * SS))
        self.composite(lyr, (x0, y0))


# ============================================================================
# POSITIONAL SCENES  (22)
# ============================================================================
def s_pos_above(s):
    s.cube((255, 320), 115, 38, 100)
    s.sphere((210, 180), 58)


def s_pos_across(s):
    s.cube((152, 302), 82, 28, 76)
    s.cube((364, 302), 82, 28, 76)
    s.rod((104, 294), (414, 294), 15)


def s_pos_against(s):
    s.cube((232, 300), 112, 37, 98)
    s.rod((292, 146), (402, 438), 16)


def s_pos_among(s):
    s.cube((256, 292), 62, 21, 56)
    s.cube((148, 354), 62, 21, 56)
    s.cube((360, 354), 62, 21, 56)
    s.sphere((256, 372), 51)
    s.cube((178, 428), 62, 21, 56)
    s.cube((332, 428), 62, 21, 56)


def s_pos_around(s):
    cx, cy = 256, 275
    a, b, h = 105, 35, 94
    ring = (256, 392, 188, 50)
    back, front = [], []
    for k in range(8):
        ang = math.radians(k * 45 + 22.5)
        p = (ring[0] + ring[2] * math.cos(ang), ring[1] + ring[3] * math.sin(ang))
        (back if math.sin(ang) < -0.05 else front).append(p)
    for p in back:
        s.sphere(p, 44)
    s.cube((cx, cy), a, b, h)
    for p in front:
        s.sphere(p, 44, shadow=True)


def s_pos_behind(s):
    s.sphere((192, 252), 60)
    s.cube((288, 316), 112, 37, 98)


def s_pos_below(s):
    s.cube((255, 235), 112, 37, 94)
    s.sphere((255, 447), 52, shadow=True)


def s_pos_beneath(s):
    s.cube((255, 235), 112, 37, 94)
    s.sphere((255, 420), 54)


def s_pos_under(s):
    s.cube((240, 235), 112, 37, 94)
    s.sphere((315, 406), 52)


def s_pos_underneath(s):
    s.sphere((250, 414), 54)
    s.cube((255, 240), 112, 37, 94)


def s_pos_beside(s):
    s.cube((200, 300), 112, 37, 98)
    s.sphere((382, 394), 54, shadow=True)


def s_pos_by(s):
    s.cube((238, 285), 112, 37, 98)
    s.sphere((346, 404), 54, shadow=True)


def s_pos_between(s):
    s.cube((140, 290), 82, 28, 76)
    s.cube((376, 290), 82, 28, 76)
    s.sphere((258, 386), 51, shadow=True)


def s_pos_in(s):
    s.cube((250, 295), 112, 37, 96)
    s.sphere((250, 345), 46, alpha=GLASS_SPHERE_ALPHA)


def s_pos_in_front_of(s):
    s.cube((262, 285), 112, 37, 98)
    s.sphere((176, 402), 58, shadow=True)


def s_pos_inside(s):
    s.cube((250, 295), 112, 37, 96)
    s.sphere((250, 370), 56, alpha=GLASS_SPHERE_ALPHA)


def s_pos_near(s):
    s.cube((190, 300), 112, 37, 98)
    s.sphere((398, 390), 52, shadow=True)


def s_pos_next_to(s):
    s.cube((195, 300), 112, 37, 98)
    s.sphere((360, 392), 54, shadow=True)


def s_pos_on(s):
    s.cube((255, 305), 112, 37, 98)
    s.soft_ellipse((242, 318), 56, 19, alpha=62, blur=8)
    s.sphere((238, 259), 54)


def s_pos_outside(s):
    s.cube((195, 295), 104, 35, 92)
    s.cross_mark((214, 356), 58)
    s.sphere((392, 392), 52, shadow=True)


def s_pos_over(s):
    s.cube((255, 330), 112, 37, 98)
    s.soft_ellipse((255, 340), 66, 22, alpha=52, blur=9)
    s.sphere((250, 150), 52)


def s_pos_through(s):
    s.cube((250, 300), 112, 37, 98)
    s.rod((92, 336), (416, 304), 16)


# ============================================================================
# DIRECTIONAL SCENES  (17)
# ============================================================================
def s_dir_across(s):
    s.band([(112, 452), (190, 430), (270, 330), (356, 208)])
    s.arrow(bez([(410, 240), (330, 285), (240, 340), (168, 372)]),
            width=13, head_len=42, head_w=40)
    s.sphere((330, 158), 49)


def s_dir_against(s):
    # sphere pressed flat against the right face of the block, two arrows
    # pushing it from the right (heads point left, into the block)
    s.cube((205, 300), 100, 33, 92)
    s.sphere((346, 336), 46)
    s.arrow(bez([(470, 302), (432, 311), (396, 322)]),
            width=12, head_len=30, head_w=32)
    s.arrow(bez([(470, 382), (432, 372), (396, 358)]),
            width=12, head_len=30, head_w=32)


def s_dir_along(s):
    s.band([(150, 452), (120, 330), (210, 250), (292, 146)])
    s.arrow(bez([(248, 416), (218, 320), (278, 255), (322, 198)]),
            width=13, head_len=42, head_w=40)
    s.sphere((300, 120), 46)


def s_dir_around(s):
    cx, cy, rx, ry = 250, 382, 172, 50
    back = [(cx + rx * math.cos(math.radians(t)),
             cy + ry * math.sin(math.radians(t)))
            for t in np.linspace(190, 350, 40)]
    front = [(cx + rx * math.cos(math.radians(t)),
              cy + ry * math.sin(math.radians(t)))
             for t in np.linspace(10, 168, 46)]
    s.arrow(back, width=12, head=False)
    s.cube((250, 280), 100, 33, 92)
    s.arrow(front, width=12, head_len=40, head_w=38)
    s.sphere((382, 398), 44, shadow=True)


def s_dir_away_from(s):
    s.cube((400, 175), 52, 17, 48)
    s.sphere((168, 395), 60, shadow=True)
    s.arrow(bez([(320, 300), (285, 322), (255, 345), (232, 362)]),
            width=12, head_len=38, head_w=36)


def s_dir_down(s):
    s.stairs(150, 395, heights=(3, 2, 1))
    s.sphere((320, 414), 44, shadow=True)
    s.arrow(bez([(160, 150), (235, 205), (290, 285), (326, 358)]),
            width=13, head_len=42, head_w=40)


def s_dir_from(s):
    s.cube((378, 190), 50, 17, 46)
    s.sphere((190, 382), 58, shadow=True)
    s.arrow(bez([(352, 244), (318, 272), (274, 306), (242, 332)]),
            width=12, head_len=38, head_w=36)


def s_dir_into(s):
    s.cube((240, 300), 102, 34, 94)
    s.sphere((240, 352), 44, alpha=GLASS_SPHERE_ALPHA)
    s.arrow(bez([(415, 330), (470, 205), (370, 180), (300, 256)]),
            width=13, head_len=42, head_w=40)


def s_dir_off_of(s):
    s.cube((225, 285), 104, 35, 94)
    s.sphere((335, 400), 46, shadow=True)
    s.arrow(bez([(300, 205), (400, 225), (442, 320), (392, 378)]),
            width=13, head_len=42, head_w=40)


def s_dir_onto(s):
    s.cube((250, 315), 104, 35, 94)
    s.soft_ellipse((240, 328), 52, 18, alpha=58, blur=8)
    s.sphere((238, 274), 48)
    s.arrow(bez([(425, 440), (490, 325), (430, 205), (340, 240)]),
            width=13, head_len=42, head_w=40)


def s_dir_out_of(s):
    s.cube((190, 285), 98, 32, 90)
    s.sphere((190, 330), 36, alpha=85)
    s.sphere((368, 392), 48, shadow=True)
    s.arrow(bez([(240, 290), (320, 175), (420, 285), (392, 348)]),
            width=13, head_len=42, head_w=40)


def s_dir_over(s):
    s.cube((255, 320), 104, 35, 94)
    s.arrow(bez([(118, 392), (150, 215), (350, 215), (398, 382)]),
            width=13, head_len=42, head_w=40)
    s.sphere((240, 150), 50)


def s_dir_through(s):
    s.cube((225, 280), 104, 34, 92)
    hole = (173, 343)
    lyr = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(lyr)
    d.ellipse([(hole[0] - 30) * SS, (hole[1] - 36) * SS,
               (hole[0] + 30) * SS, (hole[1] + 36) * SS],
              fill=(0x10, 0x1B, 0x45, 255))
    s.composite(lyr)
    s.sphere(hole, 23)
    s.arrow(bez([(58, 316), (105, 334), (138, 342)]),
            width=12, head_len=34, head_w=32)


def s_dir_to(s):
    s.cube((405, 175), 50, 16, 46)
    s.sphere((175, 360), 52, shadow=True)
    s.arrow(bez([(220, 330), (270, 300), (320, 265), (352, 240)]),
            width=12, head_len=36, head_w=34)


def s_dir_towards(s):
    s.cube((405, 175), 50, 16, 46)
    s.sphere((168, 372), 52, shadow=True)
    s.arrow(bez([(212, 342), (250, 316), (284, 295), (304, 282)]),
            width=12, head_len=36, head_w=34)


def s_dir_under(s):
    s.cube((245, 275), 104, 34, 92)
    s.arrow(bez([(110, 395), (180, 470), (330, 480), (400, 390)]),
            width=13, head_len=42, head_w=40)
    s.sphere((300, 440), 42)


def s_dir_up(s):
    s.stairs(185, 430, heights=(1, 2, 3))
    s.arrow(bez([(108, 440), (155, 380), (190, 330), (218, 298)]),
            width=13, head_len=42, head_w=40)
    s.sphere((277, 254), 44)


# ============================================================================
SCENES = {
    "pos_above": s_pos_above,
    "pos_across": s_pos_across,
    "pos_against": s_pos_against,
    "pos_among": s_pos_among,
    "pos_around": s_pos_around,
    "pos_behind": s_pos_behind,
    "pos_below": s_pos_below,
    "pos_beneath": s_pos_beneath,
    "pos_beside": s_pos_beside,
    "pos_between": s_pos_between,
    "pos_by": s_pos_by,
    "pos_in": s_pos_in,
    "pos_in_front_of": s_pos_in_front_of,
    "pos_inside": s_pos_inside,
    "pos_near": s_pos_near,
    "pos_next_to": s_pos_next_to,
    "pos_on": s_pos_on,
    "pos_outside": s_pos_outside,
    "pos_over": s_pos_over,
    "pos_through": s_pos_through,
    "pos_under": s_pos_under,
    "pos_underneath": s_pos_underneath,
    "dir_across": s_dir_across,
    "dir_against": s_dir_against,
    "dir_along": s_dir_along,
    "dir_around": s_dir_around,
    "dir_away_from": s_dir_away_from,
    "dir_down": s_dir_down,
    "dir_from": s_dir_from,
    "dir_into": s_dir_into,
    "dir_off_of": s_dir_off_of,
    "dir_onto": s_dir_onto,
    "dir_out_of": s_dir_out_of,
    "dir_over": s_dir_over,
    "dir_through": s_dir_through,
    "dir_to": s_dir_to,
    "dir_towards": s_dir_towards,
    "dir_under": s_dir_under,
    "dir_up": s_dir_up,
}

# The task spec names these dir_into / dir_onto; the UI content uses the
# "in_to" / "on_to" spellings. Emit byte-identical aliases so both keys resolve
# to a real R.drawable.prep_<key>.
ALIASES = {"dir_in_to": "dir_into", "dir_on_to": "dir_onto"}


def render_one(key):
    fn = SCENES.get(key)
    if fn is None:
        raise KeyError("unknown scene: " + key)
    s = Scene()
    fn(s)
    out = s.img.resize((SIZE, SIZE), Image.LANCZOS)
    return out


def _load_font(size):
    for path in ("/System/Library/Fonts/Supplemental/Arial.ttf",
                 "/System/Library/Fonts/Helvetica.ttc",
                 "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default(size=size)


def build_preview(out_path, keys=None):
    keys = keys or list(SCENES.keys())
    cols, cell_w, cell_h, im_size = 5, 304, 330, 280
    rows = (len(keys) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), (255, 255, 255))
    d = ImageDraw.Draw(sheet)
    font = _load_font(22)
    for i, key in enumerate(keys):
        r, c = divmod(i, cols)
        x = c * cell_w + (cell_w - im_size) // 2
        y = r * cell_h + 10
        path = os.path.join(DRAWABLE_DIR, "prep_%s.png" % key)
        im = Image.open(path).convert("RGBA").resize((im_size, im_size), Image.LANCZOS)
        bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
        bg.alpha_composite(im)
        sheet.paste(bg.convert("RGB"), (x, y))
        cap = key
        tw = d.textlength(cap, font=font)
        d.text((c * cell_w + (cell_w - tw) / 2.0, y + im_size + 10), cap,
               fill=(38, 42, 50), font=font)
    sheet.save(out_path)
    return out_path


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="*", default=None)
    ap.add_argument("--preview-only", action="store_true")
    args = ap.parse_args(argv)

    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    os.makedirs(DOCS_DIR, exist_ok=True)
    keys = args.only or list(SCENES.keys())

    if not args.preview_only:
        for key in keys:
            im = render_one(key)
            im.save(os.path.join(DRAWABLE_DIR, "prep_%s.png" % key))
            print("wrote prep_%s.png" % key)
        for alias, target in ALIASES.items():
            src = os.path.join(DRAWABLE_DIR, "prep_%s.png" % target)
            dst = os.path.join(DRAWABLE_DIR, "prep_%s.png" % alias)
            shutil.copyfile(src, dst)
            print("wrote prep_%s.png (alias of %s)" % (alias, target))

    build_preview(os.path.join(DOCS_DIR, "preview.png"))
    print("wrote docs/prep-art/preview.png")


if __name__ == "__main__":
    main()
