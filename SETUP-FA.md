<div dir="rtl" align="right">

# گذاشتن پروژه روی گیت‌هاب (۵ دقیقه)

این پوشه از قبل `git init` شده و اولین کامیت هم ساخته شده. فقط باید مخزن را روی گیت‌هاب بسازی و push کنی.

## روش ۱ — از طریق سایت (ساده‌ترین)

1. برو به <https://github.com/new>
2. **Repository name**: `espresso-english-android`
3. **Public** را انتخاب کن. هیچ‌کدام از گزینه‌های README/gitignore را تیک نزن (ما از قبل داریم).
4. Create repository.
5. در همین پوشه این دو دستور را بزن (آدرس خودت را جایگزین کن):

```bash
git remote add origin https://github.com/USERNAME/espresso-english-android.git
git branch -M main
git push -u origin main
```

وقتی رمز خواست، **رمز گیت‌هاب را نزن**. یک Personal Access Token بساز:

- GitHub → Settings → Developer settings → Personal access tokens → **Tokens (classic)** → Generate new token
- Scope: فقط `repo` را تیک بزن، ۹۰ روز اعتبار
- توکن را به‌جای رمز وارد کن

## روش ۲ — با GitHub CLI (اگر نصبش کنی)

```bash
winget install --id GitHub.cli
gh auth login          # از مرورگر تأیید می‌کنی
gh repo create espresso-english-android --public --source=. --push
```

## بعد از push — سه کار

### ۱) انتشار فایل نصبی (Release)

APK در گیت نمی‌رود (بزرگ‌تر از حد مجاز است)؛ باید به‌عنوان Release آپلود شود:

```bash
gh release create v5.4.0 \
  "../Espresso-English-5.4.0-ARM.apk#Espresso-English-5.4.0-ARM.apk" \
  "../Espresso-English-5.4.0.apk#Espresso-English-5.4.0.apk" \
  --title "Espresso English 5.4.0" \
  --notes-file docs/RELEASE-NOTES-5.4.0.md
```

یا از سایت: Releases → Draft a new release → Tag `v5.4.0` → فایل‌ها را کشیده و رها کن.

### ۲) روشن کردن GitHub Pages (سایت)

Settings → Pages → Source: **Deploy from a branch** → Branch: `main` → Folder: **`/site`** → Save.

آدرس سایت می‌شود: `https://USERNAME.github.io/espresso-english-android/`

> در `site/index.html` لینک دکمهٔ دانلود را به آدرس Release تغییر بده:
> `https://github.com/USERNAME/espresso-english-android/releases/latest`

### ۳) توضیحات مخزن

در صفحهٔ اصلی مخزن، روی چرخ‌دنده کنار About بزن:

- **Description**: `Offline English learning app for Persian speakers — synced lyrics, floating translator, 208 lessons`
- **Topics**: `android` `kotlin` `jetpack-compose` `language-learning` `persian` `farsi` `offline-first` `lyrics` `ocr` `material3`

</div>
