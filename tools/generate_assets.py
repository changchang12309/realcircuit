# -*- coding: utf-8 -*-
"""
真实物理电路 模组 - 资源生成器
仅生成 材质(PNG) + 模型/状态(JSON) 等资源文件，不包含任何 Java 代码。
运行后输出到 ../src/main/resources/assets/realcircuit/
"""
import os, json
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "realcircuit")
TEX_BLK = os.path.join(ASSETS, "textures", "block")
TEX_ITEM = os.path.join(ASSETS, "textures", "item")
MOD_BLK = os.path.join(ASSETS, "models", "block")
MOD_ITEM = os.path.join(ASSETS, "models", "item")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
MODID = "realcircuit"

for d in (TEX_BLK, TEX_ITEM, MOD_BLK, MOD_ITEM, BLOCKSTATES):
    os.makedirs(d, exist_ok=True)

# ---------------- 字体 ----------------
def load_font(size):
    for p in ("C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/msyh.ttc"):
        try:
            return ImageFont.truetype(p, size)
        except Exception:
            pass
    return ImageFont.load_default()

F = load_font(10)
FS = load_font(8)

S = 32  # 材质尺寸

def new_img(bg=(40, 40, 48)):
    img = Image.new("RGBA", (S, S), bg + (255,))
    return img, ImageDraw.Draw(img)

def save(img, name):
    img.save(os.path.join(TEX_BLK, name + ".png"))

def ctext(d, txt, y, fill, font=F):
    w = d.textbbox((0, 0), txt, font=font)[2]
    d.text(((S - w) / 2, y), txt, fill=fill, font=font)

# ---------------- 材质绘制 ----------------

def tex_resistor(value, damaged):
    if damaged:
        img, d = new_img((30, 28, 28))
        d.rectangle([4, 12, 28, 20], fill=(60, 55, 52))
        d.line([6, 14, 26, 18], fill=(20, 20, 20), width=2)
        d.line([26, 14, 6, 18], fill=(20, 20, 20), width=2)
        ctext(d, "X", 22, (180, 60, 60))
    else:
        img, d = new_img((44, 50, 60))
        d.rectangle([4, 11, 28, 21], fill=(206, 168, 110), outline=(120, 90, 50))
        d.rectangle([2, 13, 6, 19], fill=(190, 190, 190))   # 左帽
        d.rectangle([26, 13, 30, 19], fill=(190, 190, 190))  # 右帽
        ctext(d, f"{value}", 13, (20, 20, 20))
    return img

def tex_rheostat(maxr):
    img, d = new_img((44, 50, 60))
    d.rectangle([3, 20, 29, 27], fill=(90, 90, 95))  # 底座
    # 线圈折线
    for i in range(5):
        x = 5 + i * 5
        d.line([x, 18, x + 3, 12, x + 5, 18], fill=(200, 150, 80), width=2)
    d.line([4, 18, 29, 18], fill=(200, 150, 80), width=1)
    d.rectangle([14, 8, 18, 18], fill=(220, 220, 220))  # 滑片
    ctext(d, f"{maxr}", 1, (140, 220, 255), FS)
    return img

def tex_meter(letter, ranges, electronic):
    img, d = new_img((36, 40, 50))
    d.ellipse([4, 4, 28, 28], fill=(20, 24, 32), outline=(120, 200, 120) if electronic else (220, 220, 220), width=2)
    d.line([16, 16, 22, 10], fill=(255, 80, 80), width=2)  # 指针
    ctext(d, letter, 18, (120, 255, 120) if electronic else (255, 255, 255))
    ctext(d, ranges, 27, (180, 180, 180), FS)
    return img

def tex_led():
    img, d = new_img((44, 50, 60))
    d.polygon([(9, 16), (9, 24), (17, 20)], fill=(220, 60, 60), outline=(255, 255, 255))
    d.rectangle([17, 15, 19, 25], fill=(120, 120, 120))  # 阴极条
    d.line([20, 16, 26, 11], fill=(255, 255, 120), width=1)
    d.line([20, 24, 26, 29], fill=(255, 255, 120), width=1)
    ctext(d, "+", 2, (120, 255, 120), FS)
    ctext(d, "-", 27, (255, 120, 120), FS)
    return img

