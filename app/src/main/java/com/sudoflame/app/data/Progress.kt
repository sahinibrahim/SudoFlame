package com.sudoflame.app.data

import com.sudoflame.app.game.Difficulty

data class Progress(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastPlayDay: Long = -1,
    val freezes: Int = 1,
    val xp: Int = 0,
    val totalWins: Int = 0,
    val totalGames: Int = 0,
    val perfectWins: Int = 0,
    val winsByDiff: Map<Difficulty, Int> = emptyMap(),
    val bestTimeByDiff: Map<Difficulty, Int> = emptyMap(),
    val bestScore: Int = 0,
    val playedDays: Set<Long> = emptySet(),
    val frozenDays: Set<Long> = emptySet(),
    val dailyDone: Set<Long> = emptySet(),
    // Günlük görevler
    val questDay: Long = -1,
    val winsToday: Int = 0,
    val perfectToday: Boolean = false,
    val fastToday: Boolean = false,
    val questBonusClaimed: Boolean = false,
    // Kaybedilen seri (reklamla kurtarma)
    val lostStreak: Int = 0,
    val lostOnDay: Long = -1,
    // Ayarlar
    val playerName: String = "Oyuncu",
    val adsRemoved: Boolean = false,
    val vibrate: Boolean = true,
    val highlight: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val reminder: Boolean = true,
    val onboarded: Boolean = false,
) {
    val playedToday: Boolean get() = lastPlayDay == Today.epochDay()
    val level: Int get() = Levels.levelFor(xp)
    val canRepairStreak: Boolean get() = lostStreak > 1 && lostOnDay == Today.epochDay() && streak <= 1
}

object Today {
    /** Testlerde değiştirilebilir. */
    var override: Long? = null
    fun epochDay(): Long = override ?: java.time.LocalDate.now().toEpochDay()
}

object Levels {
    /** Seviye n'den n+1'e geçmek için 100*n XP gerekir. */
    fun levelFor(xp: Int): Int {
        var lvl = 1; var need = 100; var rest = xp
        while (rest >= need) { rest -= need; lvl++; need = 100 * lvl }
        return lvl
    }
    fun progressInLevel(xp: Int): Pair<Int, Int> {
        var lvl = 1; var need = 100; var rest = xp
        while (rest >= need) { rest -= need; lvl++; need = 100 * lvl }
        return rest to need
    }
    fun league(level: Int): Pair<String, String> = when {
        level >= 25 -> "Efsane" to "👑"
        level >= 15 -> "Elmas" to "💎"
        level >= 8 -> "Altın" to "🥇"
        level >= 4 -> "Gümüş" to "🥈"
        else -> "Bronz" to "🥉"
    }
}

data class WinResult(
    val xpGained: Int,
    val score: Int,
    val streakBefore: Int,
    val streakAfter: Int,
    val newBestTime: Boolean,
    val newAchievements: List<Achievement>,
    val questBonus: Boolean,
)

/** Seri kuralları — Android'den bağımsız, saf fonksiyonlar. */
object StreakLogic {

    /** Uygulama açılışında çağrılır: kaçırılan günler için dondurucu harcar ya da seriyi sıfırlar. */
    fun onNewDay(p: Progress, today: Long): Progress {
        var r = p
        if (r.questDay != today) {
            r = r.copy(questDay = today, winsToday = 0, perfectToday = false, fastToday = false, questBonusClaimed = false)
        }
        if (r.lastPlayDay < 0 || r.streak == 0) return r
        val missed = today - r.lastPlayDay - 1
        if (missed <= 0) return r
        return if (r.freezes >= missed) {
            val frozen = (r.lastPlayDay + 1 until today).toSet()
            // Dondurulan günleri "oynanmış" gibi say ki seri devam etsin
            r.copy(freezes = r.freezes - missed.toInt(), frozenDays = r.frozenDays + frozen, lastPlayDay = today - 1)
        } else {
            r.copy(lostStreak = r.streak, lostOnDay = today, streak = 0)
        }
    }

    /** Reklam izlenince kaybedilen seriyi geri getirir. */
    fun repair(p: Progress, today: Long): Progress {
        if (!p.canRepairStreak) return p
        val restored = if (p.lastPlayDay == today) p.lostStreak + 1 else p.lostStreak
        val last = if (p.lastPlayDay == today) today else today - 1
        return p.copy(streak = restored, bestStreak = maxOf(p.bestStreak, restored), lastPlayDay = last, lostStreak = 0, lostOnDay = -1)
    }

