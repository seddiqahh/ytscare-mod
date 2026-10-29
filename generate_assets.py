#!/usr/bin/env python3
"""
Generates every non-Java asset of the mod:
  - (optional, --placeholder-skins) 64x64 placeholder skins - NOT needed, real skins are already included
  - synthetic horror sounds (.ogg): whispers, voices, screams, knocks, footsteps, breathing, heartbeat
  - sounds.json, en_us.json, spawn-egg item models, biome spawn modifiers
  - glowing-eye textures, custom loot item textures/models, loot tables

Usage:  python3 tools/generate_assets.py        (needs: numpy, ffmpeg; + pillow for --placeholder-skins)
"""
import json, os, subprocess, tempfile, wave
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/ytscare")
DATA = os.path.join(RES, "data/ytscare")

# id, display name, spawn weight (must match MonsterType.java)
MONSTERS = [
    ("red_spiral", "Khazneh", 3), ("suit_shade", "Ury", 2), ("grin_thing", "YCN", 3),
    ("red_hoodie", "zSaeedx", 3), ("tuxedo_shade", "3zez", 2), ("split_mask", "Lunge", 3),
    ("crown_prince", "OmarFive", 3), ("duck", "Rest", 4), ("smiley", "SeddiQ", 3),
    ("hood_kid", "Gray", 3), ("galaxy", "Fariis", 1), ("red_king", "ii3mory", 1),
    ("beanie", "S3eeDx", 3), ("panda", "Zetrex", 2),
]


def mk(*p):
    path = os.path.join(*p)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


# =====================================================================================  SKINS
def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


HEAD, BODY, RARM, LARM, RLEG, LLEG = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (32, 48, 4, 12, 4), (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)


