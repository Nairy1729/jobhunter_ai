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
TOTAL_FRAMES = int(FPS * TOTAL_DURATION)  # 750

# Fonts
FONT_DIR = 'C:/Windows/Fonts'
FONT_MONO_PATH = os.path.join(FONT_DIR, 'consola.ttf')
FONT_SANS_BOLD_PATH = os.path.join(FONT_DIR, 'segoeuib.ttf')
FONT_SANS_REG_PATH = os.path.join(FONT_DIR, 'segoeui.ttf')

font_mono_sm = ImageFont.truetype(FONT_MONO_PATH, 20)
font_mono_md = ImageFont.truetype(FONT_MONO_PATH, 24)
font_sans_hero = ImageFont.truetype(FONT_SANS_BOLD_PATH, 74)
font_sans_title = ImageFont.truetype(FONT_SANS_BOLD_PATH, 42)
font_sans_sub = ImageFont.truetype(FONT_SANS_REG_PATH, 26)
font_sans_badge = ImageFont.truetype(FONT_MONO_PATH, 20)

# Load UI frame images (1080 x 1920)
raw_frames = {
    'hero': Image.open(os.path.join(FRAMES_DIR, '01_hero.png')).convert('RGBA'),
    'jobs': Image.open(os.path.join(FRAMES_DIR, '02_jobs_feed.png')).convert('RGBA'),
    'why': Image.open(os.path.join(FRAMES_DIR, '03_job_detail_why.png')).convert('RGBA'),
    'tailor': Image.open(os.path.join(FRAMES_DIR, '04_resume_tailoring.png')).convert('RGBA'),
    'gov': Image.open(os.path.join(FRAMES_DIR, '05_government_feed.png')).convert('RGBA'),
    'profile': Image.open(os.path.join(FRAMES_DIR, '07_profile_hub.png')).convert('RGBA'),
}

# Pre-generate gradient overlays
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

