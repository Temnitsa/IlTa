#!/usr/bin/env python3
"""Deterministic generator for the IlTa preposition illustrations.

Renders 39 preposition scenes (22 positional + 17 directional) plus 2 aliases
in one consistent isometric style:

  * true 30 degree isometric projection (top face rhombus 1.732 : 1, vertical
    edges equal to the cube edge -- no "squashed" cuboids),
  * glossy deep-blue cubes with a linear gradient per face and thin light edges,
  * glossy scarlet spheres with a radial gradient and a soft specular highlight,
  * scarlet arrows / dashed trajectories with round caps,
  * soft analytic shadows (radial gradient, squashed ellipses).

Everything is drawn with Pillow + numpy at 3x supersampling and downsampled
with LANCZOS to 1000x700 RGBA PNGs (10:7, transparent background).

Outputs:
  app/src/main/res/drawable-nodpi/prep_<key>.png   (41 files)
  docs/prep-art/preview.png                        (contact sheet)

Usage:
  python3 tools/gen_prep_art.py              # everything
  python3 tools/gen_prep_art.py --only pos_above dir_over
  python3 tools/gen_prep_art.py --preview-only

No network access, deterministic (no RNG).
"""
import argparse
import math
import os
import shutil

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DRAWABLE_DIR = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
DOCS_DIR = os.path.join(ROOT, "docs", "prep-art")

SS = 3                       # supersampling factor
W, H = 1000, 700             # final canvas (10:7)
CW, CH = W * SS, H * SS

COS30 = 0.8660254037844386
SIN30 = 0.5

# ---------------------------------------------------------------------------
# palette (spec colours)
# ---------------------------------------------------------------------------
TOP_C0 = (0x5F, 0x78, 0xFF)
TOP_C1 = (0x44, 0x5A, 0xE8)
LEFT_C0 = (0x18, 0x27, 0x9F)
LEFT_C1 = (0x11, 0x1C, 0x86)
RIGHT_C0 = (0x22, 0x33, 0xC4)
RIGHT_C1 = (0x1A, 0x27, 0xAB)
EDGE_C = (0x97, 0xA5, 0xFF)

RED = (0xE0, 0x24, 0x24)
RED_D = (0x8C, 0x0F, 0x0F)
RED_L = (0xFF, 0x6A, 0x5A)

BAND_C0 = (0x3A, 0x6B, 0xD8)
BAND_C1 = (0x17, 0x2F, 0x7E)
BAND_SHEEN = (0x6E, 0x99, 0xEE)
BAND_EDGE = (0x10, 0x1F, 0x5A)

SHADOW_C = (25, 30, 55)


def _i(v):
    return int(round(v * SS))


# ---------------------------------------------------------------------------
# canvas / compositing helpers
# ---------------------------------------------------------------------------
class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))

    def comp(self, lyr, x, y, opacity=1.0):
        if opacity < 1.0:
            a = lyr.getchannel("A").point(lambda v: int(round(v * opacity)))
            lyr = lyr.copy()
            lyr.putalpha(a)
        self.img.alpha_composite(lyr, (int(round(x)), int(round(y))))


def _bbox_layer(pts, pad=3):
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    x0 = max(0, int(math.floor(min(xs) * SS)) - pad)
    y0 = max(0, int(math.floor(min(ys) * SS)) - pad)
    x1 = min(CW, int(math.ceil(max(xs) * SS)) + pad)
    y1 = min(CH, int(math.ceil(max(ys) * SS)) + pad)
    if x1 <= x0 or y1 <= y0:
        return None
    return x0, y0, x1 - x0, y1 - y0


def poly_grad(cv, pts, c0, c1, v=(0.45, 0.89), alpha=1.0):
    """Fill polygon `pts` (design coords) with a linear gradient."""
    box = _bbox_layer(pts)
    if box is None:
        return
    x0, y0, w, h = box
    P = [(p[0] * SS, p[1] * SS) for p in pts]
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).polygon([(p[0] - x0, p[1] - y0) for p in P], fill=255)
    if alpha < 1.0:
        mask = mask.point(lambda t: int(round(t * alpha)))
    ax = np.arange(w, dtype=np.float32) + x0
    ay = np.arange(h, dtype=np.float32) + y0
    proj = [p[0] * v[0] + p[1] * v[1] for p in P]
    tmin, tmax = min(proj), max(proj)
    T = np.clip(((ax[None, :] * v[0] + ay[:, None] * v[1]) - tmin) / (tmax - tmin + 1e-6), 0, 1)
    arr = np.empty((h, w, 4), np.uint8)
    for k in range(3):
        arr[..., k] = np.clip(c0[k] * (1 - T) + c1[k] * T, 0, 255).astype(np.uint8)
    arr[..., 3] = 255
    tile = Image.fromarray(arr, "RGBA")
    tile.putalpha(mask)
    cv.comp(tile, x0, y0)


def stroke(cv, polylines, width, color, alpha=1.0, closed_caps=True):
    """Draw thick polylines (design coords) with round caps."""
    pts = [p for pl in polylines for p in pl]
    if not pts:
        return
    box = _bbox_layer(pts, pad=int(width * SS) + 3)
    if box is None:
        return
    x0, y0, w, h = box
    lyr = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(lyr)
    fill = tuple(color) + (int(round(255 * alpha)),)
    lw = max(1, int(round(width * SS)))
    r = lw / 2.0
    for pl in polylines:
        if len(pl) < 2:
            continue
        P = [(p[0] * SS - x0, p[1] * SS - y0) for p in pl]
        d.line(P, fill=fill, width=lw, joint="curve")
        if closed_caps:
            for p in (P[0], P[-1]):
                d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=fill)
    cv.comp(lyr, x0, y0)


# ---------------------------------------------------------------------------
# sprites: sphere + shadow
# ---------------------------------------------------------------------------
_SPHERE_CACHE = {}


