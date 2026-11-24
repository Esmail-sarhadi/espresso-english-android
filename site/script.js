/* ==========================================================================
   Espresso English — site behaviour
   Vanilla JS, no dependencies, no network calls.
   Everything degrades gracefully: with JS off the page is fully readable.
   ========================================================================== */
(function () {
  "use strict";

  var doc = document;
  var root = doc.documentElement;
  var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

  /* ── Language choice (fa / en) ───────────────────────────────────────── */
  var LANG_KEY = "espresso-lang";
  var isEN = root.getAttribute("lang") === "en";

  function rememberLang(lang) {
    try {
      localStorage.setItem(LANG_KEY, lang);
    } catch (err) {
      /* private mode or storage disabled — the link still navigates */
    }
  }

  // any language link carries hreflang; remember the one the visitor picked
  doc.addEventListener("click", function (e) {
    var link = e.target.closest("a[hreflang]");
    if (link) rememberLang(link.getAttribute("hreflang"));
  });

  // the Persian page sends a visitor who chose English back to en.html once.
  // ?lang=fa (or any ?lang=) means "I meant to be here" — never bounce it.
  var isHome = /(^|\/)(|index\.html)$/.test(window.location.pathname);
  if (!isEN && isHome) {
    var search = window.location.search;
    if (/[?&]lang=fa\b/.test(search)) rememberLang("fa");

    var savedLang = null;
    try {
      savedLang = localStorage.getItem(LANG_KEY);
    } catch (err) {
      savedLang = null;
    }
    if (savedLang === "en" && !/[?&]lang=/.test(search)) {
      window.location.replace("en.html" + window.location.hash);
      return;
    }
  }

  /* ── Navbar: shadow after scroll ─────────────────────────────────────── */
  var nav = doc.getElementById("nav");

  function onScroll() {
    var y = window.scrollY || window.pageYOffset;
    if (nav) nav.classList.toggle("is-scrolled", y > 8);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ── Mobile menu ─────────────────────────────────────────────────────── */
  var toggle = doc.getElementById("nav-toggle");
  var links = doc.getElementById("nav-links");

  var scrim = doc.getElementById("nav-scrim");

  function setMenu(open) {
    if (!nav || !toggle) return;
    nav.classList.toggle("is-open", open);
    doc.body.classList.toggle("menu-open", open && window.innerWidth <= 900);
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open
      ? (isEN ? "Close menu" : "بستن منو")
      : (isEN ? "Open menu" : "باز کردن منو"));
  }

  if (toggle) {
    toggle.addEventListener("click", function () {
      setMenu(toggle.getAttribute("aria-expanded") !== "true");
    });
  }

  if (links) {
    links.addEventListener("click", function (e) {
      if (e.target.closest("a")) setMenu(false);
    });
  }

  doc.addEventListener("click", function (e) {
    if (!nav || !nav.classList.contains("is-open")) return;
    if (!nav.contains(e.target)) setMenu(false);
  });

  if (scrim) scrim.addEventListener("click", function () { setMenu(false); });

  window.addEventListener("resize", function () {
    if (window.innerWidth > 900) setMenu(false);
  });

  /* ── Smooth scrolling for in-page links ──────────────────────────────── */
  function scrollToTarget(target) {
    if (!target) return;
    var behavior = reduceMotion.matches ? "auto" : "smooth";
    target.scrollIntoView({ behavior: behavior, block: "start" });

    // move focus for screen readers / keyboard users without stealing the scroll
    if (!target.hasAttribute("tabindex")) target.setAttribute("tabindex", "-1");
    try {
      target.focus({ preventScroll: true });
    } catch (err) {
      target.focus();
    }
  }

  doc.addEventListener("click", function (e) {
    var link = e.target.closest('a[href^="#"]');
    if (!link) return;

    var hash = link.getAttribute("href");
    if (!hash || hash === "#" || hash.length < 2) return;

    var target = doc.getElementById(hash.slice(1));
    if (!target) return;

    e.preventDefault();
    setMenu(false);
    scrollToTarget(target);

    if (history.replaceState) history.replaceState(null, "", hash);
  });

  /* ── Scroll spy: highlight the current nav link ──────────────────────── */
  var navAnchors = links ? Array.prototype.slice.call(links.querySelectorAll('a[href^="#"]:not(.nav__mobile-cta)')) : [];
  var spySections = navAnchors
    .map(function (a) { return doc.getElementById(a.getAttribute("href").slice(1)); })
    .filter(Boolean);

  if ("IntersectionObserver" in window && spySections.length) {
    var spy = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          navAnchors.forEach(function (a) {
            var active = a.getAttribute("href") === "#" + entry.target.id;
            a.classList.toggle("is-current", active);
            if (active) a.setAttribute("aria-current", "location");
            else a.removeAttribute("aria-current");
          });
        });
      },
      { rootMargin: "-45% 0px -50% 0px", threshold: 0 }
    );
    spySections.forEach(function (s) { spy.observe(s); });
  }

  /* ── Reveal on scroll ────────────────────────────────────────────────── */
  var revealables = doc.querySelectorAll(".reveal");

  if (!("IntersectionObserver" in window) || reduceMotion.matches) {
    // no observer or motion is unwanted: show everything immediately
    Array.prototype.forEach.call(revealables, function (el) { el.classList.add("is-in"); });
  } else {
    var revealObserver = new IntersectionObserver(
      function (entries, obs) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-in");
          obs.unobserve(entry.target);
        });
      },
      { rootMargin: "0px 0px -6% 0px", threshold: 0.12 }
    );
    Array.prototype.forEach.call(revealables, function (el) { revealObserver.observe(el); });
  }

  /* ── Hero lyrics: gentle playback simulation ─────────────────────────── */
  var lyricList = doc.getElementById("hero-lyrics");

  if (lyricList && !reduceMotion.matches) {
    var lines = lyricList.querySelectorAll(".lyric");
    var index = 1;
    var visible = true;
    var timer = null;

    var advance = function () {
      if (!visible || doc.hidden) return;
      lines[index].classList.remove("is-active");
      index = (index + 1) % lines.length;
      lines[index].classList.add("is-active");
    };

    timer = window.setInterval(advance, 3600);

    // pause while the phone is off screen to keep the page quiet
    if ("IntersectionObserver" in window) {
      new IntersectionObserver(function (entries) {
        visible = entries[0].isIntersecting;
      }, { threshold: 0.2 }).observe(lyricList);
    }

    doc.addEventListener("visibilitychange", function () {
      if (doc.hidden && timer) {
        window.clearInterval(timer);
        timer = null;
      } else if (!doc.hidden && !timer) {
        timer = window.setInterval(advance, 3600);
      }
    });
  }

  /* ── Copy the download link ──────────────────────────────────────────── */
  var APK_FALLBACK = "assets/Espresso-English-5.4.0.apk";

  // feedback wording follows the page language
  var COPY_TEXT = isEN
    ? {
        ok: "Copied ✓",
        fail: "Not copied",
        okMsg: "Download link copied to the clipboard.",
        failMsg: "Automatic copy didn't work — select the link and copy it yourself."
      }
    : {
        ok: "کپی شد ✓",
        fail: "کپی نشد",
        okMsg: "لینک دانلود در کلیپ‌بورد کپی شد.",
        failMsg: "کپی خودکار ممکن نشد؛ لینک را دستی انتخاب کن."
      };

  // read the link from the page so editing the download button is enough
  function apkHref() {
    var link = doc.querySelector('a[download][href$=".apk"], a[href$=".apk"]');
    return (link && link.getAttribute("href")) || APK_FALLBACK;
  }

  function apkUrl() {
    try {
      return new URL(apkHref(), window.location.href).href;
    } catch (err) {
      return apkHref();
    }
  }

  function legacyCopy(text) {
    var area = doc.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.style.position = "fixed";
    area.style.top = "-1000px";
    area.style.opacity = "0";
    doc.body.appendChild(area);
    area.select();
    var ok = false;
    try {
      ok = doc.execCommand("copy");
    } catch (err) {
      ok = false;
    }
    doc.body.removeChild(area);
    return ok;
  }

  function announce(message) {
    var status = doc.querySelector("[data-copy-status]");
    if (status) status.textContent = message;
  }

  // swap only the label node, so icons inside the button survive
  function flashButton(button, label) {
    var target = button.querySelector("[data-label-text]") || button;
    if (!target.dataset.original) target.dataset.original = target.textContent.trim();
    target.textContent = label;
    window.setTimeout(function () {
      target.textContent = target.dataset.original;
    }, 2200);
  }

  doc.addEventListener("click", function (e) {
    var button = e.target.closest("[data-copy-link]");
    if (!button) return;

    var url = apkUrl();

    var done = function (ok) {
      flashButton(button, ok ? COPY_TEXT.ok : COPY_TEXT.fail);
      announce(ok ? COPY_TEXT.okMsg : COPY_TEXT.failMsg);
    };

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(
        function () { done(true); },
        function () { done(legacyCopy(url)); }
      );
    } else {
      done(legacyCopy(url));
    }
  });

  /* ── Install-help modal ──────────────────────────────────────────────── */
  var modal = doc.getElementById("install-modal");
  var modalPanel = modal ? modal.querySelector(".modal__panel") : null;
  var modalOpeners = doc.querySelectorAll("[data-open-modal]");
  var lastFocused = null;

  function focusables() {
    if (!modalPanel) return [];
    return Array.prototype.filter.call(
      modalPanel.querySelectorAll('a[href], button:not([disabled]), input, [tabindex]:not([tabindex="-1"])'),
      function (el) { return el.offsetParent !== null; }
    );
  }

  function openModal() {
    if (!modal) return;
    lastFocused = doc.activeElement;
    modal.hidden = false;
    doc.body.style.overflow = "hidden";

    var field = doc.getElementById("apk-link");
    if (field) field.value = apkHref();

    // focus the panel itself so screen readers announce the dialog first
    if (modalPanel) modalPanel.focus();
  }

  function closeModal() {
    if (!modal || modal.hidden) return;
    modal.hidden = true;
    doc.body.style.overflow = "";
    announce("");
    if (lastFocused && typeof lastFocused.focus === "function") lastFocused.focus();
  }

  Array.prototype.forEach.call(modalOpeners, function (btn) {
    btn.addEventListener("click", openModal);
  });

  if (modal) {
    modal.addEventListener("click", function (e) {
      if (e.target.closest("[data-close]")) closeModal();
    });

    modal.addEventListener("keydown", function (e) {
      if (e.key === "Escape") {
        e.preventDefault();
        closeModal();
        return;
      }
      if (e.key !== "Tab") return;

      var items = focusables();
      if (!items.length) return;
      var first = items[0];
      var last = items[items.length - 1];

      if (e.shiftKey && doc.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && doc.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    });
  }

  doc.addEventListener("keydown", function (e) {
    if (e.key !== "Escape") return;
    if (modal && !modal.hidden) return; // handled by the modal itself
    setMenu(false);
  });

  /* ── Screenshot strip: arrow-key paging ──────────────────────────────── */
  var scroller = doc.querySelector(".strip__scroller");
  if (scroller) {
    scroller.addEventListener("keydown", function (e) {
      if (e.key !== "ArrowLeft" && e.key !== "ArrowRight") return;
      // RTL: scrollLeft is 0 at the start and grows negative towards the end
      var dir = e.key === "ArrowLeft" ? -1 : 1;
      e.preventDefault();
      scroller.scrollBy({ left: dir * 240, behavior: reduceMotion.matches ? "auto" : "smooth" });
    });
  }
})();

