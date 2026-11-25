# Espresso English — marketing site

The public landing page for **Espresso English 5.4.0**, an Android app that teaches English to
Persian speakers (208 podcast lessons with synced transcripts and Persian translation, spaced
repetition, pronunciation scoring, synced song lyrics and a floating translate bubble).

Static site: **no build step, no npm, no external requests**. Open `index.html` and it works —
online or offline, from disk or from a server.

---

## Files

```
site/
├── index.html                 # Persian landing page (semantic HTML5, RTL, JSON-LD)
├── en.html                    # English landing page
├── gallery.html               # media page — video + screenshot gallery
├── gallery.js                 # lightbox for the gallery
├── permissions.html           # MIUI/Xiaomi permission walkthrough (drawn screens)
├── privacy.html               # privacy policy (fa + en, no scripts)
├── styles.css                 # all styling, design tokens at the top
├── script.js                  # menu, smooth scroll, reveal, copy, modal, pattern layer
├── README.md
└── assets/
    ├── fonts.css              # Vazirmatn variable font embedded as base64
    ├── Vazirmatn-var.woff2
    ├── hero-demo.gif          # rendered hero screen (24 frames, 4.4 s loop, ~0.9 MB)
    ├── hero-poster.webp       # static frame used for reduced-motion visitors
    └── og.png                 # 1200×630 social preview image
```

There is no `package.json`, no bundler, no framework and no third-party script. The only
network-capable things in the markup are the placeholder footer links (`href="#"`).

---

## Design system — "Roastery"

The app is called Espresso English, so the site is built from the coffee it is named after
rather than from a generic SaaS palette.

**Canvas and structure.** Warm ivory paper (`--bg #FAF4EA`), sand bands (`--bg-soft #F3E8D6`)
for alternating chapters, and deep espresso (`--espresso #17100A`) for the masthead, hero,
final CTA and footer. That gives every page a dark → light → dark rhythm, and the sticky
masthead never has to change state while scrolling.

**One accent.** A single crema ramp — `#8A4A08 → #DFA046 → #F4CE92` — carries every
highlight: eyebrows, numerals, icon glyphs, focus rings, check marks. Nothing else on the
page is saturated, which is why the accent reads as deliberate instead of decorative.

**Type.** Vazirmatn variable, 900 for display, `-.03em` tracking on headings, Persian
numerals throughout, `font-variant-numeric: tabular-nums` on every figure.

**Texture.** `styles.css` is organised in numbered sections; the two worth knowing:

- **§14 Pattern + motion.** A fixed paper lattice (`html::after`, two diamond scales at
  ~30 % and ~22 % warm brown) that reads as texture on ivory and disappears on the dark
  bands; a 5 % film grain; the hero's own crema diamond mesh and slow orbit arc; the
  marquee strip injected after `#why`; the scroll-progress hairline; card and button
  micro-interactions. Everything is `transform`/`opacity` only and the whole layer is
  switched off under `prefers-reduced-motion`, where the hero GIF is swapped for
  `hero-poster.webp`.
- **§16 Permissions page.** `.ms*` draws a believable MIUI settings screen in pure CSS
  (rows, icon tiles, toggles, green checks) so the walkthrough needs no screenshots and
  no personal data in the repo.

Contrast: body on ivory 9.8:1, headings 14:1, cream on espresso 15:1, crema on espresso
7.4:1 — all above the 4.5:1 / 7:1 targets.

The hero phone screen is a **GIF**, not a video or a CSS animation: it keeps the
page a single static bundle with no autoplay policy, no codec support questions
and no JavaScript needed for the first impression.

---

## Preview locally

Just double-click `index.html`, or serve the folder:

```bash
# Python 3
python -m http.server 8000 --directory site
# → http://localhost:8000

# Node
npx serve site
```

Opening the file directly works too — that is a hard requirement of this build and it is why the
fonts are embedded (see below).

---

## Deploy to GitHub Pages

1. Push this repository to GitHub.
2. **Settings → Pages → Build and deployment**
   - **Source:** *Deploy from a branch*
   - **Branch:** `main`
   - **Folder:** `/site` if these files live in a `site/` subfolder of the repo, or `/ (root)` if
     you made `site/` the repository root.
3. Save. Pages publishes at `https://<user>.github.io/<repo>/` in a minute or two.

Notes:

- **Relative paths only.** `styles.css`, `script.js` and `assets/…` are all referenced relatively,
  so the site works from a project subpath (`/<repo>/`) as well as from a user/organisation root
  (`https://<user>.github.io/`). Do not switch them to `/styles.css`.
