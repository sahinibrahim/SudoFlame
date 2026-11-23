package com.sudoflame.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudoflame.app.data.FriendRepository
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.data.WinResult
import com.sudoflame.app.game.Difficulty
import com.sudoflame.app.game.SudokuGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Ekran kapansa da biten işler (ör. arkadaş serisini güncelleme) için. */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)

enum class GameStatus { LOADING, PLAYING, WON, LOST }

class GameState(
    val difficulty: Difficulty = Difficulty.EASY,
    val givens: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val values: IntArray = IntArray(81),
    val notes: IntArray = IntArray(81), // bit n = n notu
    val selected: Int = -1,
    val mistakes: Int = 0,
    val hintsLeft: Int = 2,
    val seconds: Int = 0,
    val noteMode: Boolean = false,
    val status: GameStatus = GameStatus.LOADING,
    val isDaily: Boolean = false,
    val dailyDay: Long = -1,
    val secondChanceUsed: Boolean = false,
    val lastPlaced: Int = -1,
    val lastWrong: Int = -1,
    val win: WinResult? = null,
) {
    fun copy(
        values: IntArray = this.values, notes: IntArray = this.notes, selected: Int = this.selected,
        mistakes: Int = this.mistakes, hintsLeft: Int = this.hintsLeft, seconds: Int = this.seconds,
        noteMode: Boolean = this.noteMode, status: GameStatus = this.status, secondChanceUsed: Boolean = this.secondChanceUsed,
        lastPlaced: Int = this.lastPlaced, lastWrong: Int = this.lastWrong, win: WinResult? = this.win,
    ) = GameState(
        difficulty, givens, solution, values, notes, selected, mistakes, hintsLeft, seconds, noteMode, status,
        isDaily, dailyDay, secondChanceUsed, lastPlaced, lastWrong, win
    )

    /** Doğru yerleştirilmiş adetler: 9 olunca o rakamın tuşu kaybolur. */
    fun placedCount(n: Int): Int = (0 until 81).count { values[it] == n && solution[it] == n }
    fun isWrong(i: Int) = values[i] != 0 && values[i] != solution[i]

    /** Canlı puan: doğru yerleştirilen her hücre puan kazandırır, hatalar ve süre düşürür. */
    val liveScore: Int
        get() {
            val correct = (0 until 81).count { givens[it] == 0 && values[it] == solution[it] }
            val perCell = difficulty.baseScore / 20
            return (correct * perCell - mistakes * perCell * 2 - seconds / 6).coerceAtLeast(0)
        }
}

class GameViewModel : ViewModel() {
    var state by mutableStateOf(GameState(), neverEqualPolicy())
        private set
    private val undo = ArrayDeque<Pair<IntArray, IntArray>>()
    private var timer: Job? = null
    private var paused = false
    private var started = false

    companion object { const val MAX_MISTAKES = 3 }