/* ═══════════════════════════════════════════════════════════════════════════
   Editorial motion layer — inlined
   ═══════════════════════════════════════════════════════════════════════════ */
/* Editorial motion layer — enhancement only, never required for the page. */
(function () {
  "use strict";

  var doc = document;
  var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function el(tag, cls, parent) {
    var node = doc.createElement(tag);
    if (cls) node.className = cls;
    (parent || doc.body).appendChild(node);
    return node;
  }

  /* ---------------------------------------------------------- injections */
  var progress = el("div", "fx-progress");
  progress.setAttribute("aria-hidden", "true");

  var hero = doc.querySelector(".hero");
  if (hero && hero.querySelector(".hero__device")) {
    var device = hero.querySelector(".hero__device");
    var marks = el("div", "fx-rings", device);
    marks.setAttribute("aria-hidden", "true");
  }

  var why = doc.getElementById("why");
  if (why && why.nextElementSibling) {
    var ticker = el("div", "fx-ticker");
    ticker.setAttribute("aria-hidden", "true");
    var isEn = doc.documentElement.getAttribute("lang") === "en";
    var phrases = isEn
      ? ["208 REAL LESSONS", "100% OFFLINE", "NO ADS", "SYNCED TRANSCRIPTS", "PERSIAN TRANSLATION", "FLOATING BUBBLE", "FREE FOREVER", "NO SIGN-UP"]
      : ["۲۰۸ درس واقعی", "کاملاً آفلاین", "بدون تبلیغ", "متن همگام", "ترجمهٔ فارسی", "حباب شناور", "رایگان", "بدون ثبتنام"];
    var line = phrases.map(function (p) {
      return "<span>" + p + "<b>✦</b></span>";
    }).join("");
    ticker.innerHTML = '<div class="fx-ticker__track">' + line + line + "</div>";
    why.parentNode.insertBefore(ticker, why.nextElementSibling);
  }

  /* ------------------------------------------------------ reveal stagger */
  var groups = doc.querySelectorAll(".stats, .cards, .gal-grid");
  Array.prototype.forEach.call(groups, function (group) {
    Array.prototype.forEach.call(group.children, function (child, i) {
      child.style.setProperty("--reveal-delay", (i * 90) + "ms");
    });
  });

  /* ------------------------------------------------- pointer-tracked glow */
  var glowCards = doc.querySelectorAll(".card, .step");
  if (!reduce && glowCards.length && window.matchMedia("(hover: hover)").matches) {
    Array.prototype.forEach.call(glowCards, function (card) {
      var frame = 0;
      card.addEventListener("pointermove", function (e) {
        if (frame) return;
        frame = window.requestAnimationFrame(function () {
          frame = 0;
          var r = card.getBoundingClientRect();
          if (!r.width || !r.height) return;
          card.style.setProperty("--mx", (((e.clientX - r.left) / r.width) * 100).toFixed(1) + "%");
          card.style.setProperty("--my", (((e.clientY - r.top) / r.height) * 100).toFixed(1) + "%");
        });
      });
    });
  }

  /* --------------------------------------------------------- progress bar */
  function onScroll() {
    var max = doc.documentElement.scrollHeight - window.innerHeight;
    var p = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
    progress.style.width = (p * 100).toFixed(2) + "%";
  }
  if (!reduce) {
    var ticking = false;
    window.addEventListener("scroll", function () {
      if (!ticking) { window.requestAnimationFrame(function () { onScroll(); ticking = false; }); ticking = true; }
    }, { passive: true });
    onScroll();
  }

  /* ------------------------------------------------------------- counters */
  var western = "0123456789";
  var persian = "۰۱۲۳۴۵۶۷۸۹";
  function toFa(n) { return String(n).replace(/[0-9]/g, function (d) { return persian[western.indexOf(d)]; }); }
  function toEn(s) { return String(s).replace(/[۰-۹]/g, function (d) { return western[persian.indexOf(d)]; }); }

  var counters = doc.querySelectorAll(".stat__num");
  if (counters.length && !reduce && "IntersectionObserver" in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        var node = entry.target;
        var raw = toEn(node.textContent.trim());
        var match = raw.match(/[0-9]+/);
        if (!match) { io.unobserve(node); return; }
        var target = parseInt(match[0], 10);
        if (!isFinite(target) || target < 2) { io.unobserve(node); return; }
        var suffix = raw.slice(match.index + match[0].length);
        var isFa = /[۰-۹]/.test(node.textContent);
        var start = performance.now();
        var duration = 1200;
        function tick(now) {
          var t = Math.min(1, (now - start) / duration);
          var eased = 1 - Math.pow(1 - t, 3);
          var value = String(Math.round(target * eased));
          node.textContent = (isFa ? toFa(value) : value) + suffix;
          if (t < 1) window.requestAnimationFrame(tick);
        }
        window.requestAnimationFrame(tick);
        io.unobserve(node);
      });
    }, { threshold: 0.5 });
    Array.prototype.forEach.call(counters, function (c) { io.observe(c); });
  }
})();