def sphere_sprite(R, dim=0.0):
    key = (round(R, 2), round(dim, 3))
    if key in _SPHERE_CACHE:
        return _SPHERE_CACHE[key]
    rad = R * SS
    n = int(math.ceil(2 * rad)) + 4
    c = n / 2.0
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    u = (xx - c) / rad
    v = (yy - c) / rad
    r2 = u * u + v * v
    inside = r2 <= 1.0
    nz = np.sqrt(np.clip(1.0 - r2, 0.0, 1.0))

    L = np.array([-0.42, -0.52, 0.74], np.float32)
    L /= np.linalg.norm(L)
    lam = np.clip(u * L[0] + v * L[1] + nz * L[2], 0.0, 1.0)
    t = lam ** 1.02

    stops = ((0x62, 0x08, 0x08), (0xC8, 0x16, 0x16), (0xFF, 0x8C, 0x76))
    pos = (0.0, 0.48, 1.0)
    col = np.stack([np.interp(t, pos, [s[k] for s in stops]) for k in range(3)],
                   axis=-1).astype(np.float32)

    # warm core toward the light
    core = np.exp(-(((u + 0.26) ** 2 + (v + 0.30) ** 2) / 0.24))
    col += np.array([22, 8, 4], np.float32) * core[..., None]

    # specular: small hot spot + soft halo (glossy, not a white blob)
    hs = np.exp(-(((u + 0.33) ** 2 + (v + 0.38) ** 2) / 0.0075))
    hh_ = np.exp(-(((u + 0.31) ** 2 + (v + 0.36) ** 2) / 0.026))
    hl = np.clip(0.60 * hs + 0.24 * hh_, 0.0, 1.0)
    col = col * (1 - hl[..., None]) + np.array([255, 247, 243], np.float32) * hl[..., None]

    # wide glossy band over the lit side
    hb = np.exp(-(((u + 0.14) ** 2 / 0.20 + (v + 0.40) ** 2) / 0.26)) * 0.16
    col = col * (1 - hb[..., None]) + np.array([255, 186, 172], np.float32) * hb[..., None]

    # faint bounce light from below-right
    br = np.exp(-(((u - 0.10) ** 2 + (v - 0.80) ** 2) / 0.070)) * 0.20
    col = col * (1 - br[..., None]) + np.array([255, 158, 136], np.float32) * br[..., None]

    # darkened rim
    rim = np.clip((r2 - 0.76) / 0.24, 0.0, 1.0)
    col *= (1.0 - 0.34 * rim ** 1.5)[..., None]

    if dim > 0:
        col *= (1.0 - 0.16 * dim)
        col = col * (1.0 - 0.32 * dim) + np.array([0x2A, 0x39, 0xC0], np.float32) * 0.32 * dim

    arr = np.zeros((n, n, 4), np.uint8)
    arr[..., :3] = np.clip(col, 0, 255).astype(np.uint8)
    arr[..., 3] = (inside * 255).astype(np.uint8)
    im = Image.fromarray(arr, "RGBA")
    _SPHERE_CACHE[key] = im
    return im


_SHADOW_CACHE = {}


def shadow_sprite(rx, ry, alpha=0.40):
    key = (round(rx, 1), round(ry, 1), round(alpha, 3))
    if key in _SHADOW_CACHE:
        return _SHADOW_CACHE[key]
    w = int(math.ceil(2 * rx * SS)) + 4
    h = int(math.ceil(2 * ry * SS)) + 4
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = (xx - w / 2.0) / (rx * SS)
    v = (yy - h / 2.0) / (ry * SS)
    r2 = u * u + v * v
    a = np.exp(-2.5 * r2)
    e = math.exp(-2.5)
    a = np.clip((a - e) / (1.0 - e), 0.0, 1.0) ** 1.25
    arr = np.zeros((h, w, 4), np.uint8)
    arr[..., :3] = SHADOW_C
    arr[..., 3] = (a * alpha * 255.0).astype(np.uint8)
    im = Image.fromarray(arr, "RGBA")
    _SHADOW_CACHE[key] = im
    return im


def shadow(cv, x, y, rx, ry, alpha=0.40):
    sp = shadow_sprite(rx, ry, alpha)
    cv.comp(sp, x * SS - sp.width / 2.0, y * SS - sp.height / 2.0)


def sphere(cv, x, y, R, contact=False, dim=0.0, squash=1.0, fa_shadow=0.40):
    if contact:
        shadow(cv, x + 2, y + R * 0.94, R * 1.02, R * 0.34, fa_shadow)
    sp = sphere_sprite(R, dim)
    if squash != 1.0:
        sp = sp.resize((sp.width, max(1, int(round(sp.height * squash)))), Image.LANCZOS)
    cv.comp(sp, x * SS - sp.width / 2.0, y * SS - sp.height / 2.0)


# ---------------------------------------------------------------------------
# solids
# ---------------------------------------------------------------------------
def iso_xy(cx, cy, xi, yi):
    """Project footprint coords (xi along +x iso axis, yi along +y) to screen.

    (cx, cy) is the screen position of footprint centre (0, 0)."""
    return (cx + COS30 * (xi - yi), cy + SIN30 * (xi + yi))


def soft_radial(cv, x, y, rx, ry, color, alpha=0.4, power=2.0):
    """Soft elliptical glow / tinted pool in `color`."""
    w = int(math.ceil(2 * rx * SS)) + 4
    h = int(math.ceil(2 * ry * SS)) + 4
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = (xx - w / 2.0) / (rx * SS)
    v = (yy - h / 2.0) / (ry * SS)
    r2 = u * u + v * v
    a = np.clip(1.0 - r2, 0.0, 1.0) ** power
    arr = np.zeros((h, w, 4), np.uint8)
    arr[..., :3] = color
    arr[..., 3] = (a * alpha * 255.0).astype(np.uint8)
    cv.comp(Image.fromarray(arr, "RGBA"), x * SS - w / 2.0, y * SS - h / 2.0)


