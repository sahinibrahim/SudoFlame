package com.sudoflame.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sudoflame.app.ads.AdsManager
import com.sudoflame.app.ads.BannerAd
import com.sudoflame.app.data.FriendRepository
import com.sudoflame.app.data.FriendState
import com.sudoflame.app.data.Levels
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.data.Quests
import com.sudoflame.app.data.Today
import com.sudoflame.app.game.Difficulty
import com.sudoflame.app.ui.AppCard
import com.sudoflame.app.ui.ChunkyButton
import com.sudoflame.app.ui.FlameIcon
import com.sudoflame.app.ui.ProgressBar
import com.sudoflame.app.ui.StatChip
import com.sudoflame.app.ui.WeekStrip
import com.sudoflame.app.ui.formatTime
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onPlay: (mode: String, difficulty: Difficulty) -> Unit,
    onDaily: () -> Unit,
    onFriends: () -> Unit,
) {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    val friends by FriendRepository.state.collectAsState()
    val activity = LocalActivity.current
    val ctx = LocalContext.current
    var hasSaved by remember { mutableStateOf(ProgressRepository.loadGame() != null) }
    var showDifficulty by remember { mutableStateOf(false) }
    var showFreezeInfo by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        ProgressRepository.refreshDay()
        hasSaved = ProgressRepository.loadGame() != null
    }

    var left by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val d = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
            left = "${d.toHours()} sa ${d.toMinutes() % 60} dk"
            delay(30_000)
        }
    }

    fun rewarded(onReward: () -> Unit) {
        val a = activity ?: return
        AdsManager.showRewarded(a, onReward) {
            Toast.makeText(ctx, "Reklam şu an hazır değil, birazdan tekrar dene.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)
            ) {
                // Başlık + sayaçlar
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sudo", fontSize = 26.sp, fontWeight = FontWeight.Black, color = c.text)
                    Text("Flame", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Brand.Flame)
                    Spacer(Modifier.weight(1f))
                    StatChip("🧊", "${p.freezes}", Brand.Sky) { showFreezeInfo = true }
                    Spacer(Modifier.width(8.dp))
                    StatChip("⚡", "Sv ${p.level}", Brand.Violet)
                }

                // Seri kartı
                val lit = p.playedToday
                AppCard(Modifier.fillMaxWidth(), brush = if (lit) Brand.sunsetBrush else Brand.violetBrush) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FlameIcon(64.dp, lit = lit || p.streak > 0)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("${p.streak}", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 48.sp)
                                Text("günlük seri", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("En iyi", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                                Text("🏆 ${p.bestStreak}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                lit -> "Harika! Bugünün alevi yandı. Yarın da gel, serin büyüsün! 🎉"
                                p.streak > 0 -> "Serini korumak için bugün 1 bulmaca çöz. Kalan süre: $left ⏳"
                                else -> "Bugün bir bulmaca çöz ve yeni serini başlat!"
                            },
                            color = Color.White, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))
                        WeekStrip(p)
                    }
                }

                // Söndürülen seriyi kurtar
                if (p.canRepairStreak) {
                    Spacer(Modifier.height(12.dp))
                    AppCard(Modifier.fillMaxWidth(), brush = Brand.iceBrush) {
                        Column(Modifier.padding(18.dp)) {
                            Text("😢 ${p.lostStreak} günlük serin söndü!", fontWeight = FontWeight.Black, color = Brand.Night, fontSize = 18.sp)
                            Text("Sadece bugün: kısa bir reklam izle, serini geri getir.", color = Brand.Night)
                            Spacer(Modifier.height(10.dp))
                            ChunkyButton("📺  Seriyi Kurtar", Modifier.fillMaxWidth(), color = Brand.Night, shadow = Color.Black) {
                                rewarded {
                                    ProgressRepository.repairStreak()
                                    Toast.makeText(ctx, "🔥 Serin geri geldi!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                // Devam et
                if (hasSaved) {
                    Spacer(Modifier.height(12.dp))
                    ChunkyButton("▶  OYUNA DEVAM ET", Modifier.fillMaxWidth(), color = Brand.Mint, shadow = Color(0xFF0B8F6E)) {
                        onPlay("resume", Difficulty.EASY)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val today = java.time.LocalDate.now()
                    val doneToday = Today.epochDay() in p.dailyDone
                    MiniCard(
                        Modifier.weight(1f), Brand.skyBrush, "📅", "GÜNÜN BULMACASI",
                        "${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.FULL, Locale("tr"))}",
                        if (doneToday) "Tamamlandı ⭐" else "Oyna", onDaily
                    )
                    val fCount = (friends as? FriendState.Ready)?.friends
                    val best = fCount?.maxOfOrNull { it.streak } ?: 0
                    MiniCard(
                        Modifier.weight(1f), Brand.mintBrush, "🤝", "ARKADAŞ SERİSİ",
                        if (fCount.isNullOrEmpty()) "Arkadaş ekle" else "${fCount.size} arkadaş · 🔥$best",
                        "Aç", onFriends
                    )
                }

                // Günlük görevler
                Spacer(Modifier.height(12.dp))
                val quests = Quests.forToday(p)
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯 Günlük Görevler", fontWeight = FontWeight.Black, fontSize = 18.sp, color = c.text)
                            Spacer(Modifier.weight(1f))
                            Text("+50 XP 🎁", color = Brand.Flame, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(10.dp))
                        ProgressBar(quests.count { it.done } / 3f, Modifier.fillMaxWidth(), track = c.line)
                        for (q in quests) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(q.emoji, fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(q.title, Modifier.weight(1f), color = c.text, fontWeight = FontWeight.SemiBold)
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(if (q.done) Brand.Mint else c.line),
                                    contentAlignment = Alignment.Center
                                ) { if (q.done) Text("✓", color = Color.White, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }

                // Seviye / lig
                Spacer(Modifier.height(12.dp))
                val (inLvl, need) = Levels.progressInLevel(p.xp)
                val (league, lEmoji) = Levels.league(p.level)
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lEmoji, fontSize = 36.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("$league Lig · Seviye ${p.level}", fontWeight = FontWeight.Black, color = c.text)
                            Spacer(Modifier.height(6.dp))
                            ProgressBar(inLvl / need.toFloat(), Modifier.fillMaxWidth(), brush = Brand.violetBrush, track = c.line)
                            Spacer(Modifier.height(4.dp))
                            Text("$inLvl / $need XP", color = c.subtext, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                ChunkyButton("YENİ OYUN", Modifier.fillMaxWidth(), height = 60.dp) { showDifficulty = true }
                Spacer(Modifier.height(20.dp))
            }
            BannerAd()
        }

        if (showDifficulty) {
            DifficultySheet(onDismiss = { showDifficulty = false }) { d ->
                showDifficulty = false
                onPlay("new", d)
            }
        }
        if (showFreezeInfo) {
            Overlay(onDismiss = { showFreezeInfo = false }) {
                Text("🧊", fontSize = 60.sp)
                Text("Seri Dondurucu", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.text)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bir gün oynamayı unutursan dondurucu otomatik devreye girer ve serin sönmez.\n\n" +
                        "Elinde: ${p.freezes}/3\nHer 7 günlük seride 1 tane hediye!",
                    color = c.subtext, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                if (p.freezes < 3) {
                    ChunkyButton("📺  Reklam izle, +1 dondurucu", Modifier.fillMaxWidth(), color = Brand.Sky, shadow = Color(0xFF2268C9)) {
                        rewarded {
                            ProgressRepository.addFreeze()
                            showFreezeInfo = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCard(modifier: Modifier, brush: Brush, emoji: String, label: String, title: String, action: String, onClick: () -> Unit) {
    AppCard(modifier.clickable(onClick = onClick), brush = brush) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 34.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            Text(
                action, color = Color.White, fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x40FFFFFF)).padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun Overlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99100828)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AppCard(Modifier.padding(24.dp).clickable(enabled = false) {}) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
    }
}

@Composable
private fun DifficultySheet(onDismiss: () -> Unit, onPick: (Difficulty) -> Unit) {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    Box(
        Modifier.fillMaxSize().background(Color(0x99100828)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(c.bg)
                .clickable(enabled = false) {}.padding(20.dp)
        ) {
            Text("Zorluk seç", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.text)
            Spacer(Modifier.height(12.dp))
            val brushes = listOf(Brand.mintBrush, Brand.sunsetBrush, Brand.skyBrush, Brand.violetBrush)
            Difficulty.entries.forEachIndexed { idx, d ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp))
                        .background(c.card).border(2.dp, c.line, RoundedCornerShape(20.dp))
                        .clickable { onPick(d) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(brushes[idx]), contentAlignment = Alignment.Center) {
                        Text(d.emoji, fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = c.text)
                        val best = p.bestTimeByDiff[d]
                        Text(
                            "+${d.xp} XP" + (best?.let { " · En iyi ${formatTime(it)}" } ?: "") +
                                " · ${p.winsByDiff[d] ?: 0} galibiyet",
                            color = c.subtext, fontSize = 13.sp
                        )
                    }
                    Text("›", fontSize = 28.sp, color = c.subtext)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
