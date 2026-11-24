<div class="gal-lightbox" id="lightbox" role="dialog" aria-modal="true" aria-label="نمایش تصویر">
  <button type="button" data-gal-close aria-label="بستن">×</button>
  <img id="lightbox-img" src="" alt="">
  <div class="gal-lightbox__nav" dir="ltr">
    <button type="button" data-gal-prev aria-label="قبلی">‹</button>
    <button type="button" data-gal-next aria-label="بعدی">›</button>
  </div>
</div>
<script>
(function () {
  var box = document.getElementById("lightbox");
  var img = document.getElementById("lightbox-img");
  var items = Array.prototype.slice.call(document.querySelectorAll(".gal-card img"));
  var index = 0;

  function show(i) {
    index = (i + items.length) % items.length;
    img.src = items[index].src;
    img.alt = items[index].alt;
  }
  function open(i) { show(i); box.classList.add("is-open"); }
  function close() { box.classList.remove("is-open"); img.src = ""; }

  items.forEach(function (node, i) {
    node.addEventListener("click", function () { open(i); });
  });
  box.addEventListener("click", function (e) {
    if (e.target === box) close();
  });
  box.querySelector("[data-gal-close]").addEventListener("click", close);
  box.querySelector("[data-gal-prev]").addEventListener("click", function () { show(index - 1); });
  box.querySelector("[data-gal-next]").addEventListener("click", function () { show(index + 1); });
  document.addEventListener("keydown", function (e) {
    if (!box.classList.contains("is-open")) return;
    if (e.key === "Escape") close();
    if (e.key === "ArrowLeft") show(index + 1);
    if (e.key === "ArrowRight") show(index - 1);
  });
})();
</script>