- **`index.html` must sit at the publish root**, next to `styles.css` and `script.js`.
- `.nojekyll` is not required (no underscore-prefixed files are used), but adding an empty
  `.nojekyll` file at the publish root does no harm and speeds up deployment.
- A custom 404 page is optional; the site is a single page.

### Custom domain

1. Create a file named `CNAME` at the **publish root** containing only the domain, e.g.
   `espresso-english.ir` (no protocol, no trailing slash).
2. At your DNS provider:
   - apex domain → `A` records to `185.199.108.153`, `185.199.109.153`,
     `185.199.110.153`, `185.199.111.153` (and the matching `AAAA` records if you want IPv6), or
   - `www` subdomain → `CNAME` to `<user>.github.io`.
3. In **Settings → Pages** set the custom domain, wait for the DNS check, then enable
   **Enforce HTTPS**.
4. Update the absolute URLs that are baked into the HTML — `canonical`, `og:url`, `og:image`
   and `twitter:image` — to the new domain.

---

## The APK download

The download button in the final CTA points at:

```html
<a href="assets/Espresso-English-5.4.0.apk" download>…</a>
```

Two ways to make it real:

- **GitHub Releases (recommended).** GitHub rejects files larger than 100 MB, and the APK is
  ~242 MB (137 MB for the ARM build), so it cannot live in the repository. Create a release, attach
  the APK, then change the `href` to the release asset URL, e.g.
  `https://github.com/<user>/<repo>/releases/download/v5.4.0/Espresso-English-5.4.0.apk`.
- **Any static host / CDN.** Drop the file into `assets/` if your host allows files that size.

`script.js` reads the APK URL from that link, so the **«کپی لینک»** button and the install-help
modal always copy whatever the button points at — no second place to edit.

The install-help modal (opened from «راهنمای نصب») explains the "install from unknown sources"
flow and exposes the same link with a copy button.

---

## Editing

- **Colours, radii, spacing, type scale** are CSS custom properties in `:root` at the top of
  `styles.css`. The palette is *midnight espresso*: background `#0C0A10`, surface `#191521`,
  outline `#322A3E`, text `#F8F2EC`, muted `#BCB0C8`, accents amber `#FFB03A`, coral `#FF8A6B`,
  lilac `#B49BFF`, sky `#6FB8FF`, mint `#4FE0BE`.
- **The phone mockups are pure CSS/HTML** — a bezel, a notch, a status bar and a stylised app UI.
  Their inner typography is sized in `cqw` (container query units), so a mockup scales perfectly
  whether it is the 330 px hero or a 216 px thumbnail. Edit the markup inside `.phone__screen`.
- **Copy** is plain Persian text in `index.html`; numbers use Persian digits (۰–۹).
- **Version numbers** appear in the `<title>`, the JSON-LD `softwareVersion`, the hero eyebrow,
  the CTA button label, the modal step 1, the footer and the APK filename. Search for `5.4.0`
  and `۵٫۴٫۰` when releasing.

### Typography

The site uses **Vazirmatn** (SIL Open Font License 1.1). The woff2 files are in `assets/`, but the
`@font-face` rules in `assets/fonts.css` embed the same files as base64 data URIs on purpose:
browsers block font requests made from a `file://` page (CORS), and this page must look identical
when opened straight from disk. If you only ever serve the site over HTTP, you can swap the data
URIs for `url("Vazirmatn-Regular.woff2")` and save ~130 KB of CSS.

If you prefer not to ship the font at all, delete `assets/fonts.css` and the `<link>` to it — the
stack falls back to `"Segoe UI", Tahoma, sans-serif`.

### Social preview image

`assets/og.png` is a 1200×630 render of the hero. Re-shoot it after big copy changes (any
1200×630 screenshot of the top of the page works), and keep `og:image` / `twitter:image` in sync
with the deployed absolute URL.

---

## Accessibility & behaviour notes

- `<html lang="fa" dir="rtl">`, one `<h1>`, ordered `<h2>`/`<h3>`.
- Skip link, visible `:focus-visible` rings, `aria-expanded` on the menu button, `aria-current`
  on the active nav link, focus is trapped in the install modal and restored on close (Esc closes).
- Phone mockups are `aria-hidden` decoration; every screenshot has a visible caption.
- `prefers-reduced-motion: reduce` disables the equalizer rotation, floating chips, reveal-on-scroll
  and smooth scrolling.
- Without JavaScript the page is fully readable: reveal animations only apply when a `js` class is
  present on `<html>`.

## Browser support

Evergreen Chrome, Edge, Firefox and Safari (2023+). The design relies on CSS custom properties,
`aspect-ratio`, `color-mix()`, container query units and `backdrop-filter`; older engines degrade
to a simpler but complete layout.