def box_top(cv, cx, cy, a, b, h, alpha=1.0, shadow_a=0.0, edge_alpha=0.55,
            glass=False, top_c0=TOP_C0, top_c1=TOP_C1, glass_alpha=0.40,
            lift=0.0):
    """Isometric cuboid whose TOP-FACE CENTRE is (cx, cy), extruded down h.

    a = footprint size along the +x iso axis, b = along +y, h = height.
    Screen offsets: +x -> (cos30, sin30), +y -> (-cos30, sin30), +z -> (0,-1).
    """
    u = np.array([COS30 * a, SIN30 * a], np.float64)
    v = np.array([-COS30 * b, SIN30 * b], np.float64)
    C = np.array([cx, cy], np.float64)
    Q0 = C - (u + v) / 2.0          # back vertex
    Q1 = C + (u - v) / 2.0          # right vertex
    Q2 = C + (u + v) / 2.0          # front vertex
    Q3 = C + (-u + v) / 2.0         # left vertex
    down = np.array([0.0, h])
    Q1d, Q2d, Q3d = Q1 + down, Q2 + down, Q3 + down

    if shadow_a > 0:
        rx = COS30 * (a + b) / 2.0 * 1.02
        ry = SIN30 * (a + b) / 2.0 * 0.82
        shadow(cv, cx + (a - b) * 0.10, cy + h + lift, rx, ry, shadow_a)

    fa = alpha if not glass else 1.0
    ga = glass_alpha if glass else 1.0
    # right + left faces first, then the top face on top of the shared edges
    poly_grad(cv, [Q1, Q2, Q2d, Q1d], RIGHT_C0, RIGHT_C1, v=(0.10, 0.99), alpha=ga * fa)
    poly_grad(cv, [Q3, Q2, Q2d, Q3d], LEFT_C0, LEFT_C1, v=(0.0, 1.0), alpha=ga * fa)
    poly_grad(cv, [Q0, Q1, Q2, Q3], top_c0, top_c1, v=(0.42, 0.91),
              alpha=(ga * 0.88 if glass else 1.0) * fa)

    stroke(cv, [
        [Q0, Q1], [Q1, Q2], [Q2, Q3], [Q3, Q0],
        [Q1, Q1d], [Q2, Q2d], [Q3, Q3d],
        [Q1d, Q2d], [Q2d, Q3d],
    ], 2.6, EDGE_C, alpha=edge_alpha)


def cube(cv, cx, cy, s, alpha=1.0, glass=False, shadow_a=0.36, edge_alpha=0.55,
         glass_alpha=0.40, lift=0.0):
    box_top(cv, cx, cy, s, s, s, alpha=alpha, shadow_a=shadow_a,
            edge_alpha=edge_alpha, glass=glass, glass_alpha=glass_alpha, lift=lift)


def bar(cv, p0, p1, thick=34, alpha=1.0, color=RED, shade=RED_D, gloss=RED_L):
    """Glossy red capsule (plank / rod)."""
    p0 = np.array(p0, np.float64)
    p1 = np.array(p1, np.float64)
    d = p1 - p0
    ln = np.linalg.norm(d)
    if ln < 1e-6:
        return
    d = d / ln
    n = np.array([-d[1], d[0]])
    if n[1] > 0:
        n = -n
    stroke(cv, [[tuple(p0), tuple(p1)]], thick, color, alpha=alpha)
    # rounded dark underside
    stroke(cv, [[tuple(p0 - n * thick * 0.26 + d * thick * 0.42),
                 tuple(p1 - n * thick * 0.26 - d * thick * 0.42)]],
           thick * 0.42, shade, alpha=alpha * 0.55)
    # specular sheen along the top
    stroke(cv, [[tuple(p0 + n * thick * 0.22 + d * thick * 0.36),
                 tuple(p1 + n * thick * 0.22 - d * thick * 0.36)]],
           thick * 0.24, gloss, alpha=alpha * 0.65)
    # bright rim
    stroke(cv, [[tuple(p0 + n * thick * 0.40 + d * thick * 0.30),
                 tuple(p1 + n * thick * 0.40 - d * thick * 0.30)]],
           max(2.0, thick * 0.07), (255, 210, 200), alpha=alpha * 0.55)


def table(cv, cx, cy, s, leg_h, th=30, leg_w=34, mid=None, shadow_a=0.34,
          edge_alpha=0.50):
    """Table: slab on four legs. `mid` is drawn between the back legs and the
    front leg (so the front leg overlaps whatever is under the table)."""
    hh = SIN30 * s
    e = s / 2.0 - leg_w / 2.0 - 4.0
    legs = {}
    for sx in (-1, 1):
        for sy in (-1, 1):
            legs[(sx, sy)] = iso_xy(cx, cy, sx * e, sy * e)
    # back, right, left legs go behind the under-table content
    for k in ((-1, -1), (1, -1), (-1, 1)):
        P = legs[k]
        box_top(cv, P[0], P[1] + th, leg_w, leg_w, leg_h, shadow_a=0.0,
                edge_alpha=edge_alpha * 0.85)
    if mid:
        mid()
    box_top(cv, cx, cy, s, s, th, shadow_a=shadow_a, edge_alpha=edge_alpha)
    # the near leg is drawn last: it cuts in front of the ball
    P = legs[(1, 1)]
    box_top(cv, P[0], P[1] + th, leg_w, leg_w, leg_h, shadow_a=0.0,
            edge_alpha=edge_alpha * 0.85)
    return legs


