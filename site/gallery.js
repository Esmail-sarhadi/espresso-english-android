/* Gallery lightbox — vanilla JS, injected so gallery.html stays clean. */
(function () {
  "use strict";

  var doc = document;
  var items = Array.prototype.slice.call(doc.querySelectorAll(".gal-card img"));
  if (!items.length) return;

  var box = doc.createElement("div");
  box.className = "gal-lightbox";
  box.id = "lightbox";
  box.setAttribute("role", "dialog");
  box.setAttribute("aria-modal", "true");
  box.setAttribute("aria-label", "نمایش تصویر");
  box.innerHTML =
    '<button type="button" data-gal-close aria-label="بستن">×</button>' +
    '<img id="lightbox-img" src="" alt="">' +
    '<div class="gal-lightbox__nav" dir="ltr">' +
      '<button type="button" data-gal-prev aria-label="قبلی">‹</button>' +
      '<button type="button" data-gal-next aria-label="بعدی">›</button>' +
    '</div>';
  doc.body.appendChild(box);

  var img = box.querySelector("#lightbox-img");
  var index = 0;
  var lastFocused = null;

  function show(i) {
    index = (i + items.length) % items.length;
    img.src = items[index].src;
    img.alt = items[index].alt;
  }

  function open(i) {
    lastFocused = doc.activeElement;
    show(i);
    box.classList.add("is-open");
    box.querySelector("[data-gal-close]").focus();
  }

  function close() {
    box.classList.remove("is-open");
    img.src = "";
    if (lastFocused && typeof lastFocused.focus === "function") lastFocused.focus();
  }

  items.forEach(function (node, i) {
    node.setAttribute("tabindex", "0");
    node.setAttribute("role", "button");
    node.addEventListener("click", function () { open(i); });
    node.addEventListener("keydown", function (e) {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); open(i); }
    });
  });

  box.addEventListener("click", function (e) {
    if (e.target === box) close();
  });
  box.querySelector("[data-gal-close]").addEventListener("click", close);
  box.querySelector("[data-gal-prev]").addEventListener("click", function () { show(index - 1); });
  box.querySelector("[data-gal-next]").addEventListener("click", function () { show(index + 1); });

  doc.addEventListener("keydown", function (e) {
    if (!box.classList.contains("is-open")) return;
    if (e.key === "Escape") { close(); return; }
    // RTL: left arrow moves forward through the strip
    if (e.key === "ArrowLeft") show(index + 1);
    if (e.key === "ArrowRight") show(index - 1);
  });
})();
