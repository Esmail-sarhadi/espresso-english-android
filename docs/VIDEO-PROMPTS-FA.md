# چهار پرامپت آمادهٔ ساخت ویدیو با Gemini / Veo 3

هر پرامپت با **یک عکس مشخص** جفت شده است. عکس را ضمیمه کن، متن انگلیسی همان بخش را
کپی کن و بچسبان. صدای ویدیو فارسی است.

| # | عکس (ضمیمه کن) | موضوع | جملهٔ فارسی که گفته می‌شود |
|---|---|---|---|
| ۱ | `docs/video-frames/01-home.png` | صفحهٔ خانه | «انگلیسی را جرعه‌جرعه یاد بگیر…» |
| ۲ | `docs/video-frames/02-lesson.png` | کلاس درس و متن همگام | «هر جمله را با صدا همگام ببین…» |
| ۳ | `docs/video-frames/03-library.png` | کتابخانه، تم روشن | «دویست و هشت درس، از مبتدی تا پیشرفته…» |
| ۴ | `docs/video-frames/04-quiz.png` | آزمون پایان درس | «آخر هر درس، یک آزمون کوتاه…» |

**تنظیمات مشترک هر چهار ویدیو**

- نسبت تصویر: **۹:۱۶** (عمودی)
- مدت: **۸ ثانیه** (سقف Veo 3)
- هر پرامپت را **۲ تا ۳ بار** بساز و بهترین برداشت را بردار
- در اپ Gemini عکس را با دکمهٔ «+» ضمیمه کن و پرامپت را در همان پیام بنویس
- در Flow: حالت **Frames to Video**، عکس را در جایگاه **First frame** بگذار

> **مهم‌ترین نکته:** Veo در بازتولید خط فارسی روی تصویر ضعیف است. هر چهار پرامپت
> عمداً می‌گویند «رابط کاربری را بازطراحی نکن، فقط نور و حرکت را زنده کن». اگر مدل
> شروع کرد به ساختن متن جدید، جملهٔ `keep all UI text pixel-identical` را در ابتدای
> پرامپت تکرار کن.

---

## ۱. صفحهٔ خانه — «جرعه‌جرعه»

**عکس:** `docs/video-frames/01-home.png`
**حس:** معرفی برند. آرام، گرم، بدون شلوغی.

```
Animate the attached image as the first frame of a vertical 9:16 product film.
Keep the composition, the phone position, and every UI element pixel-identical —
do not redraw, restyle, or re-letter the interface.

SCENE: a dark espresso-brown studio backdrop with the Espresso English app open
on a phone. Above the phone sits the amber app icon with a soft glow.

MOTION: the amber glow behind the app icon breathes slowly, like a lamp warming
up. A thin band of warm light sweeps once across the hero card from top to
bottom, revealing it rather than fading it. The two buttons settle with a barely
perceptible 1-pixel lift when the sweep passes. The bottom navigation icons glow
faintly in sequence, right to left.

LIGHT: warm 2700K key light from the upper left, deep shadows, a faint crema
rim-light along the phone's edges. Cinematic, high contrast, no colour cast on
the UI itself.

CAMERA: very slow push-in, about 4% over the full 8 seconds, no shake, no rack
focus. The UI stays tack-sharp; only the background falls off.

AUDIO: A warm, calm male voice speaks Persian with a neutral Iranian accent,
close-mic, unhurried: "انگلیسی را جرعه‌جرعه یاد بگیر. دویست و هشت درس، با متن همگام و ترجمهٔ فارسی — کامل آفلاین."

SOUND: quiet room tone, one soft low hum under the voice, no music.

NEGATIVE: no distorted or invented Persian/Arabic letterforms, no warped,
duplicated or re-flowed UI, no added text, no hands, no people, no camera shake,
no watermark, no logo changes.
```

---

## ۲. کلاس درس — متن همگام

**عکس:** `docs/video-frames/02-lesson.png`
**حس:** لحظهٔ «آها». تمرکز روی نوار صدا و خطِ روشن متن.

```
Animate the attached image as the first frame of a vertical 9:16 product film.
Keep the composition, the phone position, and every UI element pixel-identical —
do not redraw, restyle, or re-letter the interface.

SCENE: the lesson player screen of the Espresso English app, dark espresso UI,
an English transcript with Persian translation under each paragraph, one
paragraph already highlighted with a vertical amber bar on its right edge.

MOTION: the waveform in the player reacts to the voice, bar by bar, from the
centre outward — a real audio reaction, not a loop. The play button pulses
gently once, on the beat of the first syllable. The amber bar beside the
highlighted paragraph grows 6% taller and its glow brightens as the line is
spoken. The three paragraphs below brighten in sequence, one after another,
as if being read. Nothing scrolls, nothing re-flows.

LIGHT: the screen is the only light source in the lower half of the frame; a
soft amber bloom spills onto the phone bezel. The room stays dark and out of
focus.

CAMERA: locked off, with a single 2% drift toward the transcript. The UI stays
tack-sharp.

AUDIO: A warm, calm male voice speaks Persian with a neutral Iranian accent,
close-mic, slightly slower than conversational: "هر جمله را با صدا همگام ببین، روی هر واژه بزن، و ترجمهٔ فارسی را همان‌جا زیرش بخوان."

SOUND: faint room tone. No music.

NEGATIVE: no distorted or invented Persian/Arabic letterforms, no warped,
duplicated or re-flowed UI, no added text, no hands, no people, no scrolling,
no camera shake, no watermark.
```

