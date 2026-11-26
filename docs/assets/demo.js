/* SudoFlame adım adım sudoku animasyonları. Sunucu gerektirmez. */
(function () {
  var NS = 'http://www.w3.org/2000/svg', C = 40, reduce = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
  function el(n, a, p) { var e = document.createElementNS(NS, n); for (var k in a) e.setAttribute(k, a[k]); if (p) p.appendChild(e); return e; }
  function xy(i) { return [(i % 9) * C, Math.floor(i / 9) * C]; }
  function stateAt(d, k) {
    var placed = {}, marks = {};
    for (var s = 0; s <= k; s++) {
      var st = d.steps[s], i;
      if (st.marks) for (i in st.marks) marks[i] = st.marks[i].slice();
      if (st.remove) for (i in st.remove) if (marks[i]) marks[i] = marks[i].filter(function (x) { return st.remove[i].indexOf(x) < 0; });
      if (st.place) for (i in st.place) { placed[i] = st.place[i]; delete marks[i]; }
    }
    return { placed: placed, marks: marks };
  }
  function draw(svg, d, k) {
    while (svg.firstChild) svg.removeChild(svg.firstChild);
    var st = d.steps[k], S = stateAt(d, k), prev = k > 0 ? stateAt(d, k - 1) : { placed: {}, marks: {} };
    var unit = {}, tgt = {}, ring = {}, cross = {};
    (st.unit || []).forEach(function (i) { unit[i] = 1; });
    (st.target || []).forEach(function (i) { tgt[i] = 1; });
    (st.ring || []).forEach(function (i) { ring[i] = 1; });
    (st.cross || []).forEach(function (i) { cross[i] = 1; });
    var g = d.givens;
    for (var i = 0; i < 81; i++) {
      var p = xy(i), cls = 'c' + (unit[i] ? ' u' : '') + (tgt[i] ? ' t' : '');
      el('rect', { x: p[0], y: p[1], width: C, height: C, 'class': cls }, svg);
    }
    for (var n = 0; n <= 9; n++) {
      var w = n % 3 === 0 ? 'gl b' : 'gl';
      el('line', { x1: n * C, y1: 0, x2: n * C, y2: 9 * C, 'class': w }, svg);
      el('line', { x1: 0, y1: n * C, x2: 9 * C, y2: n * C, 'class': w }, svg);
    }
    if (st.rect) {
      var pts = st.rect.map(function (i) { var q = xy(i); return (q[0] + C / 2) + ',' + (q[1] + C / 2); }).join(' ');
      el('polygon', { points: pts, 'class': 'rect' + (reduce ? '' : ' draw') }, svg);
    }
    for (i = 0; i < 81; i++) {
      p = xy(i);
      var cx = p[0] + C / 2, cy = p[1] + C / 2, v = g[i] !== '.' ? g[i] : S.placed[i];
      if (ring[i]) el('circle', { cx: cx, cy: cy, r: 16, 'class': 'ring' + (reduce ? '' : ' pop') }, svg);
      if (v) {
        var isNew = g[i] === '.' && !prev.placed[i];
        var t = el('text', { x: cx, y: cy + 1, 'class': (g[i] !== '.' ? 'gv' : 'pl') + (isNew && !reduce ? ' pop' : '') }, svg);
        t.textContent = v;
      } else if (S.marks[i]) {
        var hi = (st.hi && st.hi[i]) || [], sk = (st.strike && st.strike[i]) || [];
        S.marks[i].forEach(function (m) {
          var big = !!d.digit, mx = big ? cx : p[0] + 7 + ((m - 1) % 3) * 13, my = big ? cy : p[1] + 7 + Math.floor((m - 1) / 3) * 13, rr = big ? 11 : 6.2, q = big ? 8 : 5;
          if (hi.indexOf(m) >= 0) el('circle', { cx: mx, cy: my, r: rr, 'class': 'mhi' }, svg);
          if (sk.indexOf(m) >= 0) el('circle', { cx: mx, cy: my, r: rr, 'class': 'msk' + (reduce ? '' : ' pop') }, svg);
          var tt = el('text', { x: mx, y: my + .5, 'class': 'mk' + (big ? ' big' : '') + (hi.indexOf(m) >= 0 ? ' h' : '') + (sk.indexOf(m) >= 0 ? ' s' : '') }, svg);
          tt.textContent = m;
          if (sk.indexOf(m) >= 0) el('line', { x1: mx - q, y1: my + q, x2: mx + q, y2: my - q, 'class': 'skl' }, svg);
        });
      }
      if (cross[i]) {
        el('line', { x1: p[0] + 11, y1: p[1] + 11, x2: p[0] + C - 11, y2: p[1] + C - 11, 'class': 'x' }, svg);
        el('line', { x1: p[0] + C - 11, y1: p[1] + 11, x2: p[0] + 11, y2: p[1] + C - 11, 'class': 'x' }, svg);
      }
    }
  }
  function setup(fig) {
    var data = JSON.parse(fig.querySelector('script[type="application/json"]').textContent);
    var svg = el('svg', { viewBox: '-2 -2 364 364', 'class': 'board', role: 'img', 'aria-label': fig.getAttribute('data-label') || 'Sudoku animasyonu' });
    fig.insertBefore(svg, fig.firstChild);
    var cap = document.createElement('figcaption'); cap.setAttribute('aria-live', 'polite');
    var ctr = document.createElement('div'); ctr.className = 'ctr';
    ctr.innerHTML = '<button type="button" data-a="first" aria-label="Başa dön">⏮</button><button type="button" data-a="prev" aria-label="Önceki adım">◀</button><button type="button" data-a="play" class="play" aria-label="Oynat">▶ Oynat</button><button type="button" data-a="next" aria-label="Sonraki adım">▶</button><span class="cnt"></span>';
    fig.appendChild(ctr); fig.appendChild(cap);
    var k = 0, timer = null, n = data.steps.length, playBtn = ctr.querySelector('.play'), cnt = ctr.querySelector('.cnt');
    function show(j) { k = Math.max(0, Math.min(n - 1, j)); draw(svg, data, k); cap.innerHTML = data.steps[k].t; cnt.textContent = (k + 1) + ' / ' + n; }
    function delay() { return 2600 + data.steps[k].t.replace(/<[^>]+>/g, '').length * 38; }
    function stop() { clearTimeout(timer); timer = null; playBtn.textContent = '▶ Oynat'; playBtn.setAttribute('aria-label', 'Oynat'); }
    function tick() { if (k >= n - 1) { stop(); return; } timer = setTimeout(function () { show(k + 1); tick(); }, delay()); }
    function play() { if (k >= n - 1) show(0); playBtn.textContent = '⏸ Durdur'; playBtn.setAttribute('aria-label', 'Durdur'); tick(); }
    ctr.addEventListener('click', function (e) {
      var a = e.target.getAttribute('data-a'); if (!a) return;
      if (a === 'play') { timer ? stop() : play(); return; }
      stop(); show(a === 'first' ? 0 : a === 'prev' ? k - 1 : k + 1);
    });
    show(0);
    if (!reduce && 'IntersectionObserver' in window) {
      var io = new IntersectionObserver(function (es) { if (es[0].isIntersecting) { io.disconnect(); setTimeout(function () { if (!timer && k === 0) play(); }, 600); } }, { threshold: .6 });
      io.observe(svg);
    }
  }
  document.querySelectorAll('figure.demo').forEach(setup);
})();