def tex_power(volts, student):
    img, d = new_img((40, 55, 45))
    d.rectangle([6, 8, 26, 24], fill=(60, 60, 70), outline=(120, 200, 255))
    d.rectangle([26, 11, 29, 21], fill=(120, 120, 130))  # 正极凸起
    d.line([9, 16, 9, 19], fill=(220, 220, 220), width=2)
    d.line([7, 17, 11, 17], fill=(220, 220, 220), width=2)
    if student:
        ctext(d, "0~220V", 1, (140, 220, 255), FS)
    else:
        ctext(d, f"{volts}V", 1, (140, 220, 255), FS)
    return img

def tex_switch(closed):
    img, d = new_img((44, 50, 60))
    d.line([6, 22, 13, 22], fill=(200, 200, 200), width=2)
    d.line([19, 22, 26, 22], fill=(200, 200, 200), width=2)
    d.ellipse([5, 20, 8, 24], fill=(220, 220, 120))
    d.ellipse([24, 20, 27, 24], fill=(220, 220, 120))
    if closed:
        d.line([13, 22, 20, 14], fill=(255, 200, 80), width=2)
        ctext(d, "CLOSED", 26, (120, 255, 120), FS)
    else:
        d.line([13, 22, 16, 13], fill=(160, 160, 160), width=2)
        ctext(d, "OPEN", 26, (255, 120, 120), FS)
    return img

def tex_fuse(damaged):
    img, d = new_img((44, 50, 60))
    d.rectangle([5, 14, 27, 18], fill=(200, 200, 200))
    if damaged:
        d.line([8, 16, 24, 12], fill=(40, 40, 40), width=2)
        ctext(d, "BLOWN", 22, (255, 120, 120), FS)
    else:
        d.line([8, 16, 24, 16], fill=(255, 255, 200), width=1)
        ctext(d, "FUSE", 22, (180, 180, 180), FS)
    return img

def tex_bulb(spec, damaged):
    img, d = new_img((40, 46, 56))
    if damaged:
        d.ellipse([9, 6, 23, 20], fill=(50, 50, 55), outline=(30, 30, 30))
        d.line([11, 10, 21, 16], fill=(20, 20, 20), width=1)
        d.line([21, 10, 11, 16], fill=(20, 20, 20), width=1)
        d.rectangle([12, 20, 20, 25], fill=(70, 70, 70))
        ctext(d, "X", 26, (200, 80, 80))
    else:
        d.ellipse([9, 6, 23, 20], fill=(255, 240, 180), outline=(255, 220, 120))
        d.line([14, 11, 18, 15], fill=(255, 140, 60), width=1)
        d.rectangle([12, 20, 20, 25], fill=(150, 150, 150))
        ctext(d, spec, 26, (255, 230, 150), FS)
    return img

# ---------------- 生成所有材质 ----------------
made_textures = []

def add(name, img):
    save(img, name)
    made_textures.append(name)

RES = [5, 10, 20, 50, 100, 200, 500, 1000]
for v in RES:
    add(f"resistor_{v}", tex_resistor(v, False))
    add(f"resistor_{v}_damaged", tex_resistor(v, True))

for m in (20, 50, 100, 200):
    add(f"rheostat_{m}", tex_rheostat(m))

add("ammeter_mechanical", tex_meter("A", "0.6/3", False))
add("voltmeter_mechanical", tex_meter("V", "3/15", False))
add("ammeter_electronic", tex_meter("A", "0~220", True))
add("voltmeter_electronic", tex_meter("V", "0~220", True))
add("led", tex_led())

for tag, volts in (("1_5v", "1.5"), ("3v", "3"), ("6v", "6"), ("9v", "9"), ("12v", "12")):
    add(f"power_dc_{tag}", tex_power(volts, False))
add("power_student", tex_power("", True))

add("switch_open", tex_switch(False))
add("switch_closed", tex_switch(True))
add("fuse", tex_fuse(False))
add("fuse_damaged", tex_fuse(True))

BULBS = [("0_3v_0_2a", "0.3V0.2A"), ("2_5v_0_3a", "2.5V0.3A"), ("3_8v_0_3a", "3.8V0.3A")]
for key, spec in BULBS:
    add(f"bulb_{key}", tex_bulb(spec, False))
    add(f"bulb_{key}_damaged", tex_bulb(spec, True))

# ---------------- 模型 / 状态 JSON ----------------
def jwrite(path, obj):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

def cube_model(tex):
    return {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"{MODID}:block/{tex}"}
    }

def blockstate_simple(model):
    return {"variants": {"": {"model": f"{MODID}:block/{model}"}}}

def item_model(parent_block):
    return {"parent": f"{MODID}:block/{parent_block}"}

