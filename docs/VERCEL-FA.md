<div dir="rtl" align="right">

# وصل کردن سایت به Vercel

سایت آماده است: `site/index.html` + `site/web/` (نسخهٔ وب) + `site/privacy.html`.
برای Vercel هیچ بیلدی لازم نیست — یک پوشهٔ استاتیک است.

## ⚠️ اول این را بخوان

Vercel شرکت آمریکایی است و **به IP ایران سرویس نمی‌دهد** (تحریم). یعنی:

- باز کردن پنل Vercel از ایران معمولاً کار نمی‌کند و به VPN نیاز دارد.
- استفاده از حساب با IP ایران ممکن است به بستن پروژه منجر شود.

**اگر هدف فقط «سایت بالا باشد»، همین حالا روی GitHub Pages بالاست و رایگان است:**
`https://esmail-sarhadi.github.io/espresso-english-android/`

اگر Vercel را می‌خواهی (سرعت بهتر در بعضی نقاط، دامنهٔ اختصاصی ساده‌تر)، ادامه بده.

## روش ۱ — از پنل (۲ دقیقه، بدون کد)

1. وارد <https://vercel.com/new> شو.
2. مخزن `Esmail-sarhadi/espresso-english-android` را **Import** کن.
3. در بخش **Root Directory** روی `Edit` بزن و پوشهٔ **`site`** را انتخاب کن.
4. Framework Preset: **Other**. Build Command: خالی. Output Directory: خالی.
5. **Deploy**.

تمام. آدرس می‌شود چیزی مثل `espresso-english-android.vercel.app`.

> اگر بعداً پوشهٔ سایت را عوض کردی، Vercel خودش با هر push دوباره دیپلوی می‌کند.

## روش ۲ — خودکار از گیت‌هاب (Workflow آماده است)

فایل `.github/workflows/vercel.yml` از قبل نوشته شده. فقط سه Secret اضافه کن:

1. توکن: <https://vercel.com/account/tokens> → Create Token
2. در ترمینال (یک‌بار):
   ```bash
   npm i -g vercel
   cd site
   vercel link          # پروژه را می‌سازد و به حساب وصل می‌کند
   cat .vercel/project.json
   ```
3. در گیت‌هاب → Settings → Secrets and variables → Actions:

| نام | مقدار |
|---|---|
| `VERCEL_TOKEN` | توکن مرحلهٔ ۱ |
| `VERCEL_ORG_ID` | `orgId` از `project.json` |
| `VERCEL_PROJECT_ID` | `projectId` از `project.json` |

از آن به بعد، هر push روی `site/` خودش دیپلوی می‌شود.

## تنظیمات داخل پروژه

`site/vercel.json` این‌ها را تنظیم می‌کند:

- کش یک‌ساله برای `/assets/*` و `/web/assets/*` (فایل‌ها هش‌دارند)
- `no-cache` برای `sw.js` تا سرویس‌ورکر آپدیت شود
- `Content-Type` درست برای `manifest.webmanifest`
- `cleanUrls` تا آدرس‌ها بدون `.html` کار کنند

## بعد از دیپلوی

این سه خط را عوض کن تا همه‌جا به Vercel اشاره کند:

| فایل | خط |
|---|---|
| `README.md` | لینک‌های Website / Try in browser |
| `site/index.html` | اگر دامنهٔ اختصاصی داری، تگ `canonical` |
| `marketing/*.md` | لینک بایو |

## گزینه‌های ایرانی (اگر Vercel جواب نداد)

| سرویس | مناسب برای |
|---|---|
| **GitHub Pages** | همین حالا فعال، رایگان، بدون تحریم |
| **Cloudflare Pages** | سریع، رایگان، ولی مثل Vercel تحریم دارد |
| **Liara** (`liara.ir`) | ایرانی، استقرار استاتیک ساده، پرداخت ریالی |
| **ArvanCloud** | ایرانی، CDN + استاتیک، دامنهٔ `.ir` |

</div>