def staircase(cv, base_x, base_y, steps=3, tread=120, width=120, rise=100,
              axis="x", shadow_a=0.30):
    """Stepped solid receding up-left (axis='x') or up-right (axis='y').

    Returns the top-face centres of the steps, ordered from the lowest step to
    the highest one."""
    if axis == "x":
        off = np.array([-COS30 * tread, -SIN30 * tread])
    else:
        off = np.array([COS30 * tread, -SIN30 * tread])
    tops = []
    for j in range(steps - 1, -1, -1):
        h = (j + 1) * rise
        fx = base_x + off[0] * j
        fy = base_y + off[1] * j
        a, b = (tread, width) if axis == "x" else (width, tread)
        box_top(cv, fx, fy - h, a, b, h, shadow_a=0.0, edge_alpha=0.45)
        tops.append((j, fx, fy - h))
    if shadow_a > 0:
        shadow(cv, base_x + (off[0] * (steps - 1)) / 2.0,
               base_y + (off[1] * (steps - 1)) / 2.0,
               tread * (steps * 0.78), tread * 0.42, shadow_a)
    return [(x, y) for _, x, y in sorted(tops)]


# ---------------------------------------------------------------------------
# paths, dashes, arrows, ribbon
# ---------------------------------------------------------------------------
def bez(ctrl, n=90):
    pts = [np.array(c, np.float64) for c in ctrl]
    m = len(pts) - 1
    out = []
    for i in range(n + 1):
        t = i / n
        p = [q.copy() for q in pts]
        for _ in range(m):
            p = [(1 - t) * p[j] + t * p[j + 1] for j in range(len(p) - 1)]
        out.append(tuple(p[0]))
    return out


def _resample(pts, step=1.5):
    out = []
    for i in range(len(pts) - 1):
        p0 = np.array(pts[i], np.float64)
        p1 = np.array(pts[i + 1], np.float64)
        d = np.linalg.norm(p1 - p0)
        n = max(1, int(math.ceil(d / step)))
        for k in range(n):
            out.append(tuple(p0 + (p1 - p0) * (k / n)))
    out.append(tuple(np.array(pts[-1], np.float64)))
    return out


def _cumlen(pts):
    cum = [0.0]
    for i in range(1, len(pts)):
        cum.append(cum[-1] + math.dist(pts[i - 1], pts[i]))
    return cum


def subpath(pts, cum, s0, s1):
    """Slice polyline between arc lengths s0..s1."""
    out = []
    for i, p in enumerate(pts):
        if s0 <= cum[i] <= s1:
            out.append(p)
    for i in range(1, len(pts)):
        a, b = cum[i - 1], cum[i]
        for s in (s0, s1):
            if a < s < b:
                t = (s - a) / (b - a + 1e-9)
                out.append((pts[i - 1][0] + (pts[i][0] - pts[i - 1][0]) * t,
                            pts[i - 1][1] + (pts[i][1] - pts[i - 1][1]) * t))
    order = lambda p: None  # noqa: E731  (ordering handled by caller)
    return out


def _slice(pts, cum, s0, s1):
    """Ordered slice of the polyline between arc lengths s0..s1."""
    res = [pts[0]]
    del res[:]
    for i in range(len(pts) - 1):
        a, b = cum[i], cum[i + 1]
        seg_len = b - a
        if seg_len <= 1e-9:
            continue
        lo, hi = max(a, s0), min(b, s1)
        if hi <= lo:
            continue
        p0 = np.array(pts[i])
        p1 = np.array(pts[i + 1])
        t0 = (lo - a) / seg_len
        t1 = (hi - a) / seg_len
        res.append(tuple(p0 + (p1 - p0) * t0))
        res.append(tuple(p0 + (p1 - p0) * t1))
    return res


def dashed(cv, pts, width=10, dash=26, gap=18, color=RED, alpha=1.0,
           head=False, head_len=38, head_w=34, head_alpha=None, taper_ends=True):
    pts = _resample(pts, 1.2)
    cum = _cumlen(pts)
    total = cum[-1]
    if total < 1e-6:
        return
    head_len_eff = head_len if head else 0.0
    stop = max(0.0, total - head_len_eff * 0.45)
    polylines = []
    s = 0.0
    while s < stop:
        e = min(s + dash, stop)
        if e - s > 1.0:
            pl = _slice(pts, cum, s, e)
            if len(pl) >= 2:
                polylines.append(pl)
        s = e + gap
    stroke(cv, polylines, width, color, alpha=alpha, closed_caps=True)
    if head:
        tip = np.array(pts[-1], np.float64)
        j = len(pts) - 2
        while j > 0 and np.linalg.norm(tip - np.array(pts[j])) < 1e-6:
            j -= 1
        tvec = tip - np.array(pts[j])
        tvec /= (np.linalg.norm(tvec) + 1e-9)
        norm = np.array([-tvec[1], tvec[0]])
        base = tip - tvec * head_len
        p1 = base + norm * head_w / 2.0
        p2 = base - norm * head_w / 2.0
        ha = alpha if head_alpha is None else head_alpha
        poly_grad(cv, [tuple(tip), tuple(p1), tuple(p2)], RED_L, RED_D,
                  v=(tvec[0], tvec[1]), alpha=ha)


def arrow(cv, pts, width=13, head_len=40, head_w=36, alpha=1.0):
    """Solid rounded arrow with a glossy sheen and a triangular head."""
    pts = _resample(pts, 1.2)
    cum = _cumlen(pts)
    total = cum[-1]
    tip = np.array(pts[-1], np.float64)
    j = len(pts) - 2
    while j > 0 and np.linalg.norm(tip - np.array(pts[j])) < 1e-6:
        j -= 1
    tvec = tip - np.array(pts[j])
    tvec /= (np.linalg.norm(tvec) + 1e-9)
    shaft = _slice(pts, cum, 0, max(0.0, total - head_len * 0.35))
    stroke(cv, [shaft], width, RED, alpha=alpha)
    # sheen
    off = np.array([-tvec[1], tvec[0]]) * (-width * 0.22)
    sh = [(p[0] + off[0], p[1] + off[1]) for p in shaft]
    stroke(cv, [sh], width * 0.34, RED_L, alpha=alpha * 0.55)
    norm = np.array([-tvec[1], tvec[0]])
    base = tip - tvec * head_len
    p1 = base + norm * head_w / 2.0
    p2 = base - norm * head_w / 2.0
    poly_grad(cv, [tuple(tip), tuple(p1), tuple(p2)], RED_L, RED_D,
              v=(-tvec[0], -tvec[1]), alpha=alpha)