class Skin:
    def __init__(self):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.im.load()

    def rect(self, x, y, w, h, c):
        c = rgb(c) if isinstance(c, int) else c
        for i in range(x, x + w):
            for j in range(y, y + h):
                if 0 <= i < 64 and 0 <= j < 64:
                    self.px[i, j] = c

    def box(self, part, c):
        u, v, w, h, d = part
        self.rect(u, v, 2 * d + 2 * w, d + h, c)

    def stripe(self, part, rel_y, rows, c):
        """horizontal band across all 4 side faces of a part, rel_y rows below the top of the side area"""
        u, v, w, h, d = part
        self.rect(u, v + d + rel_y, 2 * d + 2 * w, rows, c)

    def noise(self, part, palette, seed, star=0.04):
        u, v, w, h, d = part
        r = np.random.default_rng(seed)
        for i in range(u, u + 2 * d + 2 * w):
            for j in range(v, v + d + h):
                if r.random() < star:
                    self.px[i, j] = rgb(0xFFFFFF if r.random() < 0.5 else 0x70E8E8)
                else:
                    self.px[i, j] = rgb(palette[r.integers(len(palette))])

    def plaid(self, part, amount=14):
        u, v, w, h, d = part
        for i in range(u, u + 2 * d + 2 * w):
            for j in range(v, v + d + h):
                R, G, B, A = self.px[i, j]
                k = -amount if ((i // 2 + j // 2) % 2 == 0) else 0
                self.px[i, j] = (max(0, R + k), max(0, G + k), max(0, B + k), A)


SPIRAL = ["KKKKKKKK", "KRRRRRRK", "KRKKKKRK", "KRKRRKRK", "KRKRKKRK", "KRKRRRRK", "KRKKKKKK", "KRRRRRRR"]


def make_skin(name, sp):
    s = Skin()
    skin = sp.get("skin", 0xC49A7A)
    s.box(HEAD, sp.get("head", skin))

    if "hair" in sp:  # hair on the base head layer
        hc, rows = rgb(sp["hair"]), sp.get("hair_rows", 2)
        s.rect(8, 0, 8, 8, hc)  # top
        s.rect(24, 8, 8, 8, hc)  # back
        s.rect(0, 8, 8, 4, hc)  # right side
        s.rect(16, 8, 8, 4, hc)  # left side
        s.rect(8, 8, 8, rows, hc)  # fringe
        s.rect(8, 8 + rows, 1, 2, hc)
        s.rect(15, 8 + rows, 1, 2, hc)

    if "face_ascii" in sp:
        for y, row in enumerate(sp["face_ascii"]):
            for x, ch in enumerate(row):
                s.px[8 + x, 8 + y] = rgb(sp["face_pal"][ch])

    if "face_rect" in sp:
        for (x, y, w, h, c) in sp["face_rect"]:
            s.rect(x, y, w, h, c)

    eyes = sp.get("eyes")
    if eyes:
        y, rows = eyes.get("y", 11), eyes.get("rows", 1)
        white, iris = eyes.get("white"), eyes["iris"]
        for k in range(rows):
            if white is None:
                for x in (9, 10, 13, 14):
                    s.px[x, y + k] = rgb(iris)
            else:
                s.px[9, y + k], s.px[10, y + k] = rgb(white), rgb(iris)
                s.px[13, y + k], s.px[14, y + k] = rgb(iris), rgb(white)
        if "iris_r" in eyes:  # asymmetric eye (right one different)
            for k in range(rows):
                s.px[13, y + k] = s.px[14, y + k] = rgb(eyes["iris_r"])

    for (x, y, c) in sp.get("pixels", []):
        s.px[x, y] = rgb(c)

    # body
    s.box(BODY, sp["body"])
    if "plaid" in sp:
        pass
    if "shirt" in sp:
        s.rect(21, 20, 6, 9, sp["shirt"])
    if "tie" in sp:
        s.rect(23, 20, 2, sp.get("tie_len", 7), sp["tie"])
    for (ry, rows, c) in sp.get("body_stripes", []):
        s.stripe(BODY, ry, rows, c)
    for (x, y, w, h, c) in sp.get("body_rects", []):
        s.rect(x, y, w, h, c)

    # arms
    for part in (RARM, LARM):
        s.box(part, sp.get("arm", sp["body"]))
        for (ry, rows, c) in sp.get("arm_stripes", []):
            s.stripe(part, ry, rows, c)
        if "hand" in sp:
            s.stripe(part, 12 - sp.get("hand_rows", 3), sp.get("hand_rows", 3), sp["hand"])

    # legs
    for part in (RLEG, LLEG):
        s.box(part, sp.get("leg", 0x101010))
        if "shorts" in sp:
            s.stripe(part, 0, sp.get("shorts_rows", 5), sp["shorts"])
        if "shoe" in sp:
            s.stripe(part, 12 - sp.get("shoe_rows", 2), sp.get("shoe_rows", 2), sp["shoe"])

    # hat layer (crowns, beanies, hair)
    for (x, y, w, h, c) in sp.get("hat_rects", []):
        s.rect(x, y, w, h, c)

    if "plaid" in sp:
        s.plaid(BODY)
        s.plaid(RARM)
        s.plaid(LARM)

    if "split" in sp:  # half black / half white (character's left = white)
        W = 0xE4E4E4
        s.rect(12, 8, 4, 8, W)  # head front right half (viewer)
        s.rect(8, 0, 4, 8, 0x000000)
        s.rect(12, 0, 4, 8, W)
        s.rect(16, 8, 8, 8, W)  # left side of head
        s.rect(24, 8, 4, 8, W)
        s.rect(24, 20, 4, 12, W)
        s.rect(28, 20, 4, 12, W)
        s.rect(32, 20, 4, 12, W)
        s.box(LARM, W)
        s.box(LLEG, W)
        band = rgb(0xD8160F)
        s.rect(0, 10, 32, 2, band)
        s.rect(32, 10, 32, 2, band)

    if "extra" in sp:
        sp["extra"](s)

    s.im.save(os.path.join(ASSETS, "textures/entity", name + ".png"))
    return s.im


GALAXY = [0x14146E, 0x2A10A0, 0x5018B0, 0x7A20D0, 0x103090, 0x1060C0, 0x0A0A50]


def galaxy_extra(s):
    s.noise(HEAD, [0x2A0A8A, 0x3A14B0, 0x1A0A6A], 1, star=0.0)
    s.rect(9, 10, 2, 1, 0x70E8E8)
    s.rect(13, 10, 2, 1, 0x70E8E8)
    s.rect(10, 14, 4, 1, 0xE8E8E8)
    s.px[9, 13] = s.px[14, 13] = rgb(0xE8E8E8)
    s.noise(BODY, GALAXY, 2)
    s.noise(RARM, GALAXY, 3)
    s.noise(LARM, GALAXY, 4)
    s.noise(RLEG, GALAXY, 5)
    s.noise(LLEG, GALAXY, 6)
    for part in (RARM, LARM):
        s.stripe(part, 7, 3, 0x252525)
        s.stripe(part, 10, 2, 0x68E8E8)
    for part in (RLEG, LLEG):
        s.stripe(part, 8, 4, 0x252525)
    r = np.random.default_rng(9)
    for x in range(40, 64):
        for y in range(0, 11):
            if y < 3 or r.random() < 0.55:
                if x < 48 or y >= 8 or True:
                    s.px[x, y] = rgb(0xC8C0D0)


def smiley_extra(s):
    P = 0x3A0F6A
    s.px[9, 10] = s.px[14, 10] = rgb(P)
    s.px[9, 13] = s.px[14, 13] = rgb(P)
    s.rect(9, 14, 6, 1, P)


SPECS = {
    "red_spiral": dict(head=0x7A0000, face_ascii=SPIRAL, face_pal={"K": 0x2A0000, "R": 0x8A0000}, body=0x151A1D, tie=0x8A2A30, tie_len=5,
                       arm_stripes=[(0, 5, 0xC4CCD0), (5, 3, 0x7A0010)], leg=0x0D0F12, shoe=0xC8D0D4),
    "suit_shade": dict(head=0x050505, eyes=dict(iris=0xE0E0E0, y=10), body=0x0B0B0B, shirt=0xE0E0E0, tie=0x050505, hand=0xE0E0E0, leg=0x0A0A0A, shoe=0x050505),
    "grin_thing": dict(head=0x303030, eyes=dict(iris=0x000000, iris_r=0xE0E0E0, y=11, rows=1), body=0x000000, leg=0x000000,
                       pixels=[(x, 13, 0xE0E0E0) for x in range(9, 15)] + [(x, 14, 0x000000) for x in range(10, 14)] + [(9, 14, 0xE0E0E0), (14, 14, 0xE0E0E0)]),
    "red_hoodie": dict(skin=0xC49A7A, hair=0x222222, eyes=dict(white=0xC8C8C8, iris=0x3A0000, y=11, rows=2), body=0x862727, leg=0xD8B8A0,
                       shorts=0x6A8AA5, shorts_rows=5, shoe=0xE0E0E0),
    "tuxedo_shade": dict(head=0x0A0A0A, eyes=dict(iris=0xE0E0E0, y=10), body=0x0A0A0A, shirt=0xE0E0E0, tie=0x050505, arm_stripes=[(8, 2, 0xE0E0E0)],
                         leg=0x303030, shoe=0x0F0F0F, shoe_rows=3, body_rects=[(23, 23, 2, 1, 0xE0E0E0), (23, 25, 2, 1, 0xE0E0E0), (23, 27, 2, 1, 0xE0E0E0)]),
    "split_mask": dict(head=0x000000, body=0x000000, leg=0x000000, split=True),
    "crown_prince": dict(skin=0xE0B090, hair=0x7A5048, eyes=dict(white=0xE8E8E8, iris=0x2A1A1A, y=11, rows=2), body=0x5A96C8, shirt=0x8040B8,
                         body_rects=[(21, 20, 1, 9, 0xD0CFC8), (26, 20, 1, 9, 0xD0CFC8)], arm_stripes=[(8, 2, 0xD0CFC8)], hand=0xE0B090, hand_rows=2,
                         leg=0x141416, shoe=0x8040B8,
                         hat_rects=[(40, 0, 8, 8, 0xD4900F), (32, 8, 32, 2, 0xD4900F), (43, 8, 2, 1, 0xA01820)]),
    "duck": dict(head=0x1E1E1E, face_rect=[(9, 9, 6, 7, 0xCDD06A), (8, 11, 8, 2, 0x000000), (11, 12, 2, 2, 0xC87A30)], body=0x202020,
                 body_rects=[(22, 28, 4, 2, 0x181818)], leg=0xD4D46A, shoe=0xC4C460),
    "smiley": dict(head=0xE6C200, body=0xE6C200, hand=0x7A10B0, hand_rows=3, arm_stripes=[(8, 1, 0x4A1A6A)], leg=0x5A1A9A, shoe=0xE0E0E0, extra=smiley_extra),
    "hood_kid": dict(skin=0xA87858, hair=0x222222, hair_rows=3, eyes=dict(iris=0x2A0A3A, y=11, rows=1), body=0x4D5361, arm_stripes=[(8, 4, 0x1A1A1A)],
                     body_rects=[(22, 22, 1, 4, 0x1E1E1E), (25, 22, 1, 4, 0x1E1E1E)], leg=0x1F1F1F, shoe=0xE0E0E0, hand=0xA87858, hand_rows=1),
    "galaxy": dict(body=0x14146E, extra=galaxy_extra),
    "red_king": dict(skin=0xC09078, hair=0x111111, hair_rows=3, eyes=dict(white=0xAAAAAA, iris=0xCC0000, y=11, rows=2), body=0xA8202A,
                     body_stripes=[(0, 3, 0x222436), (3, 1, 0xD47010)], arm_stripes=[(0, 4, 0x222436), (4, 1, 0xD47010)], hand=0xC09078, hand_rows=3,
                     leg=0x252525, shoe=0xC02028, hat_rects=[(40, 0, 8, 8, 0xC8A860), (32, 8, 32, 2, 0xC8A860), (32, 9, 32, 1, 0xA02A20)]),
    "beanie": dict(skin=0xD09868, hair=0x4A3C3C, hair_rows=3, eyes=dict(white=0xE8E8E8, iris=0x40606A, y=11, rows=2), body=0xA89888, shirt=0xD0CECE,
                   arm_stripes=[(5, 7, 0xD09868)], plaid=True, leg=0x504848, shoe=0xD0CECE, shoe_rows=3,
                   hat_rects=[(40, 0, 8, 8, 0xB0A094), (32, 8, 32, 3, 0xB0A094), (43, 9, 2, 1, 0x604C4C)]),
    "panda": dict(skin=0xC08860, hair=0x4A4038, hair_rows=3, eyes=dict(white=0xD0B8B0, iris=0x706060, y=11, rows=2), body=0x141414,
                  body_rects=[(21, 23, 6, 5, 0xCFCCCC), (22, 22, 4, 7, 0xCFCCCC)], leg=0x141414),
}


def gen_skins():
    imgs = []
    for mid, _, _ in MONSTERS:
        imgs.append(make_skin(mid, SPECS[mid]))
    # contact sheet for quick visual check
    sheet = Image.new("RGBA", (64 * 7 * 3, 64 * 2 * 3), (60, 60, 70, 255))
    for i, im in enumerate(imgs):
        big = im.resize((192, 192), Image.NEAREST)
        sheet.alpha_composite(big, ((i % 7) * 192, (i // 7) * 192))
    sheet.save(os.path.join(ROOT, "tools", "skins_preview.png"))


# =====================================================================================  AUDIO
SR = 44100


def norm(x, peak=0.9):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def shape(x, fn):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(X * fn(f), len(x))


def formants(freqs, bws):
    return lambda f: sum(np.exp(-0.5 * ((f - fc) / bw) ** 2) for fc, bw in zip(freqs, bws))


def reverb(x, rt=0.8, mix=0.4, seed=0):
    r = np.random.default_rng(seed)
    n = int(rt * SR)
    ir = r.standard_normal(n) * np.exp(-np.linspace(0, 7, n))
    N = len(x) + n
    wet = np.fft.irfft(np.fft.rfft(x, N) * np.fft.rfft(ir, N), N)
    wet = wet / (np.max(np.abs(wet)) + 1e-9) * np.max(np.abs(x))
    dry = np.concatenate([x, np.zeros(n)])
    return dry + mix * wet


def whisper(seed):
    r = np.random.default_rng(seed)
    out = []
    for _ in range(int(r.integers(5, 9))):
        n = int(r.uniform(0.12, 0.32) * SR)
        sig = shape(r.standard_normal(n), formants([r.uniform(400, 900), r.uniform(1200, 2400), r.uniform(2800, 4200)], [150, 300, 500]))
        if r.random() < 0.45:
            sig += 0.7 * shape(r.standard_normal(n), lambda f: ((f > 5000) & (f < 9500)).astype(float))
        sig = sig / (np.std(sig) + 1e-9) * (np.sin(np.linspace(0, np.pi, n)) ** 1.4)
        out += [sig, np.zeros(int(r.uniform(0.03, 0.16) * SR))]
    return norm(reverb(np.concatenate(out), 0.9, 0.5, seed), 0.8)


def voice(seed):
    """low, slow, creepy voiced mumble"""
    r = np.random.default_rng(seed)
    out = []
    for _ in range(int(r.integers(3, 6))):
        n = int(r.uniform(0.3, 0.6) * SR)
        t = np.arange(n) / SR
        f0 = r.uniform(80, 125) * (1 - 0.25 * t / t[-1])
        ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.02 * np.sin(2 * np.pi * 5 * t))) / SR
        saw = sum(np.sin(k * ph) / k for k in range(1, 25))
        sig = shape(saw + 0.4 * r.standard_normal(n), formants([r.uniform(300, 700), r.uniform(900, 2200), 3000], [120, 250, 400]))
        sig = sig / (np.std(sig) + 1e-9) * (np.sin(np.linspace(0, np.pi, n)) ** 1.2)
        out += [sig, np.zeros(int(r.uniform(0.05, 0.2) * SR))]
    return norm(reverb(np.concatenate(out), 1.2, 0.6, seed), 0.8)


def scream(seed, dur=1.5, f0=500, f1=1800, rough=40):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f0 + (f1 - f0) * np.clip(t / 0.22, 0, 1) ** 0.6 - 0.45 * f0 * np.clip((t - 0.8 * dur) / (0.2 * dur), 0, 1)
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(k * ph) / k for k in range(1, 9))
    x *= 0.6 + 0.4 * np.sign(np.sin(2 * np.pi * rough * t))
    x += 0.35 * shape(r.standard_normal(n), formants([1500, 3000], [800, 1200]))
    x = np.tanh(2.5 * x / (np.std(x) + 1e-9))
    env = np.minimum(1, t / 0.01) * np.minimum(1, (dur - t) / 0.35)
    return x * env


def jumpscare(i):
    dur = 1.4
    x = scream(100 + i, dur, 320 + 45 * (i % 7), 1500 + 130 * (i % 9), 28 + 3 * i)
    t = np.arange(len(x)) / SR
    boom = np.sin(2 * np.pi * (60 - 20 * t)) * np.exp(-t * 5) * 1.6
    r = np.random.default_rng(i)
    burst = shape(r.standard_normal(len(x)), formants([2500], [2500])) * np.exp(-t * 28) * 2.0
    return norm(np.tanh(1.3 * (x + boom + burst)), 0.97)


def knock():
    out = np.zeros(int(1.2 * SR))
    r = np.random.default_rng(3)
    for st in (0.0, 0.32, 0.55):
        n = int(0.35 * SR)
        t = np.arange(n) / SR
        k = np.sin(2 * np.pi * 105 * t) * np.exp(-t * 32) + 0.5 * shape(r.standard_normal(n), lambda f: (f < 900).astype(float)) * np.exp(-t * 70)
        i = int(st * SR)
        out[i:i + n] += k
    return norm(reverb(out, 0.4, 0.3), 0.9)


def steps():
    out = np.zeros(int(5.0 * SR))
    r = np.random.default_rng(5)
    for k in range(8):
        n = int(0.3 * SR)
        t = np.arange(n) / SR
        s = shape(r.standard_normal(n), lambda f: (f < 450).astype(float)) * np.exp(-t * 22)
        s += 0.4 * shape(r.standard_normal(n), lambda f: ((f > 800) & (f < 3000)).astype(float)) * np.exp(-t * 90)
        i = int((0.1 + k * 0.58) * SR)
        out[i:i + n] += s / (np.max(np.abs(s)) + 1e-9) * (0.3 + 0.7 * k / 7)
    return norm(reverb(out, 0.5, 0.25), 0.9)


def breath():
    n = int(4.4 * SR)
    t = np.arange(n) / SR
    base = shape(np.random.default_rng(8).standard_normal(n), formants([700, 1800, 3200], [300, 600, 900]))
    env = np.zeros(n)
    for st in (0.1, 2.3):
        m = (t > st) & (t < st + 1.9)
        env[m] = np.sin(np.pi * (t[m] - st) / 1.9) ** 2
    return norm(reverb(base * env, 0.7, 0.3), 0.8)


def heartbeat():
    out = np.zeros(int(3.6 * SR))
    for b in range(4):
        for off, amp in ((0.0, 1.0), (0.27, 0.65)):
            n = int(0.3 * SR)
            t = np.arange(n) / SR
            s = np.sin(2 * np.pi * (52 - 18 * t) * t) * np.exp(-t * 16) * amp
            i = int((0.1 + b * 0.85 + off) * SR)
            out[i:i + n] += s
    return norm(out, 0.95)


def heartbeat_beat():
    """ONE lub-dub (0.7 s). The client replays it faster the closer a stalker gets."""
    out = np.zeros(int(0.7 * SR))
    for off, amp in ((0.0, 1.0), (0.24, 0.65)):
        n = int(0.3 * SR)
        t = np.arange(n) / SR
        s = np.sin(2 * np.pi * (52 - 18 * t) * t) * np.exp(-t * 16) * amp
        i = int(off * SR) + int(0.02 * SR)
        out[i:i + n] += s
    return norm(out, 0.95)


def write_ogg(x, path):
    path = mk(path)
    wav = tempfile.mktemp(suffix=".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(wav)


def gen_audio():
    S = os.path.join(ASSETS, "sounds")
    for i in range(1, 4):
        write_ogg(whisper(i), f"{S}/whisper/{i}.ogg")
        write_ogg(voice(10 + i), f"{S}/voice/{i}.ogg")
    for i in range(1, 3):
        write_ogg(norm(scream(50 + i, 1.8, 400 + 150 * i, 1400 + 400 * i, 33), 0.9), f"{S}/scare/{i}.ogg")
    write_ogg(knock(), f"{S}/knock.ogg")
    write_ogg(steps(), f"{S}/steps.ogg")
    write_ogg(breath(), f"{S}/breath.ogg")
    write_ogg(heartbeat(), f"{S}/heartbeat.ogg")
    write_ogg(heartbeat_beat(), f"{S}/heartbeat_beat.ogg")
    for i, (mid, _, _) in enumerate(MONSTERS):
        write_ogg(jumpscare(i), f"{S}/jumpscare/{mid}.ogg")


# =====================================================================================  JSON
def ev(name, pitch=1.0, vol=1.0, weight=1):
    return {"name": f"minecraft:{name}", "type": "event", "pitch": pitch, "volume": vol, "weight": weight}


def f(name, vol=1.0, pitch=1.0, weight=1):
    return {"name": f"ytscare:{name}", "volume": vol, "pitch": pitch, "weight": weight}


# vanilla sounds reused for ambient / hurt / death + as extra jumpscare variants
VANILLA = {
    "red_spiral":   dict(j=[ev("entity.warden.roar", 0.8)], a=[ev("entity.warden.listening", 0.7), ev("ambient.cave")], h=ev("entity.warden.hurt", 0.8), d=ev("entity.warden.death", 0.7)),
    "suit_shade":   dict(j=[ev("entity.enderman.scream", 0.5)], a=[ev("entity.enderman.stare", 0.5), ev("ambient.cave", 0.6)], h=ev("entity.enderman.hurt", 0.6), d=ev("entity.enderman.death", 0.5)),
    "grin_thing":   dict(j=[ev("entity.ghast.scream", 1.4)], a=[ev("entity.cat.hiss", 0.6), ev("entity.zombie.ambient", 1.6)], h=ev("entity.zombie.hurt", 1.6), d=ev("entity.zombie.death", 1.6)),
    "red_hoodie":   dict(j=[ev("entity.enderman.scream", 1.2)], a=[ev("entity.vex.ambient", 0.6), ev("ambient.soul_sand_valley.mood")], h=ev("entity.vex.hurt", 0.8), d=ev("entity.vex.death", 0.8)),
    "tuxedo_shade": dict(j=[ev("entity.ravager.roar", 0.7)], a=[ev("entity.wither.ambient", 0.5), ev("entity.skeleton.ambient", 0.6)], h=ev("entity.wither.hurt", 0.7), d=ev("entity.wither.death", 0.8)),
    "split_mask":   dict(j=[ev("entity.elder_guardian.curse", 1.0)], a=[ev("ambient.crimson_forest.mood"), ev("entity.ghast.ambient", 0.7)], h=ev("entity.ghast.hurt", 0.7), d=ev("entity.ghast.death", 0.8)),
    "crown_prince": dict(j=[ev("entity.ghast.scream", 0.6)], a=[ev("entity.zombie_villager.ambient", 0.4), ev("ambient.basalt_deltas.mood")], h=ev("entity.zombie_villager.hurt", 0.6), d=ev("entity.zombie_villager.death", 0.5)),
    "duck":         dict(j=[ev("entity.ghast.scream", 1.8)], a=[ev("entity.chicken.ambient", 0.4)], h=ev("entity.chicken.hurt", 0.5), d=ev("entity.chicken.death", 0.4)),
    "smiley":       dict(j=[ev("entity.elder_guardian.curse", 1.2)], a=[ev("entity.vex.ambient", 1.5), ev("ambient.cave")], h=ev("entity.vex.hurt", 1.4), d=ev("entity.vex.death", 1.3)),
    "hood_kid":     dict(j=[ev("entity.phantom.swoop", 0.6)], a=[ev("entity.phantom.ambient", 0.6), ev("entity.enderman.stare", 0.8)], h=ev("entity.phantom.hurt", 0.7), d=ev("entity.phantom.death", 0.7)),
    "galaxy":       dict(j=[ev("entity.ender_dragon.growl", 0.7), ev("entity.warden.sonic_boom", 0.8)], a=[ev("entity.ender_dragon.ambient", 0.5), ev("entity.warden.heartbeat", 0.7)], h=ev("entity.ender_dragon.hurt", 0.6), d=ev("entity.ender_dragon.death", 0.5)),
    "red_king":     dict(j=[ev("entity.ravager.roar", 0.5)], a=[ev("entity.ravager.ambient", 0.5), ev("entity.evoker.ambient", 0.6)], h=ev("entity.ravager.hurt", 0.6), d=ev("entity.ravager.death", 0.5)),
    "beanie":       dict(j=[ev("entity.warden.agitated", 0.9)], a=[ev("ambient.cave", 0.9), ev("entity.husk.ambient", 0.7)], h=ev("entity.husk.hurt", 0.8), d=ev("entity.husk.death", 0.8)),
    "panda":        dict(j=[ev("entity.polar_bear.warning", 0.6)], a=[ev("entity.polar_bear.ambient", 0.5), ev("entity.panda.worried_ambient", 0.5)], h=ev("entity.polar_bear.hurt", 0.6), d=ev("entity.polar_bear.death", 0.6)),
}


def gen_json():
    snd = {}
    for mid, name, _ in MONSTERS:
        v = VANILLA[mid]
        # jumpscare = custom synthetic screech + vanilla roar (delete the vanilla line if you only want the custom file)
        snd[f"{mid}.jumpscare"] = {"sounds": [f(f"jumpscare/{mid}", 1.0, 1.0, 2)] + v["j"]}
        snd[f"{mid}.ambient"] = {"sounds": v["a"] + [f("whisper/1", 1.0, 1.0, 2), f("whisper/2", 1.0, 1.0, 2), f("voice/1", 0.9, 1.0, 1)]}
        snd[f"{mid}.hurt"] = {"sounds": [v["h"]]}
        snd[f"{mid}.death"] = {"sounds": [v["d"]]}
    snd["ambient.whisper"] = {"sounds": [f(f"whisper/{i}", 1.2) for i in (1, 2, 3)]}
    snd["ambient.voice"] = {"sounds": [f(f"voice/{i}", 1.1) for i in (1, 2, 3)]}
    snd["ambient.scare"] = {"sounds": [f("scare/1", 1.2), f("scare/2", 1.2), ev("entity.ghast.scream", 0.6), ev("entity.elder_guardian.curse", 0.7)]}
    snd["ambient.knock"] = {"sounds": [f("knock", 1.2)]}
    snd["ambient.steps"] = {"sounds": [f("steps", 1.1)]}
    snd["ambient.breath"] = {"sounds": [f("breath", 1.2)]}
    snd["ambient.heartbeat"] = {"sounds": [f("heartbeat", 1.3)]}
    snd["ambient.heartbeat_beat"] = {"sounds": [f("heartbeat_beat", 1.3)]}
    json.dump(snd, open(mk(ASSETS, "sounds.json"), "w"), indent=2)

    lang = {"itemGroup.ytscare": "YT Horror"}
    for mid, name, w in MONSTERS:
        lang[f"entity.ytscare.{mid}"] = name
        lang[f"item.ytscare.{mid}_spawn_egg"] = f"{name} Spawn Egg"
        json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(mk(ASSETS, "models/item", f"{mid}_spawn_egg.json"), "w"), indent=2)
        json.dump({
            "type": "forge:add_spawns",
            "biomes": "#minecraft:is_overworld",
            "spawners": [{"type": f"ytscare:{mid}", "weight": w, "minCount": 1, "maxCount": 1}],
        }, open(mk(DATA, "forge/biome_modifier", f"spawn_{mid}.json"), "w"), indent=2)
    for iid, (name, _) in CUSTOM_ITEMS.items():
        lang[f"item.ytscare.{iid}"] = name
    json.dump(lang, open(mk(ASSETS, "lang/en_us.json"), "w"), indent=2, ensure_ascii=False)


# =====================================================================================  EYES / ITEMS / LOOT
# Glowing eyes: pixels of the head FRONT face (0-7, 0-7). Only these pixels are drawn by the full-bright eye layer.
# (face x, face y, colour). Edit freely - then run this script (or just edit the PNG in textures/entity/eyes/).
def _sym(rows, xs, c):
    return [(x, y, c) for y in rows for x in xs]


EYES = {
    "red_spiral":   [(2, 4, 0xFF3010), (5, 4, 0xFF3010)],
    "suit_shade":   _sym([4], [1, 2, 5, 6], 0xFFFFFF),
    "grin_thing":   [(5, 3, 0xFFFFFF), (6, 3, 0xFFFFFF)] + _sym([6], [1, 2, 5, 6], 0xFFFFFF),
    "red_hoodie":   _sym([5, 6], [2, 5], 0xFF2020),
    "tuxedo_shade": _sym([5], [1, 2, 5, 6], 0xFFFFFF),
    "split_mask":   _sym([3], [1, 2, 5, 6], 0xFF3A20),
    "crown_prince": _sym([4, 5], [2, 5], 0xFFC030),
    "duck":         [(2, 4, 0xE8FF40), (5, 4, 0xE8FF40)],
    "smiley":       _sym([3, 4], [2, 5], 0xC050FF),   # this skin has no eyes - added purple ones
    "hood_kid":     _sym([5, 6], [2, 5], 0xB040FF),
    "galaxy":       _sym([4], [1, 2, 5, 6], 0x40FFFF),
    "red_king":     _sym([5, 6], [2, 5], 0xFF1010),
    "beanie":       _sym([5, 6], [2, 5], 0x50F0FF),
    "panda":        _sym([4, 5], [2, 5], 0xFFF0E0),
}


def gen_eyes():
    for mid, px in EYES.items():
        im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        for x, y, c in px:
            im.putpixel((8 + x, 8 + y), rgb(c))
        im.save(mk(ASSETS, "textures/entity/eyes", mid + ".png"))


# custom loot items: id -> (display name, ascii art). Palette letters below.
ART_SHARD = [
    "................", ".............dd.", "............dLLd", "...........dLLMd",
    "..........dLLMMd", ".........dLLMMd.", "........dLLMMd..", ".......dLMMMd...",
    "......dLMMMd....", ".....dLMMMd.....", "....dLMMMd......", "...dLMMMd.......",
    "..dLMMd.........", "..dMMd..........", "...dd...........", "................",
]
ART_TAPE = [
    "................", "................", ".KKKKKKKKKKKKKK.", ".KGGGGGGGGGGGGK.",
    ".KGKKKKKKKKKKGK.", ".KGKWWWWWWWWKGK.", ".KGKWRRRRRRWKGK.", ".KGKWWWWWWWWKGK.",
    ".KGKKKKKKKKKKGK.", ".KGGWWGGGGWWGGK.", ".KGGWKGGGGKWGGK.", ".KGGWWGGGGWWGGK.",
    ".KKKKKKKKKKKKKK.", "................", "................", "................",
]
PAL = {"d": 0x2A0A4A, "L": 0xC58BFF, "M": 0x7A3BD0, "K": 0x16161A, "G": 0x3C3C48, "W": 0xE0E0E0, "R": 0xB01020}


def _ascii_img(art):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in PAL:
                im.putpixel((x, y), rgb(PAL[ch]))
    return im


def _eye_img():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 6.5, (y - 7.5) / 5.0
            d = dx * dx + dy * dy
            if d > 1.0:
                continue
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 0.82:
                c = 0x6A1A1A                      # outline
            elif r < 1.6:
                c = 0x050505                      # pupil
            elif r < 3.6:
                c = 0xFF2020 if r < 3.0 else 0xA00000   # glowing red iris
            else:
                c = 0xEDE6E0                      # sclera
            im.putpixel((x, y), rgb(c))
    for x, y in ((3, 6), (4, 9), (11, 5), (12, 9), (10, 11)):   # veins
        im.putpixel((x, y), rgb(0xC03030))
    im.putpixel((9, 6), rgb(0xFFFFFF))
    return im


def _heart_img():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            u, v = (x - 7.5) / 6.2, -(y - 7.0) / 6.2
            if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                shade = 0.55 + 0.45 * (1 - (y / 15.0))
                r, g, b = int(150 * shade + 20), int(8 * shade), int(30 * shade + 30)
                im.putpixel((x, y), (min(255, r), g, min(255, b), 255))
    edge = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3] == 0:
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < 16 and 0 <= ny < 16 and im.getpixel((nx, ny))[3] > 0:
                        edge.putpixel((x, y), rgb(0x8A30FF))
                        break
    im.alpha_composite(edge)
    for x, y in ((4, 4), (5, 3), (4, 5)):
        im.putpixel((x, y), rgb(0xFF8080))
    return im


CUSTOM_ITEMS = {
    "fear_shard":   ("Fear Shard", lambda: _ascii_img(ART_SHARD)),
    "cursed_tape":  ("Cursed Tape", lambda: _ascii_img(ART_TAPE)),
    "monster_eye":  ("Monster Eye", _eye_img),
    "abyss_heart":  ("Abyss Heart", _heart_img),
}


def gen_items():
    for iid, (_, make) in CUSTOM_ITEMS.items():
        make().save(mk(ASSETS, "textures/item", iid + ".png"))
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"ytscare:item/{iid}"}},
                  open(mk(ASSETS, "models/item", iid + ".json"), "w"), indent=2)


