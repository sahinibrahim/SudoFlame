package com.sudoflame.app.game

import kotlin.random.Random

enum class Difficulty(val title: String, val emoji: String, val targetClues: Int, val baseScore: Int, val xp: Int) {
    EASY("Kolay", "🌱", 40, 1000, 20),
    MEDIUM("Orta", "🔥", 33, 2000, 35),
    HARD("Zor", "⚡", 28, 3500, 50),
    EXPERT("Uzman", "💎", 24, 5000, 70);
}

/** 81 elemanlı tahtalar: 0 = boş. */
class Puzzle(val givens: IntArray, val solution: IntArray)

object SudokuGenerator {

    fun generate(difficulty: Difficulty, seed: Long = System.nanoTime()): Puzzle {
        val rnd = Random(seed)
        val solution = IntArray(81)
        fillRandom(solution, rnd)
        val puzzle = solution.copyOf()

        val order = (0 until 81).shuffled(rnd)
        var clues = 81
        for (idx in order) {
            if (clues <= difficulty.targetClues) break
            val backup = puzzle[idx]
            puzzle[idx] = 0
            if (countSolutions(puzzle.copyOf(), 2) != 1) {
                puzzle[idx] = backup
            } else {
                clues--
            }
        }
        return Puzzle(puzzle, solution)
    }

    /** Günün bulmacası: o gün herkes (ve arkadaşların) aynı bulmacayı çözer. */
    fun daily(epochDay: Long): Puzzle =
        generate(dailyDifficulty(epochDay), epochDay * 7919L + 104729L)

    fun dailyDifficulty(epochDay: Long): Difficulty = when ((epochDay % 7).toInt()) {
        0, 1, 2 -> Difficulty.EASY
        3, 4 -> Difficulty.MEDIUM
        5 -> Difficulty.HARD
        else -> Difficulty.MEDIUM
    }

    private fun candidates(b: IntArray, i: Int): Int {
        val r = i / 9; val c = i % 9
        var used = 0
        for (k in 0 until 9) {
            used = used or (1 shl b[r * 9 + k]) or (1 shl b[k * 9 + c])
        }
        val br = r / 3 * 3; val bc = c / 3 * 3
        for (rr in br until br + 3) for (cc in bc until bc + 3) used = used or (1 shl b[rr * 9 + cc])
        return used.inv() and 0x3FE // 1..9 bitleri
    }

    private fun fillRandom(b: IntArray, rnd: Random): Boolean {
        val i = b.indexOfFirst { it == 0 }
        if (i == -1) return true
        val cand = candidates(b, i)
        val nums = (1..9).filter { cand and (1 shl it) != 0 }.shuffled(rnd)
        for (n in nums) {
            b[i] = n
            if (fillRandom(b, rnd)) return true
        }
        b[i] = 0
        return false
    }

    /** limit kadar çözüm bulunca durur. En az aday olan hücreden dallanır (hızlı). */
    fun countSolutions(b: IntArray, limit: Int): Int {
        var best = -1; var bestCand = 0; var bestCount = 10
        for (i in 0 until 81) {
            if (b[i] != 0) continue
            val cand = candidates(b, i)
            val cnt = Integer.bitCount(cand)
            if (cnt == 0) return 0
            if (cnt < bestCount) { best = i; bestCand = cand; bestCount = cnt; if (cnt == 1) break }
        }
        if (best == -1) return 1
        var total = 0
        for (n in 1..9) {
            if (bestCand and (1 shl n) == 0) continue
            b[best] = n
            total += countSolutions(b, limit - total)
            if (total >= limit) break
        }
        b[best] = 0
        return total
    }
}