def start_marker(cv, x, y, r=9.0, color=RED):
    stroke(cv, [[(x - r, y), (x + r, y)]], 3.0, color, alpha=0.0)
    cv_ring(cv, x, y, r, color)


def cv_ring(cv, x, y, r, color, width=6.0):
    box = _bbox_layer([(x - r - width, y - r - width), (x + r + width, y + r + width)], pad=3)
    if box is None:
        return
    x0, y0, w, h = box
    lyr = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(lyr)
    d.ellipse([(x - r) * SS - x0, (y - r) * SS - y0,
               (x + r) * SS - x0, (y + r) * SS - y0],
              outline=tuple(color) + (255,), width=int(round(width * SS)))
    cv.comp(lyr, x0, y0)


def ribbon(cv, ctrl, w0, w1, n=80, alpha=1.0, edge=True):
    """Perspective road-like band following a bezier curve."""
    pts = _resample(bez(ctrl, n), 3.0)
    npts = len(pts)
    left, right = [], []
    for i, p in enumerate(pts):
        t = i / (npts - 1.0)
        hw = (w0 * (1 - t) + w1 * t) / 2.0
        if i == 0:
            a = np.array(pts[1]) - np.array(p)
        elif i == npts - 1:
            a = np.array(p) - np.array(pts[-2])
        else:
            a = np.array(pts[i + 1]) - np.array(pts[i - 1])
        a /= (np.linalg.norm(a) + 1e-9)
        nrm = np.array([-a[1], a[0]])
        left.append(tuple(np.array(p) + nrm * hw))
        right.append(tuple(np.array(p) - nrm * hw))
    poly = left + right[::-1]
    poly_grad(cv, poly, BAND_C0, BAND_C1, v=(0.2, 0.98), alpha=alpha)
    if edge:
        stroke(cv, [left, right], 2.2, BAND_EDGE, alpha=alpha * 0.55, closed_caps=False)
        stroke(cv, [[tuple(np.array(p) + (np.array(l) - np.array(p)) * 0.62)
                     for p, l in zip(pts, left)]], 5.0, BAND_SHEEN, alpha=alpha * 0.30)


ROAD_C_FAR = (0x7C, 0x8F, 0xDC)
ROAD_C_NEAR = (0x4E, 0x60, 0xC0)
ROAD_EDGE = (0xA9, 0xB6, 0xF2)


def road(cv, cx, cy, length, width, alpha=0.92):
    """Flat road-like band lying on the ground plane (runs up-right).

    Drawn as a parallelogram in the ground plane: `length` along the -y iso
    axis, `width` across it."""
    L, Wd = length / 2.0, width / 2.0
    q = [iso_xy(cx, cy, xi, yi) for xi, yi in ((Wd, -L), (Wd, L), (-Wd, L), (-Wd, -L))]
    poly_grad(cv, q, ROAD_C_FAR, ROAD_C_NEAR, v=(COS30, SIN30), alpha=alpha)
    stroke(cv, [q[:2], q[2:]], 3.0, ROAD_EDGE, alpha=alpha * 0.55, closed_caps=False)
    # faint shoulder line inside the near edge for depth
    inner = [iso_xy(cx, cy, xi, yi) for xi, yi in
             ((Wd * 0.62, -L * 0.97), (Wd * 0.62, L * 0.97))]
    stroke(cv, [inner], 4.0, (0xC9, 0xD3, 0xF8), alpha=alpha * 0.22, closed_caps=False)


# ---------------------------------------------------------------------------
# scene helpers
# ---------------------------------------------------------------------------
def ball_on(cv, fx, fy, R, fa=0.34, squash=1.0):
    """Ball resting on a horizontal surface at contact point (fx, fy)."""
    shadow(cv, fx + 3, fy + 4, R * 1.00, R * 0.30, fa)
    sphere(cv, fx, fy - R * squash, R, squash=squash)


def glass_glow(cv, x, y, R, alpha=0.16):
    """Warm pool of light where an object shows through a glass face."""
    soft_radial(cv, x, y, R * 1.05, R * 1.05, (255, 104, 92), alpha=alpha, power=1.9)


# ---------------------------------------------------------------------------
# POSITIONAL SCENES (22)
# ---------------------------------------------------------------------------
def s_pos_above(cv):
    """Cube low right, ball hanging high above it: no contact shadow."""
    shadow(cv, 520, 656, 92, 27, 0.28)
    cube(cv, 560, 352, 222, shadow_a=0.26)
    sphere(cv, 520, 123, 84, contact=False)


def s_pos_across(cv):
    """Two cubic supports with a red plank-bridge lying across them."""
    cube(cv, 270, 320, 250, shadow_a=0.32)
    cube(cv, 730, 320, 250, shadow_a=0.32)
    bar(cv, (96, 306), (904, 306), thick=38)


def s_pos_against(cv):
    """Red plank leaning ~45 deg, its end pressed against the cube face."""
    cube(cv, 372, 300, 250)
    soft_radial(cv, 506, 372, 34, 30, (20, 24, 60), alpha=0.28)
    bar(cv, (800, 660), (492, 360), thick=42)