# themed vanilla drops: id -> [(item, min, max, chance)]   (chance 1.0 = always)
THEMED = {
    "red_spiral":   [("minecraft:redstone", 1, 3, 1.0), ("minecraft:spider_eye", 1, 1, 0.4)],
    "suit_shade":   [("minecraft:ink_sac", 1, 2, 1.0), ("minecraft:ender_pearl", 1, 1, 0.3)],
    "grin_thing":   [("minecraft:rotten_flesh", 1, 2, 1.0), ("minecraft:gold_nugget", 1, 3, 0.5)],
    "red_hoodie":   [("minecraft:leather", 1, 2, 1.0), ("minecraft:redstone", 1, 2, 0.5)],
    "tuxedo_shade": [("minecraft:coal", 1, 3, 1.0), ("minecraft:ink_sac", 1, 1, 0.4)],
    "split_mask":   [("minecraft:bone", 1, 2, 1.0), ("minecraft:quartz", 1, 2, 0.5)],
    "crown_prince": [("minecraft:gold_nugget", 2, 5, 1.0), ("minecraft:gold_ingot", 1, 1, 0.2)],
    "duck":         [("minecraft:feather", 2, 4, 1.0), ("minecraft:bone", 1, 1, 0.5)],
    "smiley":       [("minecraft:glowstone_dust", 1, 3, 1.0), ("minecraft:amethyst_shard", 1, 2, 0.3)],
    "hood_kid":     [("minecraft:string", 1, 2, 1.0), ("minecraft:phantom_membrane", 1, 1, 0.3)],
    "galaxy":       [("minecraft:ender_pearl", 2, 4, 1.0), ("minecraft:amethyst_shard", 3, 6, 1.0), ("minecraft:diamond", 1, 2, 0.6)],
    "red_king":     [("minecraft:gold_ingot", 3, 6, 1.0), ("minecraft:emerald", 1, 3, 0.8), ("minecraft:golden_apple", 1, 1, 0.2)],
    "beanie":       [("minecraft:bone", 1, 2, 1.0), ("minecraft:paper", 1, 3, 0.6)],
    "panda":        [("minecraft:bamboo", 2, 5, 1.0), ("minecraft:bone", 1, 2, 0.6)],
}
BOSSES = {"galaxy", "red_king"}


