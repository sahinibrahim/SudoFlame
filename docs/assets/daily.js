/* SudoFlame günlük sudoku: tarayıcıda oynanır, ilerleme bu tarayıcıda saklanır. */
(function () {
  'use strict';
  var root = document.getElementById('daily');
  if (!root || !window.SudokuGen) return;

  var MAX_BACK = 30; // kaç gün geriye gidilebilir
  var AYLAR = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
  var GUNLER = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];

  // ---- güvenli localStorage ----
  function load(k) { try { var v = localStorage.getItem(k); return v ? JSON.parse(v) : null; } catch (e) { return null; } }
  function save(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) { } }

  function todayEpoch() { var d = new Date(); return Math.floor(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / 86400000); }
  function dateOf(ep) { return new Date(ep * 86400000); }
  function fmtDate(ep) { var d = dateOf(ep); return d.getUTCDate() + ' ' + AYLAR[d.getUTCMonth()] + ' ' + d.getUTCFullYear() + ', ' + GUNLER[d.getUTCDay()]; }
  function shortDate(ep) { var d = dateOf(ep); return d.getUTCDate() + '.' + (d.getUTCMonth() + 1) + '.' + d.getUTCFullYear(); }
  function fmtTime(s) { var m = Math.floor(s / 60), r = s % 60; return m + ':' + (r < 10 ? '0' : '') + r; }

  var $ = function (sel) { return root.querySelector(sel); };
  var boardEl = $('.sf-board'), padEl = $('.sf-pad'), timeEl = $('[data-time]'), mistEl = $('[data-mist]'),
    dateEl = $('[data-date]'), diffEl = $('[data-diff]'), prevBtn = $('[data-prev]'), nextBtn = $('[data-next]'),
    noteBtn = $('[data-act=note]'), winEl = $('.sf-win'), streakEl = document.querySelector('[data-streak]');

  var today = todayEpoch(), day = today, P, S, sel = -1, noteMode = false, timer = null, cells = [], padBtns = [];

  // URL'de ?gun=YYYY-MM-DD varsa o güne git
  (function () {
    var m = /[?&]gun=(\d{4})-(\d{2})-(\d{2})/.exec(location.search);
    if (m) { var ep = Math.floor(Date.UTC(+m[1], +m[2] - 1, +m[3]) / 86400000); if (ep <= today && ep >= today - MAX_BACK) day = ep; }
  })();

  // ---- tahta ----
  for (var i = 0; i < 81; i++) {
    var b = document.createElement('button');
    b.type = 'button'; b.className = 'sf-cell'; b.dataset.i = i;
    var r = (i / 9) | 0, c = i % 9;
    if (c % 3 === 2 && c < 8) b.classList.add('br');
    if (r % 3 === 2 && r < 8) b.classList.add('bb');
    b.addEventListener('click', function () { select(+this.dataset.i); });
    boardEl.appendChild(b); cells.push(b);
  }
  for (var n = 1; n <= 9; n++) {
    var pb = document.createElement('button');
    pb.type = 'button'; pb.className = 'sf-num'; pb.dataset.n = n;
    pb.innerHTML = n + '<small></small>';
    pb.setAttribute('aria-label', n + ' yaz');
    pb.addEventListener('click', function () { input(+this.dataset.n); });
    padEl.appendChild(pb); padBtns.push(pb);
  }

  function key(ep) { return 'sf-daily-' + ep; }

  function startDay(ep) {
    day = ep; stopTimer(); sel = -1;
    P = window.SudokuGen.daily(ep);
    S = load(key(ep));
    if (!S || !S.v || S.v.length !== 81) {
      S = { v: P.givens.slice(), notes: new Array(81).fill(0), t: 0, m: 0, h: 0, done: false, undo: [] };
    }
    dateEl.textContent = fmtDate(ep) + (ep === today ? ' · Bugün' : '');
    diffEl.textContent = P.difficulty.emoji + ' ' + P.difficulty.title;
    prevBtn.disabled = ep <= today - MAX_BACK;
    nextBtn.disabled = ep >= today;
    winEl.hidden = true;
    render();
    if (S.done) showWin(); else startTimer();
  }

  function persist() { var c = Object.assign({}, S); c.undo = S.undo.slice(-60); save(key(day), c); }

  function peers(i) {
    var r = (i / 9) | 0, c = i % 9, br = ((r / 3) | 0) * 3, bc = ((c / 3) | 0) * 3, out = [];
    for (var k = 0; k < 9; k++) { out.push(r * 9 + k); out.push(k * 9 + c); }
    for (var rr = br; rr < br + 3; rr++) for (var cc = bc; cc < bc + 3; cc++) out.push(rr * 9 + cc);
    return out;
  }

  function render() {
    var selVal = sel >= 0 ? S.v[sel] : 0, peer = {};
    if (sel >= 0) peers(sel).forEach(function (p) { peer[p] = 1; });
    var counts = new Array(10).fill(0);
    for (var i = 0; i < 81; i++) {
      var el = cells[i], v = S.v[i], given = P.givens[i] !== 0, wrong = v && v !== P.solution[i];
      if (v && !wrong) counts[v]++;
      el.className = 'sf-cell' + (el.classList.contains('br') ? ' br' : '') + (el.classList.contains('bb') ? ' bb' : '') +
        (given ? ' g' : '') + (wrong ? ' bad' : '') + (i === sel ? ' sel' : peer[i] ? ' peer' : '') +
        (selVal && v === selVal && i !== sel ? ' same' : '');
      var r = ((i / 9) | 0) + 1, c = (i % 9) + 1;
      if (v) { el.textContent = v; el.setAttribute('aria-label', r + '. satır ' + c + '. sütun: ' + v + (wrong ? ', hatalı' : '')); }
      else if (S.notes[i]) {
        var h = '<span class="nt">';
        for (var n = 1; n <= 9; n++) h += '<i>' + ((S.notes[i] & (1 << n)) ? n : '') + '</i>';
        el.innerHTML = h + '</span>';
        el.setAttribute('aria-label', r + '. satır ' + c + '. sütun: boş, notlu');
      } else { el.textContent = ''; el.setAttribute('aria-label', r + '. satır ' + c + '. sütun: boş'); }
    }
    padBtns.forEach(function (b, k) {
      var left = 9 - counts[k + 1];
      b.querySelector('small').textContent = left > 0 ? left : '';
      b.classList.toggle('full', left <= 0);
    });
    noteBtn.classList.toggle('on', noteMode);
    noteBtn.setAttribute('aria-pressed', noteMode ? 'true' : 'false');
    timeEl.textContent = fmtTime(S.t);
    mistEl.textContent = S.m;
    if (streakEl) streakEl.textContent = streak();
  }

  function select(i) { sel = i; render(); if (i >= 0) cells[i].focus({ preventScroll: true }); }

  function pushUndo(i) { S.undo.push([i, S.v[i], S.notes[i]]); }

  function input(n) {
    if (S.done || sel < 0 || P.givens[sel] !== 0) return;
    if (noteMode) {
      if (S.v[sel]) return;
      pushUndo(sel);
      S.notes[sel] ^= 1 << n;
    } else {
      if (S.v[sel] === n) return;
      pushUndo(sel);
      S.v[sel] = n; S.notes[sel] = 0;
      if (n !== P.solution[sel]) S.m++;
      else peers(sel).forEach(function (p) { if (S.notes[p] & (1 << n)) S.notes[p] &= ~(1 << n); });
    }
    persist(); render(); checkWin();
  }

  function erase() {
    if (S.done || sel < 0 || P.givens[sel] !== 0 || (!S.v[sel] && !S.notes[sel])) return;
    pushUndo(sel); S.v[sel] = 0; S.notes[sel] = 0; persist(); render();
  }

  function undo() {
    if (S.done || !S.undo.length) return;
    var u = S.undo.pop(); S.v[u[0]] = u[1]; S.notes[u[0]] = u[2]; sel = u[0]; persist(); render();
  }

  function hint() {
    if (S.done) return;
    var i = sel;
    if (i < 0 || P.givens[i] !== 0 || S.v[i] === P.solution[i]) {
      i = -1;
      for (var k = 0; k < 81; k++) if (S.v[k] !== P.solution[k]) { i = k; break; }
    }
    if (i < 0) return;
    pushUndo(i); S.v[i] = P.solution[i]; S.notes[i] = 0; S.h++;
    var n = P.solution[i];
    peers(i).forEach(function (p) { if (S.notes[p] & (1 << n)) S.notes[p] &= ~(1 << n); });
    sel = i; persist(); render(); checkWin();
  }

  function checkWin() {
    for (var i = 0; i < 81; i++) if (S.v[i] !== P.solution[i]) return;
    S.done = true; stopTimer();
    var solved = load('sf-daily-solved') || [];
    if (solved.indexOf(day) < 0) { solved.push(day); solved.sort(function (a, b) { return a - b; }); save('sf-daily-solved', solved.slice(-400)); }
    persist(); sel = -1; render(); showWin(true);
  }

  function streak() {
    var solved = load('sf-daily-solved') || [], set = {}, s = 0;
    solved.forEach(function (d) { set[d] = 1; });
    var d = set[today] ? today : today - 1;
    while (set[d]) { s++; d--; }
    return s;
  }

  function shareText() {
    return 'SudoFlame Günlük Sudoku ' + shortDate(day) + ' ' + P.difficulty.emoji + '\n✅ ' + fmtTime(S.t) +
      (S.m ? ' · ❌ ' + S.m : ' · hatasız') + (S.h ? ' · 💡 ' + S.h : '') + (day === today ? ' · 🔥 ' + streak() + ' gün' : '') +
      '\nhttps://sudoflame.com/gunluk/';
  }

  function showWin(fresh) {
    winEl.hidden = false;
    winEl.querySelector('[data-wtime]').textContent = fmtTime(S.t);
    winEl.querySelector('[data-wmist]').textContent = S.m;
    winEl.querySelector('[data-whint]').textContent = S.h;
    winEl.querySelector('[data-wstreak]').textContent = streak();
    if (fresh) winEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function startTimer() {
    stopTimer();
    timer = setInterval(function () {
      if (document.hidden || S.done) return;
      S.t++; timeEl.textContent = fmtTime(S.t);
      if (S.t % 5 === 0) persist();
    }, 1000);
  }
  function stopTimer() { if (timer) clearInterval(timer); timer = null; }

  // ---- olaylar ----
  root.addEventListener('click', function (e) {
    var a = e.target.closest('[data-act]'); if (!a) return;
    var act = a.dataset.act;
    if (act === 'note') { noteMode = !noteMode; render(); }
    else if (act === 'erase') erase();
    else if (act === 'undo') undo();
    else if (act === 'hint') hint();
    else if (act === 'share') {
      var txt = shareText();
      if (navigator.share) navigator.share({ text: txt }).catch(function () { });
      else if (navigator.clipboard) navigator.clipboard.writeText(txt).then(function () { a.textContent = 'Kopyalandı ✓'; });
    } else if (act === 'reset') {
      S = { v: P.givens.slice(), notes: new Array(81).fill(0), t: 0, m: 0, h: 0, done: false, undo: [] };
      persist(); winEl.hidden = true; render(); startTimer();
    }
  });
  prevBtn.addEventListener('click', function () { if (day > today - MAX_BACK) startDay(day - 1); });
  nextBtn.addEventListener('click', function () { if (day < today) startDay(day + 1); });

  boardEl.addEventListener('keydown', function (e) {
    var k = e.key;
    if (k >= '1' && k <= '9') { input(+k); e.preventDefault(); return; }
    if (k === 'Backspace' || k === 'Delete' || k === '0') { erase(); e.preventDefault(); return; }
    if (k === 'n' || k === 'N') { noteMode = !noteMode; render(); return; }
    if ((e.ctrlKey || e.metaKey) && (k === 'z' || k === 'Z')) { undo(); e.preventDefault(); return; }
    var d = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -9, ArrowDown: 9 }[k];
    if (d !== undefined) {
      var cur = sel < 0 ? 0 : sel, r = (cur / 9) | 0, c = cur % 9;
      if (d === -1) c = (c + 8) % 9; else if (d === 1) c = (c + 1) % 9; else if (d === -9) r = (r + 8) % 9; else r = (r + 1) % 9;
      select(r * 9 + c); e.preventDefault();
    }
  });

  document.addEventListener('visibilitychange', function () { if (document.hidden && S && !S.done) persist(); });
  // gece yarısı geçince "bugün" değişsin
  setInterval(function () { var t = todayEpoch(); if (t !== today) { var wasToday = day === today; today = t; if (wasToday) startDay(t); else nextBtn.disabled = day >= today; } }, 60000);

  root.classList.add('ready');
  startDay(day);
})();