# Scene definitions for Vertical Video (1080x1920)
def render_scene_1(progress, w=1080, h=1920):
    # Hook: 0.0s - 3.2s
    scale = 1.0 + 0.04 * progress
    crop_w, crop_h = int(w / scale), int(h / scale)
    cx, cy = w // 2, int(h * 0.42)
    x1, y1 = max(0, cx - crop_w // 2), max(0, cy - crop_h // 2)
    cropped = raw_frames['hero'].crop((x1, y1, x1 + crop_w, y1 + crop_h)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    # Monospace Pill
    draw_pill(draw, [70, 140, 520, 190], '// SEARCH FATIGUE IS REAL', font_mono_md)
    # Title
    draw.text((70, 215), "Finding the right job shouldn't mean\nsifting through 1,000s of listings.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    # Subtitle
    draw.text((70, 340), "Traditional job boards dump endless unfiltered noise. Zero signal.", font=font_sans_sub, fill=(148, 163, 184, 255))
    # Bottom callout
    draw_bottom_badge(draw, "⚡ LEGACY PLATFORMS PRIORITIZE VOLUME OVER FIT", font_mono_sm, w, y=1720, accent_color=(244, 63, 94))
    return img

def render_scene_2(progress, w=1080, h=1920):
    # Solution / Private Jobs: 3.2s - 7.6s
    pan_y = int(smoothstep(progress) * 220)
    cropped = raw_frames['jobs'].crop((0, pan_y, w, pan_y + h - 100)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    # Pill
    draw_pill(draw, [70, 140, 620, 190], '// MEET JOBHUNTER • DISCOVERY ENGINE', font_mono_md)
    # Title
    draw.text((70, 215), "Surfaces roles that genuinely match\nyour verified experience.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    # Subtitle
    draw.text((70, 340), "Grounded in commercial engineering track record. Not buzzwords.", font=font_sans_sub, fill=(148, 163, 184, 255))
    # Bottom Callout
    draw_bottom_badge(draw, "🟢 94% FIT SCORE // VERIFIED RELEVANCE RANKING", font_mono_sm, w, y=1720, accent_color=(16, 185, 129))
    return img

def render_scene_3(progress, w=1080, h=1920):
    # Semantic Match: 7.6s - 12.4s
    scale = 1.0 + 0.05 * smoothstep(progress)
    crop_w, crop_h = int(w / scale), int(h / scale)
    cx, cy = w // 2, int(h * 0.45)
    x1, y1 = max(0, cx - crop_w // 2), max(0, cy - crop_h // 2)
    cropped = raw_frames['why'].crop((x1, y1, x1 + crop_w, y1 + crop_h)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    # Pill
    draw_pill(draw, [70, 140, 580, 190], '// FACTUAL GROUNDING // REASONING', font_mono_md)
    # Title
    draw.text((70, 215), "Not More Jobs. Better Matches.\nSemantic Truth-Checking.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    # Subtitle
    draw.text((70, 340), "Explains exactly why you qualify with evidence citations.", font=font_sans_sub, fill=(148, 163, 184, 255))
    # Bottom Callout
    draw_bottom_badge(draw, "⚡ ZERO GUESSWORK // FACT-CHECKED FIT ANALYSIS", font_mono_sm, w, y=1720, accent_color=(6, 182, 212))
    return img

def render_scene_4(progress, w=1080, h=1920):
    # Resume Tailoring: 12.4s - 17.0s
    pan_y = int(smoothstep(progress) * 160)
    cropped = raw_frames['tailor'].crop((0, pan_y, w, pan_y + h - 80)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    # Pill
    draw_pill(draw, [70, 140, 600, 190], '// OFFERPILOT ATS OPTIMIZATION', font_mono_md)
    # Title
    draw.text((70, 215), "Truth-Grounded Resume Tailoring.\nWithout Hallucinating Skills.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    # Subtitle
    draw.text((70, 340), "Reframes existing accomplishments into clean ATS-ready LaTeX.", font=font_sans_sub, fill=(148, 163, 184, 255))
    # Bottom Callout
    draw_bottom_badge(draw, "📄 LATEX COMPILED // 100% FACTUAL // ZERO FABRICATION", font_mono_sm, w, y=1720, accent_color=(16, 185, 129))
    return img

def render_scene_5(progress, w=1080, h=1920):
    # Government Jobs Engine: 17.0s - 21.4s
    pan_y = int(smoothstep(progress) * 220)
    cropped = raw_frames['gov'].crop((0, pan_y, w, pan_y + h - 100)).resize((w, h), Image.Resampling.BILINEAR)

    img = cropped.copy()
    img.alpha_composite(TOP_OVERLAY_V)
    img.alpha_composite(BOT_OVERLAY_V)

    draw = ImageDraw.Draw(img, 'RGBA')
    # Pill
    draw_pill(draw, [70, 140, 640, 190], '// ALL-INDIA PUBLIC SECTOR ENGINE', font_mono_md)
    # Title
    draw.text((70, 215), "Government Jobs Discovery.\nVerified Official Portals.", font=font_sans_title, fill=(248, 250, 252, 255), spacing=12)
    # Subtitle
    draw.text((70, 340), "Central, State, Samvida, Municipal & Scheme-based vacancies.", font=font_sans_sub, fill=(148, 163, 184, 255))
    # Bottom Callout
    draw_bottom_badge(draw, "🏛️ NO RESUME TAILORING // 100% DIRECT OFFICIAL LINKS", font_mono_sm, w, y=1720, accent_color=(245, 158, 11))
    return img

def render_scene_6(progress, w=1080, h=1920):
    # Final Payoff: 21.4s - 25.0s
    scale = 1.0 + 0.03 * (1.0 - progress)
    crop_w, crop_h = int(w / scale), int(h / scale)
    cx, cy = w // 2, h // 2
    x1, y1 = max(0, cx - crop_w // 2), max(0, cy - crop_h // 2)
    base = raw_frames['profile'].crop((x1, y1, x1 + crop_w, y1 + crop_h)).resize((w, h), Image.Resampling.BILINEAR)

    # Dark cinematic wash
    wash = Image.new('RGBA', (w, h), (7, 10, 15, 215))
    base.paste(wash, (0, 0), wash)

    draw = ImageDraw.Draw(base, 'RGBA')

    # Central Glass Card
    card_w, card_h = 940, 720
    x1, y1 = cx - card_w // 2, cy - card_h // 2
    x2, y2 = cx + card_w // 2, cy + card_h // 2

    # Glass Card background
    draw.rounded_rectangle([x1, y1, x2, y2], radius=28, fill=(13, 19, 29, 240), outline=(255, 255, 255, 30), width=1)
    draw.rounded_rectangle([x1 + 40, y1, x2 - 40, y1 + 3], radius=2, fill=(16, 185, 129, 220))

    # Mono pill
    pill_w = 460
    draw.rounded_rectangle([cx - pill_w // 2, y1 + 60, cx + pill_w // 2, y1 + 105], radius=12, fill=(16, 185, 129, 30), outline=(16, 185, 129, 90), width=1)
    draw.ellipse([cx - pill_w // 2 + 18, y1 + 78, cx - pill_w // 2 + 28, y1 + 88], fill=(16, 185, 129, 255))
    draw.text((cx - pill_w // 2 + 40, y1 + 72), '// PRODUCTION READY ARCHITECTURE', font=font_mono_md, fill=(16, 185, 129, 255))

    # Big Title
    draw.text((cx, y1 + 180), 'JOBHUNTER', font=font_sans_hero, fill=(248, 250, 252, 255), anchor='mm')

    # Subtitle
    draw.text((cx, y1 + 245), 'Intelligent Private & Government Job Engine', font=font_sans_sub, fill=(148, 163, 184, 255), anchor='mm')

    # Divider
    draw.line([(x1 + 80, y1 + 295), (x2 - 80, y1 + 295)], fill=(255, 255, 255, 20), width=1)

    # Tech Stack Badges
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

    # Bottom Developer Credit
    draw.rounded_rectangle([cx - 240, y2 - 130, cx + 240, y2 - 65], radius=16, fill=(16, 185, 129, 20), outline=(16, 185, 129, 80), width=1)
    draw.text((cx, y2 - 108), 'ENGINEERED BY NARENDRA', font=font_mono_md, fill=(16, 185, 129, 255), anchor='mm')
    draw.text((cx, y2 - 84), 'Available for Client Projects & High-Impact Roles', font=font_sans_badge, fill=(148, 163, 184, 255), anchor='mm')

    # Fade out at the very end
    if progress > 0.88:
        fade = (progress - 0.88) / 0.12
        fade_mask = Image.new('RGBA', (w, h), (0, 0, 0, int(255 * fade)))
        base.paste(fade_mask, (0, 0), fade_mask)

    return base

# Timeline Map (Vertical): 750 frames
timeline_v = [
    (0, 95, render_scene_1),      # 0.0s - 3.17s (Hook)
    (96, 229, render_scene_2),    # 3.2s - 7.63s (Meet JobHunter)
    (230, 374, render_scene_3),   # 7.67s - 12.47s (Semantic Why)
    (375, 514, render_scene_4),   # 12.5s - 17.13s (Resume Tailoring)
    (515, 644, render_scene_5),   # 17.17s - 21.47s (Government Engine)
    (645, 749, render_scene_6),   # 21.5s - 25.0s (Final Payoff)
]

def get_vertical_frame(f_idx):
    for i, (f_start, f_end, render_fn) in enumerate(timeline_v):
        if f_start <= f_idx <= f_end:
            # Check for crossfade with next scene
            t = (f_idx - f_start) / max(1, (f_end - f_start))
            img_cur = render_fn(t)
            
            # 8-frame cross-dissolve to next scene
            fade_len = 8
            if f_idx >= f_end - fade_len and i < len(timeline_v) - 1:
                next_fn = timeline_v[i + 1][2]
                alpha = (f_idx - (f_end - fade_len)) / fade_len
                img_next = next_fn(0.0)
                return Image.blend(img_cur.convert('RGB'), img_next.convert('RGB'), alpha)
            return img_cur.convert('RGB')
    return render_scene_6(1.0).convert('RGB')

def render_vertical_video():
    out_mp4 = os.path.join(OUTPUT_DIR, 'jobhunter_showcase_vertical.mp4')
    print(f'Starting Vertical Video Render (1080x1920, 30fps, 25.0s) -> {out_mp4}...')

    cmd = [
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
        out_mp4
    ]

    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

    for f in range(TOTAL_FRAMES):
        frame_img = get_vertical_frame(f)
        proc.stdin.write(frame_img.tobytes())
        if f % 75 == 0:
            print(f'  Rendered frame {f}/{TOTAL_FRAMES} ({(f / TOTAL_FRAMES * 100):.1f}%)')

    proc.stdin.close()
    stderr = proc.stderr.read().decode('utf-8')
    proc.wait()

    if proc.returncode != 0:
        print('FFmpeg Error:', stderr)
        sys.exit(1)

    print(f'Vertical Video Render Complete! Saved to: {out_mp4}')

    # Save Poster Frame
    poster_frame = get_vertical_frame(200)  # Frame from Scene 2
    poster_path = os.path.join(OUTPUT_DIR, 'jobhunter_poster.png')
    poster_frame.save(poster_path)
    print(f'Vertical Poster saved to: {poster_path}')

if __name__ == '__main__':
    render_vertical_video()