def s_pos_among(cv):
    """Far cubes behind, ball in the centre, near cubes overlap it partly."""
    cube(cv, 300, 175, 150, shadow_a=0.0, edge_alpha=0.45)
    cube(cv, 700, 175, 150, shadow_a=0.0, edge_alpha=0.45)
    sphere(cv, 500, 375, 96, contact=True, fa_shadow=0.32)
    cube(cv, 350, 430, 160, shadow_a=0.0)
    cube(cv, 650, 430, 160, shadow_a=0.0)


def s_pos_around(cv):
    """Ball behind the cube, cube, then a ring of near balls in front."""
    cx, cy = 500, 497
    rx, ry = 330, 170
    back, front = [], []
    for k in range(8):
        ang = math.radians(12 + k * 45)
        p = (cx + rx * math.cos(ang), cy + ry * math.sin(ang))
        (back if math.sin(ang) < -0.15 else front).append(p)
    for p in back:
        ball_on(cv, p[0], p[1], 62, fa=0.30)
    cube(cv, 500, 265, 232)
    for p in front:
        ball_on(cv, p[0], p[1], 62, fa=0.34)


def s_pos_behind(cv):
    """Sphere first, cube drawn over it: the sphere peeks out on the left."""
    sphere(cv, 350, 390, 98, contact=True, fa_shadow=0.30)
    cube(cv, 620, 290, 246)


def s_pos_below(cv):
    """Cube lifted off the ground, ball on the floor strictly below it."""
    cube(cv, 500, 130, 240, shadow_a=0.26, lift=110)
    sphere(cv, 500, 596, 82, contact=True)


def s_pos_beneath(cv):
    """Sphere directly under the bottom face; the cube hides its top part."""
    sphere(cv, 500, 610, 90, contact=True, fa_shadow=0.26)
    cube(cv, 500, 200, 240)


def s_pos_beside(cv):
    """Cube with the ball beside it on its own contact patch."""
    cube(cv, 380, 290, 250)
    sphere(cv, 752, 570, 88, contact=True)


def s_pos_by(cv):
    """Ball right at the outer edge of the cube face (closer than beside)."""
    cube(cv, 392, 290, 250)
    sphere(cv, 650, 510, 82, contact=True, fa_shadow=0.36)


def s_pos_between(cv):
    """Two tall columns left and right, the ball exactly between them."""
    box_top(cv, 230, 255, 160, 160, 330, shadow_a=0.30)
    box_top(cv, 770, 255, 160, 160, 330, shadow_a=0.30)
    sphere(cv, 500, 500, 96, contact=True)


def s_pos_in(cv):
    """Glass cube, ball just behind the front faces (shows through)."""
    sphere(cv, 462, 470, 80, contact=False, dim=0.05)
    cube(cv, 500, 290, 230, glass=True, glass_alpha=0.50, edge_alpha=0.85,
         shadow_a=0.18)
    glass_glow(cv, 462, 470, 80, 0.20)


def s_pos_in_front_of(cv):
    """Full cube behind, the ball in front covers its lower vertex."""
    cube(cv, 640, 215, 240)
    sphere(cv, 600, 604, 92, contact=True)


def s_pos_inside(cv):
    """Ball deeper inside: stronger tint, darker ball, glass over it."""
    sphere(cv, 516, 420, 78, contact=False, dim=0.19)
    cube(cv, 500, 296, 236, glass=True, glass_alpha=0.52, edge_alpha=0.85,
         shadow_a=0.16)
    glass_glow(cv, 516, 420, 78, 0.12)


def s_pos_near(cv):
    """Cube on the right, ball far away on the left: the gap is the meaning."""
    cube(cv, 760, 220, 240)
    sphere(cv, 200, 520, 94, contact=True)


def s_pos_next_to(cv):
    """Ball right against the side face, the two contact shadows touch."""
    cube(cv, 400, 290, 250)
    sphere(cv, 690, 545, 86, contact=True, fa_shadow=0.40)


def s_pos_on(cv):
    """Ball resting on the top face: contact shadow on the face, slight squash."""
    cube(cv, 500, 215, 236)
    ball_on(cv, 500, 215, 84, fa=0.34, squash=0.96)


def s_pos_outside(cv):
    """Empty glass cube; the ball stays outside, bright and to the right."""
    cube(cv, 350, 290, 250, glass=True, glass_alpha=0.56, edge_alpha=0.90,
         shadow_a=0.20)
    sphere(cv, 790, 540, 88, contact=True)


def s_pos_over(cv):
    """Ball right above the cube, casting a shadow on the top face."""
    cube(cv, 480, 230, 232)
    shadow(cv, 480, 240, 82, 26, 0.34)
    sphere(cv, 480, 122, 90, contact=False)


def s_pos_through(cv):
    """Glass cube with a red plank passing horizontally through it."""
    bar(cv, (140, 400), (860, 400), thick=38)
    sphere(cv, 500, 402, 86, contact=False, dim=0.12)
    glass_glow(cv, 500, 402, 86, 0.14)
    cube(cv, 500, 300, 248, glass=True, glass_alpha=0.50, edge_alpha=0.85,
         shadow_a=0.18)


def s_pos_under(cv):
    """Table: the ball sits on the floor under it, the front leg overlaps it."""

    def mid():
        sphere(cv, 408, 592, 88, contact=True, fa_shadow=0.28)

    table(cv, 500, 215, 420, 195, th=26, leg_w=36, mid=mid)


def s_pos_underneath(cv):
    """A wide flat plate covers the ball from above; only the bottom shows."""
    sphere(cv, 500, 585, 100, contact=True, fa_shadow=0.30)
    box_top(cv, 500, 330, 460, 460, 34, shadow_a=0.24)


# ---------------------------------------------------------------------------
# DIRECTIONAL SCENES (17)
# ---------------------------------------------------------------------------
def s_dir_across(cv):
    """Road band; the dashed path crosses it, the ball is on the far side."""
    road(cv, 500, 430, 720, 250)
    dashed(cv, [(230, 205), (465, 336), (700, 467)],
           width=11, head=True, head_len=40, head_w=36)
    sphere(cv, 762, 516, 82, contact=True)


