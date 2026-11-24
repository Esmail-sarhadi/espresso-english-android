/* Decorative motion layer — enhancement only, never required for the page to work. */
(function () {
  "use strict";

  var doc = document;
  var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var fine = window.matchMedia && window.matchMedia("(hover: hover) and (pointer: fine)").matches;

  function el(tag, cls, parent) {
    var node = doc.createElement(tag);
    if (cls) node.className = cls;
    (parent || doc.body).appendChild(node);
    return node;
  }

  /* ---------------------------------------------------------- injections */
  el("div", "fx-aurora").innerHTML =
    '<div class="fx-aurora__blob fx-aurora__blob--a"></div>' +
    '<div class="fx-aurora__blob fx-aurora__blob--b"></div>' +
    '<div class="fx-aurora__blob fx-aurora__blob--c"></div>';

  el("div", "fx-grain");
  el("div", "fx-progress");
  var cursor = el("div", "fx-cursor");

  var hero = doc.querySelector(".hero");
  if (hero) {
    var rings = el("div", "fx-rings");
    hero.appendChild(rings);
  }

  var why = doc.getElementById("why");
  if (why && why.nextElementSibling) {
    var ticker = el("div", "fx-ticker");
    var phrases = [
      "۲۰۸ درس واقعی", "کاملاً آفلاین", "بدون تبلیغ", "متن همگام",
      "ترجمهٔ فارسی", "حباب شناور", "رایگان", "بدون ثبتنام"
    ];
    var line = phrases
      .map(function (p) { return "<span>" + p + "<b>✦</b></span>"; })
      .join("");
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

  /* ------------------------------------------------------ scroll progress */
  var bar = doc.querySelector(".fx-progress");
  var device = doc.querySelector(".hero__device");
  var glows = doc.querySelectorAll(".hero__glow");
  var ticking = false;

  function onScroll() {
    var max = doc.documentElement.scrollHeight - window.innerHeight;
    var p = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
    bar.style.width = (p * 100).toFixed(2) + "%";

    if (!reduce && window.scrollY < window.innerHeight * 1.5) {
      var y = window.scrollY;
      if (device) device.style.transform = "translate3d(0," + (-y * 0.045).toFixed(1) + "px,0)";
      Array.prototype.forEach.call(glows, function (g, i) {
        g.style.transform = "translate3d(0," + (y * (i ? 0.05 : 0.09)).toFixed(1) + "px,0)";
      });
    }
    ticking = false;
  }
  window.addEventListener("scroll", function () {
    if (!ticking) { window.requestAnimationFrame(onScroll); ticking = true; }
  }, { passive: true });
  onScroll();

  /* ------------------------------------------------------------- counters */
  var western = "0123456789";
  var persian = "۰۱۲۳۴۵۶۷۸۹";
  function toFa(n) {
    return String(n).replace(/[0-9]/g, function (d) { return persian[western.indexOf(d)]; });
  }
  function toEn(s) {
    return String(s).replace(/[۰-۹]/g, function (d) { return western[persian.indexOf(d)]; });
  }

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
        var start = performance.now();
        var duration = 1250;
        function tick(now) {
          var t = Math.min(1, (now - start) / duration);
          var eased = 1 - Math.pow(1 - t, 3);
          node.textContent = toFa(Math.round(target * eased)) + suffix;
          if (t < 1) window.requestAnimationFrame(tick);
        }
        window.requestAnimationFrame(tick);
        io.unobserve(node);
      });
    }, { threshold: 0.5 });
    Array.prototype.forEach.call(counters, function (c) { io.observe(c); });
  }

  /* ---------------------------------------------------------- pointer fx */
  if (fine && !reduce) {
    var mx = 0, my = 0, cx = 0, cy = 0;
    window.addEventListener("pointermove", function (event) {
      mx = event.clientX; my = event.clientY;
      if (!cursor.classList.contains("is-visible")) cursor.classList.add("is-visible");
    }, { passive: true });

    function follow() {
      cx += (mx - cx) * 0.12;
      cy += (my - cy) * 0.12;
      cursor.style.transform = "translate3d(" + cx + "px," + cy + "px,0)";
      window.requestAnimationFrame(follow);
    }
    follow();

    Array.prototype.forEach.call(doc.querySelectorAll(".card"), function (card) {
      card.addEventListener("pointermove", function (event) {
        var r = card.getBoundingClientRect();
        var px = (event.clientX - r.left) / r.width;
        var py = (event.clientY - r.top) / r.height;
        card.style.setProperty("--mx", (px * 100).toFixed(1) + "%");
        card.style.setProperty("--my", (py * 100).toFixed(1) + "%");
        card.style.transform =
          "perspective(900px) rotateX(" + ((0.5 - py) * 5).toFixed(2) + "deg) rotateY(" +
          ((px - 0.5) * 6).toFixed(2) + "deg) translateY(-4px)";
      });
      card.addEventListener("pointerleave", function () { card.style.transform = ""; });
    });

    Array.prototype.forEach.call(doc.querySelectorAll(".btn"), function (btn) {
      btn.addEventListener("pointermove", function (event) {
        var r = btn.getBoundingClientRect();
        var dx = (event.clientX - r.left - r.width / 2) * 0.09;
        var dy = (event.clientY - r.top - r.height / 2) * 0.15;
        btn.style.transform = "translate3d(" + dx.toFixed(1) + "px," + dy.toFixed(1) + "px,0)";
      });
      btn.addEventListener("pointerleave", function () { btn.style.transform = ""; });
    });
  }
})();
