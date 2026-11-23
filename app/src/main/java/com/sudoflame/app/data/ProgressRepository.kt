package com.sudoflame.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sudoflame.app.game.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProgressRepository {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(Progress())
    val state: StateFlow<Progress> = _state.asStateFlow()
    val current: Progress get() = _state.value

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
        _state.value = load()
        refreshDay()
    }

    /** Açılışta ve uygulama öne geldiğinde: gün değiştiyse seriyi kontrol et. */
    fun refreshDay() = update { StreakLogic.onNewDay(it, Today.epochDay()) }

    fun update(block: (Progress) -> Progress) {
        val n = block(_state.value)
        if (n != _state.value) { _state.value = n; save(n) }
    }

    fun recordWin(d: Difficulty, seconds: Int, mistakes: Int, isDaily: Boolean, dailyDay: Long): WinResult {
        val (p, result) = StreakLogic.onWin(_state.value, Today.epochDay(), d, seconds, mistakes, isDaily, dailyDay)
        _state.value = p; save(p)
        return result
    }

    fun recordGameStarted() = update { it.copy(totalGames = it.totalGames + 1) }
    fun repairStreak() = update { StreakLogic.repair(it, Today.epochDay()) }
    fun addFreeze() = update { it.copy(freezes = (it.freezes + 1).coerceAtMost(3)) }

    // --- Kayıtlı oyun (devam et) ---
    fun saveGame(json: String?) = prefs.edit { if (json == null) remove("saved_game") else putString("saved_game", json) }
    fun loadGame(): String? = prefs.getString("saved_game", null)

    private fun Set<Long>.enc() = map { it.toString() }.toSet()
    private fun Set<String>?.dec() = this?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    private fun Map<Difficulty, Int>.enc() = entries.joinToString(",") { "${it.key.name}:${it.value}" }
    private fun String?.decMap(): Map<Difficulty, Int> = this.orEmpty().split(",").mapNotNull {
        val parts = it.split(":")
        if (parts.size != 2) null else runCatching { Difficulty.valueOf(parts[0]) to parts[1].toInt() }.getOrNull()
    }.toMap()

    private fun load(): Progress = with(prefs) {
        Progress(
            streak = getInt("streak", 0),
            bestStreak = getInt("bestStreak", 0),
            lastPlayDay = getLong("lastPlayDay", -1),
            freezes = getInt("freezes", 1),
            xp = getInt("xp", 0),
            totalWins = getInt("totalWins", 0),
            totalGames = getInt("totalGames", 0),
            perfectWins = getInt("perfectWins", 0),
            winsByDiff = getString("winsByDiff", null).decMap(),
            bestTimeByDiff = getString("bestTimeByDiff", null).decMap(),
            bestScore = getInt("bestScore", 0),
            playedDays = getStringSet("playedDays", null).dec(),
            frozenDays = getStringSet("frozenDays", null).dec(),
            dailyDone = getStringSet("dailyDone", null).dec(),
            questDay = getLong("questDay", -1),
            winsToday = getInt("winsToday", 0),
            perfectToday = getBoolean("perfectToday", false),
            fastToday = getBoolean("fastToday", false),
            questBonusClaimed = getBoolean("questBonusClaimed", false),
            lostStreak = getInt("lostStreak", 0),
            lostOnDay = getLong("lostOnDay", -1),
            playerName = getString("playerName", "Oyuncu") ?: "Oyuncu",
            adsRemoved = getBoolean("adsRemoved", false),
            vibrate = getBoolean("vibrate", true),
            highlight = getBoolean("highlight", true),
            autoRemoveNotes = getBoolean("autoRemoveNotes", true),
            reminder = getBoolean("reminder", true),
            onboarded = getBoolean("onboarded", false),
        )
    }

    private fun save(p: Progress) = prefs.edit {
        putInt("streak", p.streak); putInt("bestStreak", p.bestStreak); putLong("lastPlayDay", p.lastPlayDay)
        putInt("freezes", p.freezes); putInt("xp", p.xp); putInt("totalWins", p.totalWins)
        putInt("totalGames", p.totalGames); putInt("perfectWins", p.perfectWins)
        putString("winsByDiff", p.winsByDiff.enc()); putString("bestTimeByDiff", p.bestTimeByDiff.enc())
        putInt("bestScore", p.bestScore)
        putStringSet("playedDays", p.playedDays.enc()); putStringSet("frozenDays", p.frozenDays.enc())
        putStringSet("dailyDone", p.dailyDone.enc())
        putLong("questDay", p.questDay); putInt("winsToday", p.winsToday); putBoolean("perfectToday", p.perfectToday)
        putBoolean("fastToday", p.fastToday); putBoolean("questBonusClaimed", p.questBonusClaimed)
        putInt("lostStreak", p.lostStreak); putLong("lostOnDay", p.lostOnDay)
        putString("playerName", p.playerName); putBoolean("adsRemoved", p.adsRemoved)
        putBoolean("vibrate", p.vibrate); putBoolean("highlight", p.highlight)
        putBoolean("autoRemoveNotes", p.autoRemoveNotes); putBoolean("reminder", p.reminder)
        putBoolean("onboarded", p.onboarded)
    }
}
