import os
import sys
import math
import subprocess
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# Paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(BASE_DIR)
FRAMES_DIR = os.path.join(BASE_DIR, 'frames')
OUTPUT_DIR = os.path.join(PROJECT_DIR, 'output')
os.makedirs(OUTPUT_DIR, exist_ok=True)

MASTER_AUDIO = os.path.join(BASE_DIR, 'master_audio_25s.wav')

# Configuration
FPS = 30
TOTAL_DURATION = 25.0
TOTAL_FRAMES = int(FPS * TOTAL_DURATION)  # 750 frames

# Fonts
FONT_DIR = 'C:/Windows/Fonts'
FONT_MONO_PATH = os.path.join(FONT_DIR, 'consola.ttf')
FONT_SANS_BOLD_PATH = os.path.join(FONT_DIR, 'segoeuib.ttf')
FONT_SANS_REG_PATH = os.path.join(FONT_DIR, 'segoeui.ttf')

font_mono_xs = ImageFont.truetype(FONT_MONO_PATH, 18)
font_mono_sm = ImageFont.truetype(FONT_MONO_PATH, 22)
font_mono_md = ImageFont.truetype(FONT_MONO_PATH, 25)
font_sans_hero = ImageFont.truetype(FONT_SANS_BOLD_PATH, 74)
font_sans_title = ImageFont.truetype(FONT_SANS_BOLD_PATH, 42)
font_sans_title_ls = ImageFont.truetype(FONT_SANS_BOLD_PATH, 46)
font_sans_sub = ImageFont.truetype(FONT_SANS_REG_PATH, 26)
font_sans_sub_ls = ImageFont.truetype(FONT_SANS_REG_PATH, 24)
font_sans_badge = ImageFont.truetype(FONT_MONO_PATH, 20)

# Load real UI screenshots
print("Loading UI frames...")
raw_frames = {
    'hero': Image.open(os.path.join(FRAMES_DIR, '01_hero.png')).convert('RGBA'),
    'jobs': Image.open(os.path.join(FRAMES_DIR, '02_jobs_feed.png')).convert('RGBA'),
    'why': Image.open(os.path.join(FRAMES_DIR, '03_job_detail_why.png')).convert('RGBA'),
    'tailor': Image.open(os.path.join(FRAMES_DIR, '04_resume_tailoring.png')).convert('RGBA'),
    'gov': Image.open(os.path.join(FRAMES_DIR, '05_government_feed.png')).convert('RGBA'),
    'profile': Image.open(os.path.join(FRAMES_DIR, '07_profile_hub.png')).convert('RGBA'),
}

# Pre-generate gradient overlays for Vertical (1080x1920)
def create_vertical_overlays(w, h):
    top_overlay = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d_top = ImageDraw.Draw(top_overlay)
    for y in range(480):
        alpha = int(225 * (1.0 - y / 480) ** 1.3)
        d_top.line([(0, y), (w, y)], fill=(7, 10, 15, alpha))

    bot_overlay = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d_bot = ImageDraw.Draw(bot_overlay)
    for y in range(h - 380, h):
        progress = (y - (h - 380)) / 380
        alpha = int(230 * (progress ** 1.3))
        d_bot.line([(0, y), (w, y)], fill=(7, 10, 15, alpha))

    return top_overlay, bot_overlay

TOP_OVERLAY_V, BOT_OVERLAY_V = create_vertical_overlays(1080, 1920)

def smoothstep(x):
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)

