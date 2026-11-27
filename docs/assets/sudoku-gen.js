/*
 * SudoFlame günlük bulmaca üreticisi.
 * Android uygulamasındaki SudokuGenerator (game/Sudoku.kt) ve Kotlin'in
 * kotlin.random.Random (XorWow) davranışının birebir JavaScript karşılığıdır.
 * Böylece sitede ve uygulamada aynı gün aynı bulmaca çıkar.
 * Uygulamadaki üretici değişirse bu dosya da güncellenmeli.
 */
(function (root) {
  'use strict';

  // ---- Kotlin XorWowRandom ----
  function XorWow(seed1, seed2) {
    this.x = seed1 | 0; this.y = seed2 | 0; this.z = 0; this.w = 0;
    this.v = ~seed1 | 0; this.addend = ((seed1 << 10) ^ (seed2 >>> 4)) | 0;
    for (var i = 0; i < 64; i++) this.nextInt();
  }
  XorWow.prototype.nextInt = function () {
    var t = this.x;
    t = t ^ (t >>> 2);
    this.x = this.y; this.y = this.z; this.z = this.w;
    var v0 = this.v;
    this.w = v0;
    t = (t ^ (t << 1)) ^ v0 ^ (v0 << 4);
    this.v = t | 0;
    this.addend = (this.addend + 362437) | 0;
    return (t + this.addend) | 0;
  };
  XorWow.prototype.nextBits = function (bitCount) {
    var r = this.nextInt();
    return (r >>> (32 - bitCount)) & ((-bitCount) >> 31);
  };
  // Random.nextInt(until) — Kotlin'deki nextInt(0, until)
  XorWow.prototype.nextIntUntil = function (n) {
    if ((n & -n) === n) return this.nextBits(31 - Math.clz32(n));
    var bits, v;
    do {
      bits = this.nextInt() >>> 1;
      v = bits % n;
    } while (((bits - v + (n - 1)) | 0) < 0);
    return v;
  };
  // Random(seed: Long): seed.toInt() ve seed.shr(32).toInt()
  function kotlinRandom(seed) {
    var big = BigInt.asIntN(64, BigInt(seed));
    var lo = Number(BigInt.asIntN(32, big));
    var hi = Number(BigInt.asIntN(32, big >> 32n));
    return new XorWow(lo, hi);
  }
  // List.shuffled(random)
  function shuffled(list, rnd) {
    var a = list.slice();
    for (var i = a.length - 1; i >= 1; i--) {
      var j = rnd.nextIntUntil(i + 1);
      var tmp = a[j]; a[j] = a[i]; a[i] = tmp;
    }
    return a;
  }

  // ---- SudokuGenerator ----
  var DIFF = {
    EASY: { key: 'EASY', title: 'Kolay', emoji: '🌱', clues: 40 },
    MEDIUM: { key: 'MEDIUM', title: 'Orta', emoji: '🔥', clues: 33 },
    HARD: { key: 'HARD', title: 'Zor', emoji: '⚡', clues: 28 },
    EXPERT: { key: 'EXPERT', title: 'Uzman', emoji: '💎', clues: 24 }
  };

  function candidates(b, i) {
    var r = (i / 9) | 0, c = i % 9, used = 0, k;
    for (k = 0; k < 9; k++) used |= (1 << b[r * 9 + k]) | (1 << b[k * 9 + c]);
    var br = ((r / 3) | 0) * 3, bc = ((c / 3) | 0) * 3;
    for (var rr = br; rr < br + 3; rr++) for (var cc = bc; cc < bc + 3; cc++) used |= 1 << b[rr * 9 + cc];
    return ~used & 0x3FE;
  }

  function fillRandom(b, rnd) {
    var i = b.indexOf(0);
    if (i === -1) return true;
    var cand = candidates(b, i), nums = [];
    for (var n = 1; n <= 9; n++) if (cand & (1 << n)) nums.push(n);
    nums = shuffled(nums, rnd);
    for (var k = 0; k < nums.length; k++) {
      b[i] = nums[k];
      if (fillRandom(b, rnd)) return true;
    }
    b[i] = 0;
    return false;
  }

  function popcount(x) { var c = 0; while (x) { x &= x - 1; c++; } return c; }

  function countSolutions(b, limit) {
    var best = -1, bestCand = 0, bestCount = 10;
    for (var i = 0; i < 81; i++) {
      if (b[i] !== 0) continue;
      var cand = candidates(b, i), cnt = popcount(cand);
      if (cnt === 0) return 0;
      if (cnt < bestCount) { best = i; bestCand = cand; bestCount = cnt; if (cnt === 1) break; }
    }
    if (best === -1) return 1;
    var total = 0;
    for (var n = 1; n <= 9; n++) {
      if (!(bestCand & (1 << n))) continue;
      b[best] = n;
      total += countSolutions(b, limit - total);
      if (total >= limit) break;
    }
    b[best] = 0;
    return total;
  }

  function generate(diff, seed) {
    var rnd = kotlinRandom(seed);
    var solution = new Array(81).fill(0);
    fillRandom(solution, rnd);
    var puzzle = solution.slice();
    var idx = []; for (var i = 0; i < 81; i++) idx.push(i);
    var order = shuffled(idx, rnd), clues = 81;
    for (var k = 0; k < order.length; k++) {
      if (clues <= diff.clues) break;
      var p = order[k], backup = puzzle[p];
      puzzle[p] = 0;
      if (countSolutions(puzzle.slice(), 2) !== 1) puzzle[p] = backup; else clues--;
    }
    return { givens: puzzle, solution: solution, difficulty: diff };
  }

  function dailyDifficulty(epochDay) {
    switch (((epochDay % 7) + 7) % 7) {
      case 0: case 1: case 2: return DIFF.EASY;
      case 3: case 4: return DIFF.MEDIUM;
      case 5: return DIFF.HARD;
      default: return DIFF.MEDIUM;
    }
  }

  function daily(epochDay) {
    return generate(dailyDifficulty(epochDay), BigInt(epochDay) * 7919n + 104729n);
  }

  var api = { daily: daily, dailyDifficulty: dailyDifficulty, generate: generate, DIFF: DIFF, countSolutions: countSolutions };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.SudokuGen = api;
})(this);
