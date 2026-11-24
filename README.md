# Espresso English · اسپرسو انگلیش

<p align="center">
  <b>اپلیکیشن اندروید یادگیری زبان انگلیسی برای فارسی‌زبانان — آفلاین، رایگان، بدون تبلیغ</b><br>
  <sub>An offline-first English learning app for Persian speakers · Android 7.0+ · Kotlin + Jetpack Compose</sub>
</p>

<p align="center">
  <img alt="version" src="https://img.shields.io/badge/version-5.4.0-FFB03A">
  <img alt="android" src="https://img.shields.io/badge/Android-7.0%2B-4FE0BE">
  <img alt="size" src="https://img.shields.io/badge/APK-131%20MB-B49BFF">
  <img alt="license" src="https://img.shields.io/badge/license-MIT-6FB8FF">
  <img alt="price" src="https://img.shields.io/badge/price-free-FF8A6B">
</p>

---

<div dir="rtl" align="right">

## این برنامه چیست؟

**Espresso English** یک اپلیکیشن اندروید برای یادگیری انگلیسی با گوش دادن است: ۲۰۸ درس با متن همگام، ترجمهٔ فارسی خط‌به‌خط، واژه‌نامه، آزمون، مرور فاصله‌دار و تمرین تلفظ — همه در حافظهٔ گوشی و بدون نیاز به اینترنت.

دو ابزار کمتر دیده‌شده هم دارد:

- **ترانهٔ همگام** — آهنگی که در گوشی پخش می‌شود را تشخیص می‌دهد و متن آن را همراه با موسیقی جلو می‌برد؛ هر خط ترجمهٔ فارسی دارد.
- **حباب ترجمه** — یک حباب کوچک روی هر برنامه‌ای شناور می‌ماند؛ با زدنش متن انگلیسی همان صفحه خوانده و ترجمه می‌شود، بدون اینکه از برنامه‌ای که در آن هستی بیرون بیایی.

## امکانات

| | |
|---|---|
| 🎧 **۲۰۸ درس صوتی** | متن همگام، ترجمهٔ فارسی، واژه‌نامه و آزمون برای هر درس |
| 📴 **کاملاً آفلاین** | صدای عصبی (Piper) داخل برنامه — بدون اینترنت هم کار می‌کند |
| 🎵 **ترانهٔ همگام** | تشخیص آهنگ در حال پخش + ترجمهٔ خط‌به‌خط + قاب RGB متحرک |
| 🫧 **حباب شناور** | ترجمهٔ متن هر برنامه با یک ضربه، روی همهٔ برنامه‌ها |
| 🔁 **مرور فاصله‌دار** | واژه‌ها در فاصله‌های بهینه برمی‌گردند (الگوریتم SM-2) |
| 🎤 **تمرین تلفظ** | ضبط صدا و نمرهٔ دقت |
| 📊 **آمار روزانه** | استریک، دقیقهٔ گوش دادن، واژهٔ ذخیره‌شده |
| 🌙 **تم روشن/تاریک** | رابط فارسی راست‌به‌چپ، بدون ثبت‌نام و بدون ردیابی |

## دانلود

| منبع | لینک |
|---|---|
| **GitHub Releases** (همیشه آخرین نسخه) | [Releases](../../releases/latest) |
| کافه‌بازار | _به‌زودی_ |
| مایکت | _به‌زودی_ |
| نسخهٔ وب (دمو در مرورگر) | [site/index.html](site/index.html) |

> **کدام APK؟** برای گوشی واقعی نسخهٔ `-ARM` (۱۳۱ مگابایت) را بگیر. نسخهٔ کامل (۲۳۲ مگابایت) برای امولاتور و دستگاه‌های x86 است.

## نصب

1. فایل APK را دانلود کن.
2. اگر اندروید هشدار داد، «نصب از منابع نامشخص» را برای مرورگر یا فایل‌منیجرت روشن کن.
3. نصب کن و باز کن — اولین اجرا یک تور چهارصفحه‌ای فارسی نشان می‌دهد.

</div>

---

## English

**Espresso English** is an offline-first Android app that teaches English through listening, built for Persian speakers. 208 lessons with synchronised transcripts, line-by-line Persian translation, vocabulary, quizzes, spaced repetition and pronunciation scoring — all bundled, no account, no ads, no telemetry.

Two features worth calling out:

- **Live lyric sync** — reads the media session of whatever is playing, scrolls the lyrics in time with the music and translates every line to Persian.
- **Floating translate bubble** — a draggable bubble that stays on top of other apps. Tap it and the on-screen English text is OCR'd on-device and translated in an overlay panel.

### Tech stack

- Kotlin 2.2 · Jetpack Compose (Material 3) · Navigation Compose
- Room (KSP) for lessons, vocabulary, SRS state and stats
- Media3 / ExoPlayer + MediaSessionService for playback and lock-screen controls
- Piper neural TTS bundled on-device, with the system TTS engine as a fallback
- ML Kit text recognition (Latin) for the screen translator
- `MediaProjection` foreground services for one-shot capture and the floating bubble
- Keyless lyrics catalogues: LRCLIB, iTunes Search, Deezer, lyrics.ovh
- Translation: Google `gtx` endpoint with MyMemory as a fallback

### Build

```bash
cd android-app
./gradlew :app:assembleRelease -ParmOnly   # 131 MB, real phones
./gradlew :app:assembleRelease             # 232 MB, universal
./gradlew :app:testDebugUnitTest           # pure-JVM tests for the lyric/sync maths
```

You need JDK 21 (Android Studio's JBR works) and the Android SDK 36.

### Project layout

```
android-app/          Android application (Kotlin)
site/                 Marketing website (static, GitHub Pages ready)
marketing/            Launch content: LinkedIn, Instagram, YouTube, 30-day calendar, SEO
docs/PUBLISHING-FA.md Step-by-step guide to Bazaar / Myket / Google Play / GitHub
```

---

## Privacy

No account, no analytics SDK, no advertising. Progress, vocabulary and settings live in a local Room database; the only network calls are the ones you trigger explicitly (lyrics lookup and translation). See [PRIVACY.md](PRIVACY.md).

## Content attribution

The lesson audio and transcripts are derived from the **Espresso English podcast by Shayna Oliveira** and remain the property of their author. This repository publishes the *application*, not a licence to redistribute that content. If you are the rights holder and want the bundled lessons removed, open an issue and it will be handled immediately.

## License

The source code is released under the [MIT License](LICENSE). That covers the code only — it does not extend to the third-party lesson content described above.