def draw_pill(draw, xy, text, font, bg_color=(16, 185, 129, 35), border_color=(16, 185, 129, 90), text_color=(16, 185, 129, 255), dot_color=(16, 185, 129, 255)):
    x1, y1, x2, y2 = xy
    draw.rounded_rectangle([x1, y1, x2, y2], radius=12, fill=bg_color, outline=border_color, width=1)
    if dot_color:
        cy = (y1 + y2) // 2
        draw.ellipse([x1 + 18, cy - 5, x1 + 28, cy + 5], fill=dot_color)
        draw.text((x1 + 40, y1 + (y2 - y1 - 24) // 2), text, font=font, fill=text_color)
    else:
        draw.text((x1 + 20, y1 + (y2 - y1 - 24) // 2), text, font=font, fill=text_color)

def draw_bottom_badge(draw, text, font, w, y=1720, accent_color=(16, 185, 129)):
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0] + 60
    cx = w // 2
    x1, x2 = cx - tw // 2, cx + tw // 2
    y1, y2 = y, y + 54
    draw.rounded_rectangle([x1, y1, x2, y2], radius=14, fill=(13, 19, 29, 230), outline=(accent_color[0], accent_color[1], accent_color[2], 100), width=1)
    draw.text((cx, y1 + 14), text, font=font, fill=(248, 250, 252, 255), anchor='mt')

# ==============================================================================
# VERTICAL SCENES (1080 x 1920)
# ==============================================================================
def render_v_scene_1(progress, w=1080, h=1920):
    scale = 1.0 + 0.04 * progress
    crop_w, crop_h = int(w / scale), int(h / scale)
    cx, cy = w // 2, int(h * 0.42)
    x1, y1 = max(0, cx - crop_w // 2), max(0, cy - crop_h // 2)
    cropped = raw_frames['hero'].crop((x1, y1, x1 + crop_w, y1 + crop_h)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    draw_pill(draw, [70, 140, 520, 190], '// SEARCH FATIGUE IS REAL', font_mono_md)
    draw.text((70, 215), "Finding the right job shouldn't mean\nsifting through 1,000s of listings.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    draw.text((70, 340), "Traditional job boards dump endless unfiltered noise. Zero signal.", font=font_sans_sub, fill=(148, 163, 184, 255))
    draw_bottom_badge(draw, "⚡ LEGACY PLATFORMS PRIORITIZE VOLUME OVER FIT", font_mono_sm, w, y=1720, accent_color=(244, 63, 94))
    return img

def render_v_scene_2(progress, w=1080, h=1920):
    pan_y = int(smoothstep(progress) * 220)
    cropped = raw_frames['jobs'].crop((0, pan_y, w, pan_y + h - 100)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    draw_pill(draw, [70, 140, 620, 190], '// MEET JOBHUNTER • DISCOVERY ENGINE', font_mono_md)
    draw.text((70, 215), "Surfaces roles that genuinely match\nyour verified experience.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    draw.text((70, 340), "Grounded in commercial engineering track record. Not buzzwords.", font=font_sans_sub, fill=(148, 163, 184, 255))
    draw_bottom_badge(draw, "🟢 94% FIT SCORE // VERIFIED RELEVANCE RANKING", font_mono_sm, w, y=1720, accent_color=(16, 185, 129))
    return img

def render_v_scene_3(progress, w=1080, h=1920):
    scale = 1.0 + 0.05 * smoothstep(progress)
    crop_w, crop_h = int(w / scale), int(h / scale)
    cx, cy = w // 2, int(h * 0.45)
    x1, y1 = max(0, cx - crop_w // 2), max(0, cy - crop_h // 2)
    cropped = raw_frames['why'].crop((x1, y1, x1 + crop_w, y1 + crop_h)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    draw_pill(draw, [70, 140, 580, 190], '// FACTUAL GROUNDING // REASONING', font_mono_md)
    draw.text((70, 215), "Not More Jobs. Better Matches.\nSemantic Truth-Checking.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    draw.text((70, 340), "Explains exactly why you qualify with evidence citations.", font=font_sans_sub, fill=(148, 163, 184, 255))
    draw_bottom_badge(draw, "⚡ ZERO GUESSWORK // FACT-CHECKED FIT ANALYSIS", font_mono_sm, w, y=1720, accent_color=(6, 182, 212))
    return img

def render_v_scene_4(progress, w=1080, h=1920):
    pan_y = int(smoothstep(progress) * 160)
    cropped = raw_frames['tailor'].crop((0, pan_y, w, pan_y + h - 80)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    draw_pill(draw, [70, 140, 600, 190], '// OFFERPILOT ATS OPTIMIZATION', font_mono_md)
    draw.text((70, 215), "Truth-Grounded Resume Tailoring.\nWithout Hallucinating Skills.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    draw.text((70, 340), "Reframes existing accomplishments into clean ATS-ready LaTeX.", font=font_sans_sub, fill=(148, 163, 184, 255))
    draw_bottom_badge(draw, "📄 LATEX COMPILED // 100% FACTUAL // ZERO FABRICATION", font_mono_sm, w, y=1720, accent_color=(16, 185, 129))
    return img

def render_v_scene_5(progress, w=1080, h=1920):
    pan_y = int(smoothstep(progress) * 220)
    cropped = raw_frames['gov'].crop((0, pan_y, w, pan_y + h - 100)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    draw_pill(draw, [70, 140, 640, 190], '// ALL-INDIA PUBLIC SECTOR ENGINE', font_mono_md)
    draw.text((70, 215), "Government Jobs Discovery.\nVerified Official Portals.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    draw.text((70, 340), "Central, State, Samvida, Municipal & Scheme-based vacancies.", font=font_sans_sub, fill=(148, 163, 184, 255))
    draw_bottom_badge(draw, "🏛️ NO RESUME TAILORING // 100% DIRECT OFFICIAL LINKS", font_mono_sm, w, y=1720, accent_color=(245, 158, 11))
    return img

def render_v_scene_6(progress, w=1080, h=1920):
    scale = 1.0 + 0.03 * (1.0 - progress)
    crop_w, crop_h = int(w / scale), int(h / scale)
    cx, cy = w // 2, h // 2
    x1, y1 = max(0, cx - crop_w // 2), max(0, cy - crop_h // 2)
    base = raw_frames['profile'].crop((x1, y1, x1 + crop_w, y1 + crop_h)).resize((w, h), Image.Resampling.BILINEAR)

    wash = Image.new('RGBA', (w, h), (7, 10, 15, 215))
    base.paste(wash, (0, 0), wash)

    draw = ImageDraw.Draw(base, 'RGBA')

    card_w, card_h = 940, 720
    x1, y1 = cx - card_w // 2, cy - card_h // 2
    x2, y2 = cx + card_w // 2, cy + card_h // 2

    draw.rounded_rectangle([x1, y1, x2, y2], radius=28, fill=(13, 19, 29, 240), outline=(255, 255, 255, 30), width=1)
    draw.rounded_rectangle([x1 + 40, y1, x2 - 40, y1 + 3], radius=2, fill=(16, 185, 129, 220))

    pill_w = 460
    draw.rounded_rectangle([cx - pill_w // 2, y1 + 60, cx + pill_w // 2, y1 + 105], radius=12, fill=(16, 185, 129, 30), outline=(16, 185, 129, 90), width=1)
    draw.ellipse([cx - pill_w // 2 + 18, y1 + 78, cx - pill_w // 2 + 28, y1 + 88], fill=(16, 185, 129, 255))
    draw.text((cx - pill_w // 2 + 40, y1 + 72), '// PRODUCTION READY ARCHITECTURE', font=font_mono_md, fill=(16, 185, 129, 255))

    draw.text((cx, y1 + 180), 'JOBHUNTER', font=font_sans_hero, fill=(248, 250, 252, 255), anchor='mm')
    draw.text((cx, y1 + 245), 'Intelligent Private & Government Job Engine', font=font_sans_sub, fill=(148, 163, 184, 255), anchor='mm')
    draw.line([(x1 + 80, y1 + 295), (x2 - 80, y1 + 295)], fill=(255, 255, 255, 20), width=1)

    techs = ['AI Grounding', 'Java Spring Boot', 'React 18', 'PostgreSQL', 'OfferPilot ATS']
    tx = x1 + 70
    ty = y1 + 340
    for t in techs:
        bbox = font_sans_badge.getbbox(t)
        tw = bbox[2] - bbox[0] + 32
        if tx + tw > x2 - 60:
            tx = x1 + 70
            ty += 52
        draw.rounded_rectangle([tx, ty, tx + tw, ty + 38], radius=10, fill=(255, 255, 255, 12), outline=(255, 255, 255, 25), width=1)
        draw.text((tx + 16, ty + 8), t, font=font_sans_badge, fill=(203, 213, 225, 255))
        tx += tw + 14

    draw.rounded_rectangle([cx - 240, y2 - 130, cx + 240, y2 - 65], radius=16, fill=(16, 185, 129, 20), outline=(16, 185, 129, 80), width=1)
    draw.text((cx, y2 - 108), 'ENGINEERED BY NARENDRA', font=font_mono_md, fill=(16, 185, 129, 255), anchor='mm')
    draw.text((cx, y2 - 84), 'Available for Client Projects & High-Impact Roles', font=font_sans_badge, fill=(148, 163, 184, 255), anchor='mm')

    if progress > 0.88:
        fade = (progress - 0.88) / 0.12
        fade_mask = Image.new('RGBA', (w, h), (0, 0, 0, int(255 * fade)))
        base.paste(fade_mask, (0, 0), fade_mask)

    return base

# Timeline Map (Vertical): 750 frames
timeline_v = [
    (0, 95, render_v_scene_1),      # Hook (0.0s - 3.2s)
    (96, 229, render_v_scene_2),    # Solution (3.2s - 7.6s)
    (230, 374, render_v_scene_3),   # Semantic Why (7.6s - 12.5s)
    (375, 514, render_v_scene_4),   # Resume Tailoring (12.5s - 17.1s)
    (515, 644, render_v_scene_5),   # Government Engine (17.1s - 21.5s)
    (645, 749, render_v_scene_6),   # Payoff (21.5s - 25.0s)
]

def get_vertical_frame(f_idx):
    for i, (f_start, f_end, render_fn) in enumerate(timeline_v):
        if f_start <= f_idx <= f_end:
            t = (f_idx - f_start) / max(1, (f_end - f_start))
            img_cur = render_fn(t)
            fade_len = 8
            if f_idx >= f_end - fade_len and i < len(timeline_v) - 1:
                next_fn = timeline_v[i + 1][2]
                alpha = (f_idx - (f_end - fade_len)) / fade_len
                img_next = next_fn(0.0)
                return Image.blend(img_cur.convert('RGB'), img_next.convert('RGB'), alpha)
            return img_cur.convert('RGB')
    return render_v_scene_6(1.0).convert('RGB')

# ==============================================================================
# LANDSCAPE SCENES (1920 x 1080)
# ==============================================================================
# Helper to render a sleek browser/device card on the right side
def render_browser_card(raw_img, pan_ratio, card_w=980, card_h=900):
    card = Image.new('RGBA', (card_w, card_h), (11, 15, 23, 255))
    draw = ImageDraw.Draw(card)

    # Browser Top Chrome bar (height 48)
    draw.rectangle([0, 0, card_w, 48], fill=(18, 24, 38, 255))
    # Traffic light dots
    draw.ellipse([20, 18, 32, 30], fill=(244, 63, 94, 255))  # red
    draw.ellipse([40, 18, 52, 30], fill=(245, 158, 11, 255)) # yellow
    draw.ellipse([60, 18, 72, 30], fill=(16, 185, 129, 255)) # green

    # Address bar
    draw.rounded_rectangle([110, 12, card_w - 40, 36], radius=6, fill=(11, 15, 23, 200), outline=(255, 255, 255, 20), width=1)
    draw.text((124, 15), 'https://jobhunter.ai/verified-pipeline', font=font_mono_xs, fill=(100, 116, 139, 255))

    # Inner viewport content
    vp_w, vp_h = card_w, card_h - 48
    src_w, src_h = raw_img.size
    
    # Crop from raw UI image with pan
    crop_h = int(src_w * (vp_h / vp_w))
    max_pan = max(0, src_h - crop_h)
    pan_y = int(smoothstep(pan_ratio) * max_pan * 0.6)
    
    view_slice = raw_img.crop((0, pan_y, src_w, min(src_h, pan_y + crop_h))).resize((vp_w, vp_h), Image.Resampling.BILINEAR)
    card.paste(view_slice, (0, 48))

    # Border outline
    draw.rounded_rectangle([0, 0, card_w - 1, card_h - 1], radius=16, fill=None, outline=(255, 255, 255, 35), width=1)
    return card

def render_ls_scene_1(progress, w=1920, h=1080):
    img = Image.new('RGBA', (w, h), (7, 10, 15, 255))
    draw = ImageDraw.Draw(img)

    # Ambient glow on right
    draw.ellipse([w - 600, -200, w + 300, 700], fill=(16, 185, 129, 12))

    # Browser card
    card = render_browser_card(raw_frames['hero'], progress)
    img.paste(card, (860, 90), card)

    # Left content
    draw_pill(draw, [100, 140, 520, 190], '// SEARCH FATIGUE IS REAL', font_mono_md)
    draw.text((100, 230), "Finding the right job\nshouldn't mean sifting\nthrough 1,000s of listings.", font=font_sans_title_ls, fill=(248, 250, 252, 255), spacing=12)
    draw.text((100, 440), "Traditional job platforms prioritize volume over fit.\nEndless keyword spam. Zero actual qualification signal.", font=font_sans_sub_ls, fill=(148, 163, 184, 255), spacing=8)

    # Feature box
    draw.rounded_rectangle([100, 620, 760, 750], radius=14, fill=(18, 24, 38, 200), outline=(244, 63, 94, 80), width=1)
    draw.text((130, 650), "⚡ THE PROBLEM // UNFILTERED APPLICANT NOISE", font=font_mono_sm, fill=(244, 63, 94, 255))
    draw.text((130, 690), "Candidates waste 20+ hours a week applying to irrelevant specs.", font=font_sans_sub_ls, fill=(203, 213, 225, 255))

    return img

def render_ls_scene_2(progress, w=1920, h=1080):
    img = Image.new('RGBA', (w, h), (7, 10, 15, 255))
    draw = ImageDraw.Draw(img)

    card = render_browser_card(raw_frames['jobs'], progress)
    img.paste(card, (860, 90), card)

    draw_pill(draw, [100, 140, 640, 190], '// MEET JOBHUNTER • DISCOVERY ENGINE', font_mono_md)
    draw.text((100, 230), "Surfaces opportunities\nthat genuinely match\nyour verified profile.", font=font_sans_title_ls, fill=(248, 250, 252, 255), spacing=12)
    draw.text((100, 440), "Learns what you are looking for and filters out irrelevant listings.\nGrounded in proven engineering commercial experience.", font=font_sans_sub_ls, fill=(148, 163, 184, 255), spacing=8)

    draw.rounded_rectangle([100, 620, 760, 750], radius=14, fill=(18, 24, 38, 200), outline=(16, 185, 129, 80), width=1)
    draw.text((130, 650), "🟢 94% FIT SCORE // VERIFIED RELEVANCE RANKING", font=font_mono_sm, fill=(16, 185, 129, 255))
    draw.text((130, 690), "Ranks top engineering specimens strictly by factual alignment.", font=font_sans_sub_ls, fill=(203, 213, 225, 255))

    return img

def render_ls_scene_3(progress, w=1920, h=1080):
    img = Image.new('RGBA', (w, h), (7, 10, 15, 255))
    draw = ImageDraw.Draw(img)

    card = render_browser_card(raw_frames['why'], progress)
    img.paste(card, (860, 90), card)

    draw_pill(draw, [100, 140, 600, 190], '// FACTUAL GROUNDING // REASONING', font_mono_md)
    draw.text((100, 230), "Not More Jobs.\nBetter Matches.\nSemantic Truth-Checking.", font=font_sans_title_ls, fill=(248, 250, 252, 255), spacing=12)
    draw.text((100, 450), "AI cross-references every JD bullet point against candidate history.\nExplains exactly why you qualify with evidence citations.", font=font_sans_sub_ls, fill=(148, 163, 184, 255), spacing=8)

    draw.rounded_rectangle([100, 620, 760, 750], radius=14, fill=(18, 24, 38, 200), outline=(6, 182, 212, 80), width=1)
    draw.text((130, 650), "⚡ ZERO GUESSWORK // FACT-CHECKED FIT ANALYSIS", font=font_mono_sm, fill=(6, 182, 212, 255))
    draw.text((130, 690), "Deterministic gap analysis: Know your edge before applying.", font=font_sans_sub_ls, fill=(203, 213, 225, 255))

    return img

def render_ls_scene_4(progress, w=1920, h=1080):
    img = Image.new('RGBA', (w, h), (7, 10, 15, 255))
    draw = ImageDraw.Draw(img)

    card = render_browser_card(raw_frames['tailor'], progress)
    img.paste(card, (860, 90), card)

    draw_pill(draw, [100, 140, 620, 190], '// OFFERPILOT ATS OPTIMIZATION', font_mono_md)
    draw.text((100, 230), "Truth-Grounded\nResume Tailoring.\nZero Hallucinations.", font=font_sans_title_ls, fill=(248, 250, 252, 255), spacing=12)
    draw.text((100, 440), "Never invents skills or fabricated projects.\nRestructures existing achievements for ATS parsers in clean LaTeX.", font=font_sans_sub_ls, fill=(148, 163, 184, 255), spacing=8)

    draw.rounded_rectangle([100, 620, 760, 750], radius=14, fill=(18, 24, 38, 200), outline=(16, 185, 129, 80), width=1)
    draw.text((130, 650), "📄 LATEX COMPILED // 100% FACTUAL // ATS READY", font=font_mono_sm, fill=(16, 185, 129, 255))
    draw.text((130, 690), "Real diff view: verify every bullet reframe before downloading.", font=font_sans_sub_ls, fill=(203, 213, 225, 255))

    return img

def render_ls_scene_5(progress, w=1920, h=1080):
    img = Image.new('RGBA', (w, h), (7, 10, 15, 255))
    draw = ImageDraw.Draw(img)

    card = render_browser_card(raw_frames['gov'], progress)
    img.paste(card, (860, 90), card)

    draw_pill(draw, [100, 140, 640, 190], '// ALL-INDIA PUBLIC SECTOR ENGINE', font_mono_md)
    draw.text((100, 230), "Government Jobs Discovery.\nVerified Official Sources.", font=font_sans_title_ls, fill=(248, 250, 252, 255), spacing=12)
    draw.text((100, 390), "Central, State, Samvida, Municipal, Scheme & District recruitment.\nZero resume tailoring — strictly authentic notifications & official links.", font=font_sans_sub_ls, fill=(148, 163, 184, 255), spacing=8)

    draw.rounded_rectangle([100, 580, 760, 740], radius=14, fill=(18, 24, 38, 200), outline=(245, 158, 11, 80), width=1)
    draw.text((130, 610), "🏛️ OFFICIALLY VERIFIED // DIRECT GAZETTES & PORTALS", font=font_mono_sm, fill=(245, 158, 11, 255))
    draw.text((130, 650), "Deterministic eligibility evaluation matching domicile, age & reservation.", font=font_sans_sub_ls, fill=(203, 213, 225, 255))

    return img

def render_ls_scene_6(progress, w=1920, h=1080):
    # Full-width cinematic hero payoff card
    img = Image.new('RGBA', (w, h), (7, 10, 15, 255))
    draw = ImageDraw.Draw(img)

    card_w, card_h = 1380, 760
    cx, cy = w // 2, h // 2
    x1, y1 = cx - card_w // 2, cy - card_h // 2
    x2, y2 = cx + card_w // 2, cy + card_h // 2

    draw.rounded_rectangle([x1, y1, x2, y2], radius=28, fill=(13, 19, 29, 240), outline=(255, 255, 255, 30), width=1)
    draw.rounded_rectangle([x1 + 60, y1, x2 - 60, y1 + 3], radius=2, fill=(16, 185, 129, 220))

    pill_w = 480
    draw.rounded_rectangle([cx - pill_w // 2, y1 + 60, cx + pill_w // 2, y1 + 105], radius=12, fill=(16, 185, 129, 30), outline=(16, 185, 129, 90), width=1)
    draw.ellipse([cx - pill_w // 2 + 18, y1 + 78, cx - pill_w // 2 + 28, y1 + 88], fill=(16, 185, 129, 255))
    draw.text((cx - pill_w // 2 + 40, y1 + 72), '// PRODUCTION READY ARCHITECTURE', font=font_mono_md, fill=(16, 185, 129, 255))

    draw.text((cx, y1 + 190), 'JOBHUNTER', font=font_sans_hero, fill=(248, 250, 252, 255), anchor='mm')
    draw.text((cx, y1 + 265), 'Intelligent Private & Government Job Engine', font=font_sans_sub, fill=(148, 163, 184, 255), anchor='mm')
    draw.line([(x1 + 100, y1 + 325), (x2 - 100, y1 + 325)], fill=(255, 255, 255, 20), width=1)

    techs = ['AI Grounding Engine', 'Java 21 / Spring Boot 3', 'React 18 / TypeScript', 'PostgreSQL / Flyway', 'OfferPilot ATS Engine']
    tx = x1 + 100
    ty = y1 + 380
    for t in techs:
        bbox = font_sans_badge.getbbox(t)
        tw = bbox[2] - bbox[0] + 36
        draw.rounded_rectangle([tx, ty, tx + tw, ty + 42], radius=10, fill=(255, 255, 255, 12), outline=(255, 255, 255, 25), width=1)
        draw.text((tx + 18, ty + 10), t, font=font_sans_badge, fill=(203, 213, 225, 255))
        tx += tw + 18

    draw.rounded_rectangle([cx - 280, y2 - 140, cx + 280, y2 - 60], radius=16, fill=(16, 185, 129, 20), outline=(16, 185, 129, 80), width=1)
    draw.text((cx, y2 - 114), 'ENGINEERED BY NARENDRA', font=font_mono_md, fill=(16, 185, 129, 255), anchor='mm')
    draw.text((cx, y2 - 86), 'Available for Client Projects & High-Impact Engineering Roles', font=font_sans_badge, fill=(148, 163, 184, 255), anchor='mm')

    if progress > 0.88:
        fade = (progress - 0.88) / 0.12
        fade_mask = Image.new('RGBA', (w, h), (0, 0, 0, int(255 * fade)))
        img.paste(fade_mask, (0, 0), fade_mask)

    return img

timeline_ls = [
    (0, 95, render_ls_scene_1),
    (96, 229, render_ls_scene_2),
    (230, 374, render_ls_scene_3),
    (375, 514, render_ls_scene_4),
    (515, 644, render_ls_scene_5),
    (645, 749, render_ls_scene_6),
]

def get_landscape_frame(f_idx):
    for i, (f_start, f_end, render_fn) in enumerate(timeline_ls):
        if f_start <= f_idx <= f_end:
            t = (f_idx - f_start) / max(1, (f_end - f_start))
            img_cur = render_fn(t)
            fade_len = 8
            if f_idx >= f_end - fade_len and i < len(timeline_ls) - 1:
                next_fn = timeline_ls[i + 1][2]
                alpha = (f_idx - (f_end - fade_len)) / fade_len
                img_next = next_fn(0.0)
                return Image.blend(img_cur.convert('RGB'), img_next.convert('RGB'), alpha)
            return img_cur.convert('RGB')
    return render_ls_scene_6(1.0).convert('RGB')

# ==============================================================================
# RENDER EXECUTION
# ==============================================================================
def render_all():
    # 1. Vertical Video (1080x1920)
    out_mp4_v = os.path.join(OUTPUT_DIR, 'jobhunter_showcase_vertical.mp4')
    print(f"\n[1/4] Rendering Vertical Video -> {out_mp4_v}...")

    cmd_v = [
        'ffmpeg', '-y',
        '-f', 'rawvideo',
        '-pix_fmt', 'rgb24',
        '-s', '1080x1920',
        '-r', '30',
        '-i', '-',
        '-i', MASTER_AUDIO,
        '-c:v', 'libx264',
        '-pix_fmt', 'yuv420p',
        '-preset', 'fast',
        '-crf', '18',
        '-c:a', 'aac',
        '-b:a', '192k',
        '-shortest',
        out_mp4_v
    ]

    proc_v = subprocess.Popen(cmd_v, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for f in range(TOTAL_FRAMES):
        frame = get_vertical_frame(f)
        proc_v.stdin.write(frame.tobytes())
        if f % 150 == 0:
            print(f"  Vertical progress: {f}/{TOTAL_FRAMES} ({(f/TOTAL_FRAMES*100):.0f}%)")
    proc_v.stdin.close()
    proc_v.wait()
    print("  Vertical video render complete!")

    # 2. Vertical Poster
    out_poster_v = os.path.join(OUTPUT_DIR, 'jobhunter_poster.png')
    poster_frame_v = get_vertical_frame(200)
    poster_frame_v.save(out_poster_v)
    print(f"[2/4] Vertical poster saved -> {out_poster_v}")

    # 3. Landscape Video (1920x1080)
    out_mp4_ls = os.path.join(OUTPUT_DIR, 'jobhunter_showcase_landscape.mp4')
    print(f"\n[3/4] Rendering Landscape Video -> {out_mp4_ls}...")

    cmd_ls = [
        'ffmpeg', '-y',
        '-f', 'rawvideo',
        '-pix_fmt', 'rgb24',
        '-s', '1920x1080',
        '-r', '30',
        '-i', '-',
        '-i', MASTER_AUDIO,
        '-c:v', 'libx264',
        '-pix_fmt', 'yuv420p',
        '-preset', 'fast',
        '-crf', '18',
        '-c:a', 'aac',
        '-b:a', '192k',
        '-shortest',
        out_mp4_ls
    ]

    proc_ls = subprocess.Popen(cmd_ls, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for f in range(TOTAL_FRAMES):
        frame = get_landscape_frame(f)
        proc_ls.stdin.write(frame.tobytes())
        if f % 150 == 0:
            print(f"  Landscape progress: {f}/{TOTAL_FRAMES} ({(f/TOTAL_FRAMES*100):.0f}%)")
    proc_ls.stdin.close()
    proc_ls.wait()
    print("  Landscape video render complete!")

    # 4. Landscape Poster
    out_poster_ls = os.path.join(OUTPUT_DIR, 'jobhunter_poster_landscape.png')
    poster_frame_ls = get_landscape_frame(200)
    poster_frame_ls.save(out_poster_ls)
    print(f"[4/4] Landscape poster saved -> {out_poster_ls}")

    print("\nALL SHOWCASE MEDIA DELIVERABLES RENDERED SUCCESSFULLY!")

if __name__ == '__main__':
    render_all()