def _uni(lo, hi):
    return lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}


def _entry(item, lo, hi, looting=0.0):
    fn = [{"function": "minecraft:set_count", "count": _uni(lo, hi)}]
    if looting:
        fn.append({"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": looting}})
    return {"type": "minecraft:item", "name": item, "functions": fn}


def _pool(item, lo, hi, chance=1.0, looting=0.0, loot_chance=0.0, player_only=False):
    conds = []
    if chance < 1.0:
        conds.append({"condition": "minecraft:random_chance_with_looting", "chance": chance, "looting_multiplier": loot_chance})
    if player_only:
        conds.append({"condition": "minecraft:killed_by_player"})
    pool = {"rolls": 1, "bonus_rolls": 0, "entries": [_entry(item, lo, hi, looting)]}
    if conds:
        pool["conditions"] = conds
    return pool


def gen_loot():
    for mid, _, _ in MONSTERS:
        boss = mid in BOSSES
        pools = [_pool("ytscare:fear_shard", 3 if boss else 1, 6 if boss else 2, looting=1)]
        pools.append(_pool("ytscare:cursed_tape", 1, 1, 1.0 if boss else 0.25, loot_chance=0.05, player_only=True))
        pools.append(_pool("ytscare:monster_eye", 1, 3 if boss else 1, 1.0 if boss else 0.12, loot_chance=0.04, player_only=True))
        if boss:
            pools.append(_pool("ytscare:abyss_heart", 1, 1))
        for item, lo, hi, ch in THEMED[mid]:
            pools.append(_pool(item, lo, hi, ch, looting=1, loot_chance=0.05))
        json.dump({"type": "minecraft:entity", "pools": pools},
                  open(mk(DATA, "loot_tables/entities", mid + ".json"), "w"), indent=2)


def gen_extras():
    gen_eyes()
    gen_items()
    gen_loot()


if __name__ == "__main__":
    import sys
    # The REAL skins are already in textures/entity/. Placeholders are only made if you ask for them.
    if "--placeholder-skins" in sys.argv:
        gen_skins()
    gen_json()
    gen_extras()
    if "--no-audio" not in sys.argv:   # --no-audio keeps your own replaced .ogg files untouched
        gen_audio()
    print("done")