def s_dir_along(cv):
    """Same road, the dashed path runs parallel to it, ball in the middle."""
    road(cv, 500, 420, 760, 250)
    dashed(cv, [(240, 570), (500, 420), (760, 270)],
           width=11, head=True, head_len=40, head_w=36)
    sphere(cv, 500, 420, 82, contact=True)


def s_dir_against(cv):
    """Ball runs into the cube face; short sparks fly at the impact point."""
    cube(cv, 420, 300, 250)
    dashed(cv, [(900, 310), (830, 348), (762, 382)], width=11, head=False)
    sphere(cv, 700, 395, 86, contact=True)
    for ang in (-72, -36, -4, 30, 66, 104):
        a = math.radians(ang)
        p0 = (614 + math.cos(a) * 16, 395 + math.sin(a) * 16)
        p1 = (614 + math.cos(a) * 48, 395 + math.sin(a) * 48)
        stroke(cv, [[p0, p1]], 7.0, RED, alpha=0.92)


def s_dir_around(cv):
    """Dashed elliptical orbit around the cube base; ball riding on it."""
    cx, cy = 500, 526
    rx, ry = 330, 160
    back, front = [], []
    for k in range(72):
        ang = math.radians(-150 + k * 300 / 71.0)
        p = (cx + rx * math.cos(ang), cy + ry * math.sin(ang))
        (back if math.sin(ang) < -0.05 else front).append(p)
    dashed(cv, back, width=10, head=False)
    cube(cv, 500, 290, 236)
    dashed(cv, front, width=10, head=True, head_len=36, head_w=32)
    ball_on(cv, cx + rx * math.cos(math.radians(38)),
            cy + ry * math.sin(math.radians(38)), 64, fa=0.34)


def s_dir_away_from(cv):
    """Cube on the right, the dashed path leaves to the left, ball far away."""
    cube(cv, 780, 250, 215)
    dashed(cv, [(640, 372), (546, 405), (452, 438)], width=11,
           head=True, head_len=38, head_w=34)
    sphere(cv, 280, 505, 92, contact=True)


def s_dir_from(cv):
    """Dashed path starts at the cube face (start marker), ball mid-way."""
    cube(cv, 800, 250, 205)
    dashed(cv, [(680, 320), (360, 506)], width=11,
           head=True, head_len=38, head_w=34)
    start_marker(cv, 680, 320, 13)
    sphere(cv, 488, 431, 84, contact=True)


def s_dir_into(cv):
    """Glass cube: the dashed path enters through the front face, ball inside."""
    sphere(cv, 492, 470, 80, contact=False, dim=0.12)
    dashed(cv, [(900, 270), (800, 330), (700, 400), (610, 450)],
           width=11, head=True, head_len=38, head_w=34)
    cube(cv, 500, 286, 238, glass=True, glass_alpha=0.52, edge_alpha=0.85,
         shadow_a=0.16)
    glass_glow(cv, 492, 470, 80, 0.16)


def s_dir_off_of(cv):
    """Cube pedestal: the dashed path rolls off the edge, ball drops below it."""
    cube(cv, 450, 215, 236)
    dashed(cv, [(480, 200), (660, 235), (770, 300), (800, 400)],
           width=11, head=True, head_len=38, head_w=34)
    sphere(cv, 846, 510, 80, contact=True, fa_shadow=0.28)


def s_dir_onto(cv):
    """Arc from above landing on the top face; contact shadow on the face."""
    cube(cv, 500, 215, 230)
    dashed(cv, [(880, 150), (830, 62), (700, 70), (612, 148)],
           width=11, head=True, head_len=38, head_w=34)
    ball_on(cv, 500, 215, 82, fa=0.34, squash=0.97)


def s_dir_out_of(cv):
    """Dim ball starts inside, the path leaves through the right face."""
    sphere(cv, 390, 420, 64, contact=False, dim=0.34)
    dashed(cv, [(450, 430), (590, 455), (700, 480), (790, 500)],
           width=11, head=True, head_len=38, head_w=34)
    cube(cv, 390, 286, 240, glass=True, glass_alpha=0.52, edge_alpha=0.85,
         shadow_a=0.16)
    glass_glow(cv, 390, 420, 64, 0.10)
    sphere(cv, 846, 540, 80, contact=True, fa_shadow=0.30)


def s_dir_over(cv):
    """The arc passes above the top vertex of the cube; ball hovers above it."""
    cube(cv, 400, 340, 230)
    dashed(cv, [(900, 470), (800, 180), (560, 105), (240, 300)],
           width=11, head=True, head_len=38, head_w=34)
    sphere(cv, 450, 140, 80, contact=False)


def s_dir_through(cv):
    """Glass cube; the dashed path goes right through, dimmed inside."""
    dashed(cv, [(90, 430), (400, 340), (700, 270), (890, 230)],
           width=11, head=True, head_len=38, head_w=34)
    sphere(cv, 560, 322, 84, contact=False, dim=0.12)
    glass_glow(cv, 560, 322, 84, 0.12)
    cube(cv, 470, 210, 244, glass=True, glass_alpha=0.52, edge_alpha=0.85,
         shadow_a=0.16)


def s_dir_to(cv):
    """The dashed path ends at the cube face; the ball reached its goal."""
    cube(cv, 800, 240, 210)
    dashed(cv, [(250, 540), (400, 510), (540, 478), (650, 452)],
           width=11, head=True, head_len=38, head_w=34)
    sphere(cv, 700, 470, 84, contact=True)


def s_dir_towards(cv):
    """Shorter path: the ball has not reached the cube yet, clear gap."""
    cube(cv, 800, 230, 200)
    dashed(cv, [(280, 560), (400, 530), (500, 505)],
           width=11, head=True, head_len=36, head_w=32)
    sphere(cv, 580, 485, 84, contact=True)