    fun start(mode: String, difficulty: Difficulty, day: Long) {
        if (started) return
        started = true
        viewModelScope.launch {
            if (mode == "resume") {
                val restored = ProgressRepository.loadGame()?.let { runCatching { fromJson(it) }.getOrNull() }
                if (restored != null) { state = restored; startTimer(); return@launch }
            }
            val isDaily = mode == "daily"
            val puzzle = withContext(Dispatchers.Default) {
                if (isDaily) SudokuGenerator.daily(day) else SudokuGenerator.generate(difficulty)
            }
            val diff = if (isDaily) SudokuGenerator.dailyDifficulty(day) else difficulty
            state = GameState(
                difficulty = diff, givens = puzzle.givens, solution = puzzle.solution,
                values = puzzle.givens.copyOf(), status = GameStatus.PLAYING,
                isDaily = isDaily, dailyDay = if (isDaily) day else -1,
            )
            ProgressRepository.recordGameStarted()
            persist()
            startTimer()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!paused && state.status == GameStatus.PLAYING) {
                    state = state.copy(seconds = state.seconds + 1)
                    if (state.seconds % 10 == 0) persist()
                }
            }
        }
    }

    fun setPaused(p: Boolean) { paused = p; if (p) persist() }

    fun select(i: Int) { state = state.copy(selected = i, lastWrong = -1) }

    fun toggleNotes() { state = state.copy(noteMode = !state.noteMode) }

    private fun pushUndo() {
        undo.addLast(state.values.copyOf() to state.notes.copyOf())
        if (undo.size > 100) undo.removeFirst()
    }

    /** @return true ise hata yapıldı (titreşim için). */
    fun input(n: Int): Boolean {
        val s = state
        val i = s.selected
        if (paused || s.status != GameStatus.PLAYING || i < 0 || s.givens[i] != 0) return false
        if (s.values[i] != 0 && s.values[i] == s.solution[i]) return false // doğru hücre kilitli
        if (s.noteMode && s.values[i] != 0) return false
        pushUndo()
        val values = s.values.copyOf(); val notes = s.notes.copyOf()
        if (s.noteMode) {
            notes[i] = notes[i] xor (1 shl n)
            state = s.copy(values = values, notes = notes)
            persist(); return false
        }
        values[i] = n
        notes[i] = 0
        val wrong = n != s.solution[i]
        if (!wrong && ProgressRepository.current.autoRemoveNotes) clearPeerNotes(notes, i, n)
        var mistakes = s.mistakes
        var status = s.status
        if (wrong) {
            mistakes++
            if (mistakes >= MAX_MISTAKES) status = GameStatus.LOST
        }
        state = s.copy(values = values, notes = notes, mistakes = mistakes, status = status,
            lastPlaced = if (wrong) -1 else i, lastWrong = if (wrong) i else -1)
        checkWin()
        persist()
        return wrong
    }

    private fun clearPeerNotes(notes: IntArray, i: Int, n: Int) {
        val r = i / 9; val c = i % 9
        val mask = (1 shl n).inv()
        for (k in 0 until 9) { notes[r * 9 + k] = notes[r * 9 + k] and mask; notes[k * 9 + c] = notes[k * 9 + c] and mask }
        val br = r / 3 * 3; val bc = c / 3 * 3
        for (rr in br until br + 3) for (cc in bc until bc + 3) notes[rr * 9 + cc] = notes[rr * 9 + cc] and mask
    }

    fun erase() {
        val s = state; val i = s.selected
        if (s.status != GameStatus.PLAYING || i < 0 || s.givens[i] != 0) return
        if (s.values[i] != 0 && s.values[i] == s.solution[i]) return
        pushUndo()
        val values = s.values.copyOf(); val notes = s.notes.copyOf()
        values[i] = 0; notes[i] = 0
        state = s.copy(values = values, notes = notes)
        persist()
    }

    fun undo() {
        if (state.status != GameStatus.PLAYING) return
        val (v, n) = undo.removeLastOrNull() ?: return
        state = state.copy(values = v, notes = n)
        persist()
    }

    /** @return false ise ipucu hakkı yok (reklam önerilir). */
    fun hint(): Boolean {
        val s = state
        if (s.status != GameStatus.PLAYING) return true
        if (s.hintsLeft <= 0) return false
        val target = if (s.selected >= 0 && s.values[s.selected] != s.solution[s.selected]) s.selected
        else (0 until 81).filter { s.values[it] != s.solution[it] }.randomOrNull() ?: return true
        pushUndo()
        val values = s.values.copyOf(); val notes = s.notes.copyOf()
        values[target] = s.solution[target]; notes[target] = 0
        clearPeerNotes(notes, target, s.solution[target])
        state = s.copy(values = values, notes = notes, hintsLeft = s.hintsLeft - 1, selected = target, lastPlaced = target)
        checkWin(); persist()
        return true
    }

    fun addHintFromAd() { state = state.copy(hintsLeft = state.hintsLeft + 1) }

    fun secondChance() {
        state = state.copy(mistakes = MAX_MISTAKES - 1, status = GameStatus.PLAYING, secondChanceUsed = true)
        persist()
    }

    private fun checkWin() {
        val s = state
        if (s.status != GameStatus.PLAYING) {
            if (s.status == GameStatus.LOST) persist()
            return
        }
        if (!s.values.contentEquals(s.solution)) return
        val result = ProgressRepository.recordWin(s.difficulty, s.seconds, s.mistakes, s.isDaily, s.dailyDay)
        state = s.copy(status = GameStatus.WON, win = result, selected = -1)
        ProgressRepository.saveGame(null)
        AppScope.launch { FriendRepository.onPlayedToday() }
    }

    fun abandon() { ProgressRepository.saveGame(null) }

    private fun persist() {
        val s = state
        if (s.status == GameStatus.WON || s.status == GameStatus.LOADING) return
        ProgressRepository.saveGame(toJson(s))
    }

    private fun toJson(s: GameState): String = JSONObject().apply {
        put("d", s.difficulty.name)
        put("g", JSONArray(s.givens.toList())); put("s", JSONArray(s.solution.toList()))
        put("v", JSONArray(s.values.toList())); put("n", JSONArray(s.notes.toList()))
        put("m", s.mistakes); put("h", s.hintsLeft); put("t", s.seconds)
        put("daily", s.isDaily); put("day", s.dailyDay); put("sc", s.secondChanceUsed)
        put("lost", s.status == GameStatus.LOST)
    }.toString()

    private fun fromJson(str: String): GameState {
        val o = JSONObject(str)
        fun arr(k: String) = o.getJSONArray(k).let { a -> IntArray(81) { a.getInt(it) } }
        return GameState(
            difficulty = Difficulty.valueOf(o.getString("d")),
            givens = arr("g"), solution = arr("s"), values = arr("v"), notes = arr("n"),
            mistakes = o.getInt("m"), hintsLeft = o.getInt("h"), seconds = o.getInt("t"),
            isDaily = o.getBoolean("daily"), dailyDay = o.getLong("day"),
            secondChanceUsed = o.optBoolean("sc"),
            status = if (o.optBoolean("lost")) GameStatus.LOST else GameStatus.PLAYING,
        )
    }
}