    fun onWin(
        p0: Progress, today: Long, difficulty: Difficulty, seconds: Int, mistakes: Int,
        isDaily: Boolean, dailyDay: Long,
    ): Pair<Progress, WinResult> {
        val p = onNewDay(p0, today)
        val before = Achievements.unlocked(p)
        val streakBefore = p.streak
        val streakAfter = when {
            p.lastPlayDay == today -> p.streak
            else -> p.streak + 1
        }
        val perfect = mistakes == 0
        val fast = seconds < 10 * 60
        val timeBonus = (1.0 + (900 - seconds).coerceAtLeast(0) / 900.0)
        val score = ((difficulty.baseScore * timeBonus) * (1.0 - 0.15 * mistakes)).toInt().coerceAtLeast(100)
        val prevBest = p.bestTimeByDiff[difficulty]
        val newBestTime = prevBest == null || seconds < prevBest
        var xp = difficulty.xp + (if (perfect) 10 else 0) + (if (isDaily) 25 else 0) + (streakAfter.coerceAtMost(30))

        var r = p.copy(
            streak = streakAfter,
            bestStreak = maxOf(p.bestStreak, streakAfter),
            lastPlayDay = today,
            totalWins = p.totalWins + 1,
            perfectWins = p.perfectWins + if (perfect) 1 else 0,
            winsByDiff = p.winsByDiff + (difficulty to (p.winsByDiff[difficulty] ?: 0) + 1),
            bestTimeByDiff = if (newBestTime) p.bestTimeByDiff + (difficulty to seconds) else p.bestTimeByDiff,
            bestScore = maxOf(p.bestScore, score),
            playedDays = (p.playedDays + today).filter { it > today - 400 }.toSet(),
            dailyDone = if (isDaily) p.dailyDone + dailyDay else p.dailyDone,
            winsToday = p.winsToday + 1,
            perfectToday = p.perfectToday || perfect,
            fastToday = p.fastToday || fast,
        )
        // Her 7 günlük seride bir dondurucu hediye (en fazla 3)
        if (streakAfter > streakBefore && streakAfter % 7 == 0) {
            r = r.copy(freezes = (r.freezes + 1).coerceAtMost(3))
        }
        val questsDone = Quests.forToday(r).all { it.done }
        val questBonus = questsDone && !r.questBonusClaimed
        if (questBonus) { xp += 50; r = r.copy(questBonusClaimed = true) }
        r = r.copy(xp = r.xp + xp)
        val newAch = Achievements.all.filter { it.id !in before && it.check(r) }
        return r to WinResult(xp, score, streakBefore, streakAfter, newBestTime && prevBest != null, newAch, questBonus)
    }
}

data class Quest(val emoji: String, val title: String, val done: Boolean)

object Quests {
    fun forToday(p: Progress): List<Quest> {
        val today = Today.epochDay()
        val fresh = p.questDay == today
        return listOf(
            Quest("🧩", "Bugün 1 bulmaca çöz", fresh && p.winsToday >= 1),
            Quest("🎯", "Hatasız bir oyun bitir", fresh && p.perfectToday),
            Quest("📅", "Günlük mücadeleyi tamamla", today in p.dailyDone),
        )
    }
}

data class Achievement(val id: String, val emoji: String, val title: String, val desc: String, val check: (Progress) -> Boolean)

object Achievements {
    val all = listOf(
        Achievement("first", "🐣", "İlk Adım", "İlk bulmacanı çöz") { it.totalWins >= 1 },
        Achievement("ten", "🧠", "Isınma Turu", "10 bulmaca çöz") { it.totalWins >= 10 },
        Achievement("fifty", "🏅", "Bulmaca Avcısı", "50 bulmaca çöz") { it.totalWins >= 50 },
        Achievement("hundred", "🏆", "Yüzler Kulübü", "100 bulmaca çöz") { it.totalWins >= 100 },
        Achievement("s3", "🔥", "Kıvılcım", "3 günlük seri") { it.bestStreak >= 3 },
        Achievement("s7", "🔥", "Haftalık Alev", "7 günlük seri") { it.bestStreak >= 7 },
        Achievement("s30", "🌋", "Volkan", "30 günlük seri") { it.bestStreak >= 30 },
        Achievement("s100", "☄️", "Söndürülemez", "100 günlük seri") { it.bestStreak >= 100 },
        Achievement("perfect", "🎯", "Kusursuz", "Hatasız bir oyun bitir") { it.perfectWins >= 1 },
        Achievement("perfect10", "🪄", "Keskin Nişancı", "10 hatasız oyun") { it.perfectWins >= 10 },
        Achievement("hard", "⚡", "Cesur", "Zor seviyede kazan") { (it.winsByDiff[Difficulty.HARD] ?: 0) >= 1 },
        Achievement("expert", "💎", "Usta", "Uzman seviyede kazan") { (it.winsByDiff[Difficulty.EXPERT] ?: 0) >= 1 },
        Achievement("speed", "⏱️", "Şimşek", "Kolay bulmacayı 3 dakikadan kısa sürede bitir") {
            (it.bestTimeByDiff[Difficulty.EASY] ?: Int.MAX_VALUE) < 180
        },
        Achievement("daily7", "📅", "Takvim Ustası", "7 günlük mücadele tamamla") { it.dailyDone.size >= 7 },
        Achievement("lvl10", "🚀", "Yükselen Yıldız", "10. seviyeye ulaş") { it.level >= 10 },
    )

    fun unlocked(p: Progress): Set<String> = all.filter { it.check(p) }.map { it.id }.toSet()
}