def s_dir_under(cv):
    """Table with legs; the dashed path runs under the top, ball under it."""

    def mid():
        dashed(cv, [(100, 640), (320, 610), (560, 600), (880, 570)],
               width=11, head=True, head_len=38, head_w=34)
        sphere(cv, 590, 600, 80, contact=True, fa_shadow=0.28)

    table(cv, 500, 215, 420, 140, th=26, leg_w=36, mid=mid)


def s_dir_up(cv):
    """Staircase receding up-right; the path climbs it, ball on the top step."""
    tops = staircase(cv, 300, 616, steps=3, tread=118, width=140, rise=96,
                     axis="y")
    dashed(cv, tops, width=11, head=True, head_len=38, head_w=34)
    ball_on(cv, tops[-1][0], tops[-1][1], 72, fa=0.32, squash=0.97)


def s_dir_down(cv):
    """Staircase receding up-left; the path runs down it, ball at the foot."""
    tops = staircase(cv, 676, 588, steps=3, tread=124, width=132, rise=104,
                     axis="x")
    dashed(cv, tops + [(716, 566)], width=11, head=True, head_len=38, head_w=34)
    ball_on(cv, 776, 640, 80, fa=0.32)


# ---------------------------------------------------------------------------
# ---------------------------------------------------------------------------
SCENES = {
    # positional
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
    # directional
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

# The app keys dir_in_to / dir_on_to must exist as PNGs too.
ALIASES = {"dir_in_to": "dir_into", "dir_on_to": "dir_onto"}

# composition target (fraction of the 1000x700 canvas the content should fill)
FILL_W, FILL_H = 0.92, 0.92
SCALE_MIN, SCALE_MAX = 0.80, 1.12


def render_one(key):
    fn = SCENES[key]
    cv = Canvas()
    fn(cv)
    im = cv.img
    bb = im.getbbox()
    if bb is None:
        raise RuntimeError("empty scene: " + key)
    x0, y0, x1, y1 = bb
    bw, bh = (x1 - x0) / SS, (y1 - y0) / SS
    scale = min(W * FILL_W / bw, H * FILL_H / bh)
    scale = min(SCALE_MAX, max(SCALE_MIN, scale))
    nw = max(1, int(round(bw * scale)))
    nh = max(1, int(round(bh * scale)))
    crop = im.crop(bb).resize((nw, nh), Image.LANCZOS)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    out.alpha_composite(crop, ((W - nw) // 2, (H - nh) // 2))
    meta = {"bbox": (round(bw, 1), round(bh, 1)), "scale": round(scale, 3),
            "size": (nw, nh)}
    return out, meta


# ---------------------------------------------------------------------------
# preview contact sheet
# ---------------------------------------------------------------------------
def _load_font(size):
    for path in ("/System/Library/Fonts/Supplemental/Arial.ttf",
                 "/System/Library/Fonts/Helvetica.ttc",
                 "/System/Library/Fonts/Supplemental/Verdana.ttf"):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default(size=size)


def build_preview(out_path):
    keys = list(SCENES.keys()) + list(ALIASES.keys())
    cols = 6
    cell_w, img_w, img_h, cap_h = 236, 216, 151, 26
    cell_h = img_h + cap_h
    rows = (len(keys) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h + 8), (255, 255, 255))
    d = ImageDraw.Draw(sheet)
    font = _load_font(17)
    for i, key in enumerate(keys):
        r, c = divmod(i, cols)
        path = os.path.join(DRAWABLE_DIR, "prep_%s.png" % key)
        im = Image.open(path).convert("RGBA")
        im = im.resize((img_w, img_h), Image.LANCZOS)
        bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
        bg.alpha_composite(im)
        x = c * cell_w + (cell_w - img_w) // 2
        y = r * cell_h + 4
        sheet.paste(bg.convert("RGB"), (x, y))
        tw = d.textlength(key, font=font)
        d.text((c * cell_w + (cell_w - tw) / 2.0, y + img_h + 4), key,
               fill=(40, 44, 52), font=font)
    sheet.save(out_path)
    return out_path


# ---------------------------------------------------------------------------
def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="*", default=None)
    ap.add_argument("--preview-only", action="store_true")
    ap.add_argument("--no-preview", action="store_true")
    ap.add_argument("--report", action="store_true")
    args = ap.parse_args(argv)

    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    os.makedirs(DOCS_DIR, exist_ok=True)
    keys = args.only or list(SCENES.keys())

    total = 0
    if not args.preview_only:
        for key in keys:
            im, meta = render_one(key)
            path = os.path.join(DRAWABLE_DIR, "prep_%s.png" % key)
            im.save(path, optimize=True)
            sz = os.path.getsize(path)
            total += sz
            print("wrote prep_%s.png  %4d KB  bbox=%s scale=%s out=%s"
                  % (key, sz // 1024, meta["bbox"], meta["scale"], meta["size"]))
        for alias, target in ALIASES.items():
            src = os.path.join(DRAWABLE_DIR, "prep_%s.png" % target)
            dst = os.path.join(DRAWABLE_DIR, "prep_%s.png" % alias)
            shutil.copyfile(src, dst)
            total += os.path.getsize(dst)
            print("wrote prep_%s.png (alias of %s)" % (alias, target))

    if not args.no_preview:
        build_preview(os.path.join(DOCS_DIR, "preview.png"))
        print("wrote docs/prep-art/preview.png")

    pngs = [f for f in os.listdir(DRAWABLE_DIR) if f.startswith("prep_") and f.endswith(".png")]
    total_all = sum(os.path.getsize(os.path.join(DRAWABLE_DIR, f)) for f in pngs)
    print("total: %d files, %.2f MB" % (len(pngs), total_all / 1e6))


if __name__ == "__main__":
    main()
