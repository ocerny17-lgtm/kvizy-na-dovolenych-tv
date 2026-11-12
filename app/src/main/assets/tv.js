// Úpravy webu pro ovládání ovladačem na TV. Vkládá je aplikace, web se nemění.
// __LAYOUT_WIDTH__ nahradí aplikace šířkou rozvržení v CSS px (oddálení stránky).
(function () {
  if (window.__kvizyTv) return;
  window.__kvizyTv = true;

  // ---------- 1) Oddálení: stránka se rozvrhne do širšího viewportu ----------
  var LAYOUT_WIDTH = __LAYOUT_WIDTH__;
  function fixViewport() {
    if (!document.documentElement) return;
    var metas = document.querySelectorAll('meta[name="viewport"]');
    var m = metas[0];
    if (!m) {
      if (!document.head) return;
      m = document.createElement('meta');
      m.name = 'viewport';
      document.head.appendChild(m);
    }
    var want = 'width=' + LAYOUT_WIDTH;
    if (m.getAttribute('content') !== want) m.setAttribute('content', want);
  }
  fixViewport();
  var mo = new MutationObserver(fixViewport);
  mo.observe(document, { childList: true, subtree: true });
  document.addEventListener('DOMContentLoaded', function () {
    fixViewport();
    setTimeout(function () { mo.disconnect(); }, 2000);
  });

  // ---------- 2) Pomocné funkce pro fokus ----------
  var FOCUSABLE = 'button, a[href], input:not([type=hidden]), select, textarea, [tabindex]';

  function visible(el) {
    if (el.disabled || el.tabIndex < 0) return false;
    var r = el.getBoundingClientRect();
    if (r.width === 0 || r.height === 0) return false;
    var s = getComputedStyle(el);
    return s.visibility !== 'hidden' && s.display !== 'none';
  }

  function scope() {
    // Otevřené modální okno má přednost před obrazovkou pod ním.
    var modals = document.querySelectorAll('.modal-bg');
    for (var i = 0; i < modals.length; i++) {
      if (getComputedStyle(modals[i]).display !== 'none') return modals[i];
    }
    return document;
  }

  // Nejbližší prvek ve směru dolů / nahoru (jednoduchá prostorová navigace).
  function moveVertical(from, down) {
    var a = from.getBoundingClientRect();
    var ax = a.left + a.width / 2;
    var best = null, bestScore = Infinity;
    var all = scope().querySelectorAll(FOCUSABLE);
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (el === from || !visible(el)) continue;
      var b = el.getBoundingClientRect();
      var dy = down ? b.top - a.bottom : a.top - b.bottom;
      if (dy < -4) continue;               // není v daném směru
      var bx = Math.max(b.left, Math.min(ax, b.right));
      var dx = Math.abs(bx - ax);
      var score = Math.max(dy, 0) + dx * 2;
      if (score < bestScore) { bestScore = score; best = el; }
    }
    if (best) {
      best.focus();
      best.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
    return !!best;
  }

  // ---------- 3) Posuvník: šipky nahoru/dolů z něj odejdou ----------
  window.addEventListener('keydown', function (e) {
    var t = e.target;
    if (!t || t.tagName !== 'INPUT' || t.type !== 'range') return;
    if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
      e.preventDefault();
      e.stopPropagation();
      moveVertical(t, e.key === 'ArrowDown');
    }
  }, true);

  // ---------- 4) Tlačítko Zpět na ovladači ----------
  var BACK_TEXT = /zpět|zrušit|hrát dál|hlavní menu|do menu/i;

  function clickBack(root) {
    var btns = root.querySelectorAll('button');
    for (var i = 0; i < btns.length; i++) {
      var b = btns[i];
      if (visible(b) && BACK_TEXT.test(b.textContent)) { b.click(); return true; }
    }
    return false;
  }

  // Vrací true, pokud Zpět zpracoval web; jinak rozhodne aplikace.
  window.__kvizyTvBack = function () {
    // a) otevřené modální okno → zavřít
    var root = scope();
    if (root !== document) {
      if (clickBack(root)) return true;
      var only = root.querySelectorAll('button');
      for (var i = 0; i < only.length; i++) {
        if (visible(only[i])) { only[i].click(); return true; }
      }
      return true;
    }
    // b) fokus v posuvníku nebo textovém poli → jen z něj odejít na další prvek (např. „+“)
    var f = document.activeElement;
    if (f && f.tagName === 'INPUT' && /^(range|text|password|number|search|email)$/.test(f.type)) {
      var all = Array.prototype.filter.call(document.querySelectorAll(FOCUSABLE), visible);
      var next = all[all.indexOf(f) + 1];
      if (next) next.focus(); else f.blur();
      return true;
    }
    // c) obrazovka mimo hlavní menu s tlačítkem Zpět
    var screen = document.querySelector('.screen.active');
    if (screen && screen.id !== 's-menu' && clickBack(screen)) return true;
    return false;
  };

  // Pomocník pro aplikaci: jsme v hlavním menu?
  window.__kvizyTvOnMenu = function () {
    var screen = document.querySelector('.screen.active');
    return !screen || screen.id === 's-menu';
  };
})();