---

## ۳. کتابخانه — ۲۰۸ درس

**عکس:** `docs/video-frames/03-library.png`
**حس:** تم روشن، کاغذ کرم، نور روز. تنوع رنگی کارت‌ها را نشان می‌دهد.

```
Animate the attached image as the first frame of a vertical 9:16 product film.
Keep the composition, the phone position, and every UI element pixel-identical —
do not redraw, restyle, or re-letter the interface.

SCENE: the library screen of the Espresso English app in its light theme —
cream paper background, a two-column grid of six lesson cards in blue, violet,
terracotta, green and amber, filter chips above them, a search field at the top.

MOTION: the card grid drifts upward by about 12 pixels over the full 8 seconds,
slowly enough to read, and the cards nearest the camera move slightly more than
the ones behind (subtle parallax). The coloured headers of the cards catch a
single soft specular highlight that travels down the grid. The four filter chips
lift 1 pixel and settle, one after another, right to left. The thin progress
bars at the bottom of each card fill a few percent further.

LIGHT: soft window daylight from the upper right, 5200K, with a gentle gradient
falling across the paper. No harsh shadows, no colour cast on the UI.

CAMERA: very slow push-in, 3%, locked horizon, no shake. The UI stays
tack-sharp.

AUDIO: A warm, calm male voice speaks Persian with a neutral Iranian accent,
close-mic: "دویست و هشت درس، دسته‌بندی‌شده از مبتدی تا پیشرفته. هر جا هستی، حتی بدون اینترنت."

SOUND: quiet room tone, a hint of paper. No music.

NEGATIVE: no distorted or invented Persian/Arabic letterforms, no warped,
duplicated or re-flowed UI, no added text, no hands, no people, no fast
scrolling, no camera shake, no watermark.
```

---

## ۴. آزمون پایان درس — سنجش

**عکس:** `docs/video-frames/04-quiz.png`
**حس:** بستن حلقهٔ یادگیری. سبزِ تأیید، آرام و مطمئن.

```
Animate the attached image as the first frame of a vertical 9:16 product film.
Keep the composition, the phone position, and every UI element pixel-identical —
do not redraw, restyle, or re-letter the interface.

SCENE: the quiz screen of the Espresso English app in its light theme — a
Persian question at the top, five progress dots (three filled), four answer
cards, the first one already correct with a green tick, and a wide terracotta
confirm button at the bottom.

MOTION: the green tick draws itself in, stroke by stroke, then a soft green
halo blooms once around the correct card. That card lifts 3 pixels and settles.
The fifth progress dot fills with a small pop. The confirm button receives a
single tap ripple from the centre, without any finger appearing in frame. The
other three cards dim by 4% as the correct one brightens.

LIGHT: soft daylight, 5000K, cream paper, one gentle highlight along the top
edge of the correct card. No colour cast on the UI.

CAMERA: locked off, no movement at all — this shot is about the confirmation,
not the camera.

AUDIO: A warm, calm male voice speaks Persian with a neutral Iranian accent,
close-mic, a touch of satisfaction in the tone: "آخر هر درس، یک آزمون کوتاه — تا مطمئن شوی یاد گرفته‌ای."

SOUND: one soft, satisfying UI click when the tick completes. No music.

NEGATIVE: no distorted or invented Persian/Arabic letterforms, no warped,
duplicated or re-flowed UI, no added text, no hands, no people, no camera shake,
no watermark.
```

---

## بعد از ساخت: مونتاژ ۳۰ ثانیه‌ای

اگر می‌خواهی چهار کلیپ را به یک ویدیوی معرفی ۳۰ ثانیه‌ای برای اینستاگرام یا مایکت
بچسبانی:

1. ترتیب: **۱ → ۳ → ۲ → ۴** (برند، تنوع، لحظهٔ «آها»، نتیجه)
2. هر کلیپ ۸ ثانیه است؛ از هرکدام ۶ ثانیه بردار تا ۲۴ ثانیه شود
3. بین کلیپ‌ها یک برش سخت بگذار، نه محو تدریجی — با این پالت تیره/روشن، برش سخت
   تمیزتر می‌نشیند
4. صدا: چهار جملهٔ بالا را جداگانه ضبط کن (یا همان صدای Veo را نگه دار) و زیرش یک
   موسیقی بدون کلام و آرام بگذار، حدود ۲۰٪ ولوم
5. پایان: یک کارت ثابت با لوگو و آدرس سایت، ۳ ثانیه