# 简单立方体方块：材质名 == 方块注册名
SIMPLE_CUBE = [
    "ammeter_electronic", "voltmeter_electronic", "led",
    "power_student", "fuse", "fuse_damaged",
]
SIMPLE_CUBE += [f"power_dc_{t}" for t, _ in (("1_5v", None), ("3v", None), ("6v", None), ("9v", None), ("12v", None))]
SIMPLE_CUBE += [f"resistor_{v}" for v in RES] + [f"resistor_{v}_damaged" for v in RES]
SIMPLE_CUBE += [f"bulb_{k}" for k, _ in BULBS] + [f"bulb_{k}_damaged" for k, _ in BULBS]

for name in SIMPLE_CUBE:
    jwrite(os.path.join(MOD_BLK, name + ".json"), cube_model(name))
    jwrite(os.path.join(BLOCKSTATES, name + ".json"), blockstate_simple(name))
    jwrite(os.path.join(MOD_ITEM, name + ".json"), item_model(name))

# 滑动变阻器：自定义 4 接线柱模型
def rheostat_model(name):
    return {
        "parent": "block/block",
        "textures": {"all": f"{MODID}:block/{name}", "particle": f"{MODID}:block/{name}"},
        "elements": [
            {"from": [2, 0, 2], "to": [14, 5, 14], "faces": {"all": {"texture": "#all"}}},
            {"from": [3, 5, 7], "to": [13, 8, 9], "faces": {"all": {"texture": "#all"}}},
            {"from": [4, 8, 4], "to": [6, 14, 6], "faces": {"all": {"texture": "#all"}}},
            {"from": [10, 8, 4], "to": [12, 14, 6], "faces": {"all": {"texture": "#all"}}},
            {"from": [4, 5, 10], "to": [6, 8, 12], "faces": {"all": {"texture": "#all"}}},
            {"from": [10, 5, 10], "to": [12, 8, 12], "faces": {"all": {"texture": "#all"}}},
        ],
    }

for m in (20, 50, 100, 200):
    name = f"rheostat_{m}"
    jwrite(os.path.join(MOD_BLK, name + ".json"), rheostat_model(name))
    jwrite(os.path.join(BLOCKSTATES, name + ".json"), blockstate_simple(name))
    jwrite(os.path.join(MOD_ITEM, name + ".json"), item_model(name))

# 机械式电表：3x1x1 宽体 + 3 接线柱
def meter_model(name):
    return {
        "parent": "block/block",
        "textures": {"all": f"{MODID}:block/{name}", "particle": f"{MODID}:block/{name}"},
        "elements": [
            {"from": [-16, 0, 5], "to": [32, 10, 11], "faces": {"all": {"texture": "#all"}}},
            {"from": [-12, 10, 7], "to": [-8, 15, 9], "faces": {"all": {"texture": "#all"}}},
            {"from": [4, 10, 7], "to": [8, 15, 9], "faces": {"all": {"texture": "#all"}}},
            {"from": [20, 10, 7], "to": [24, 15, 9], "faces": {"all": {"texture": "#all"}}},
        ],
    }

for name in ("ammeter_mechanical", "voltmeter_mechanical"):
    jwrite(os.path.join(MOD_BLK, name + ".json"), meter_model(name))
    jwrite(os.path.join(BLOCKSTATES, name + ".json"), blockstate_simple(name))
    jwrite(os.path.join(MOD_ITEM, name + ".json"), item_model(name))

# 单刀开关：open / closed 两种状态
jwrite(os.path.join(MOD_BLK, "switch_open.json"), cube_model("switch_open"))
jwrite(os.path.join(MOD_BLK, "switch_closed.json"), cube_model("switch_closed"))
jwrite(os.path.join(BLOCKSTATES, "switch.json"), {
    "variants": {
        "open=true": {"model": f"{MODID}:block/switch_open"},
        "open=false": {"model": f"{MODID}:block/switch_closed"},
    }
})
jwrite(os.path.join(MOD_ITEM, "switch.json"), item_model("switch_closed"))

# 复制一份材质到 item 目录作为物品图标（可选，部分方块物品直接复用 block 材质）
for name in made_textures:
    src = os.path.join(TEX_BLK, name + ".png")
    dst = os.path.join(TEX_ITEM, name + ".png")
    if not os.path.exists(dst):
        import shutil
        shutil.copy(src, dst)

print(f"[OK] 材质 {len(made_textures)} 张")
print(f"[OK] 简单立方块模型 {len(SIMPLE_CUBE)} 个")
print("[OK] 滑动变阻器/电表/开关 自定义模型已生成")
print("DONE")
