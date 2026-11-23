package com.sudoflame.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sudoflame.app.ads.AdsManager
import com.sudoflame.app.ads.BannerAd
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.data.WinResult
import com.sudoflame.app.ui.AppCard
import com.sudoflame.app.ui.ChunkyButton
import com.sudoflame.app.ui.Confetti
import com.sudoflame.app.ui.FlameIcon
import com.sudoflame.app.ui.formatTime
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors

@Composable
fun GameScreen(vm: GameViewModel, onExit: () -> Unit, onNewGame: () -> Unit) {
    val s = vm.state
    val c = LocalAppColors.current
    val activity = LocalActivity.current
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val progress by ProgressRepository.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.setPaused(true) }
    var userPaused by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (!userPaused) vm.setPaused(false) }
    BackHandler { onExit() }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(s.lastWrong, s.mistakes) {
        if (s.lastWrong >= 0) {
            for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    fun rewarded(onReward: () -> Unit) {
        val a = activity ?: return
        AdsManager.showRewarded(a, onReward) {
            Toast.makeText(ctx, "Reklam şu an hazır değil, birazdan tekrar dene.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Üst çubuk: geri · canlı puan · durdur
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIcon("←") { onExit() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PUAN", color = c.subtext, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("${s.liveScore}", color = c.text, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
                RoundIcon(if (userPaused) "▶" else "❚❚") { userPaused = !userPaused; vm.setPaused(userPaused) }
            }
            // Bilgi satırı
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                InfoCol(
                    if (s.isDaily) "Günlük" else "Zorluk",
                    if (s.isDaily) java.time.LocalDate.ofEpochDay(s.dailyDay).let { "${it.dayOfMonth}.${it.monthValue}" } else "${s.difficulty.emoji} ${s.difficulty.title}",
                    Modifier.weight(1.3f)
                )
                InfoCol("Hatalar", "${s.mistakes}/${GameViewModel.MAX_MISTAKES}", Modifier.weight(1f),
                    if (s.mistakes > 0) Brand.Error else c.text)
                InfoCol("Süre", formatTime(s.seconds), Modifier.weight(1f))
                InfoCol("Seri", "🔥 ${progress.streak}", Modifier.weight(1f), Brand.Flame)
            }
            Spacer(Modifier.height(6.dp))

            if (s.status == GameStatus.LOADING) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand.Violet)
                }
            } else {
                Box(
                    Modifier.padding(horizontal = 10.dp).fillMaxWidth().aspectRatio(1f)
                        .offset(x = shake.value.dp)
                ) {
                    Board(s, progress.highlight, onSelect = vm::select)
                    if (userPaused) {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(c.card)
                                .clickable { userPaused = false; vm.setPaused(false) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("☕", fontSize = 56.sp)
                                Text("Mola verildi", fontWeight = FontWeight.Black, fontSize = 22.sp, color = c.text)
                                Text("Devam etmek için dokun", color = c.subtext)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                // Araçlar
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ToolButton("↩️", "Geri Al") { vm.undo() }
                    ToolButton("🧽", "Sil") { vm.erase() }
                    ToolButton("✏️", "Not", badge = if (s.noteMode) "AÇIK" else "KAPALI", active = s.noteMode) { vm.toggleNotes() }
                    ToolButton("💡", "İpucu", badge = if (s.hintsLeft > 0) "${s.hintsLeft}" else "📺") {
                        if (!vm.hint()) rewarded { vm.addHintFromAd(); vm.hint() }
                    }
                }
                Spacer(Modifier.height(14.dp))
                NumberPad(s) { n ->
                    val wrong = vm.input(n)
                    if (wrong && progress.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                Spacer(Modifier.weight(1f))
            }
            BannerAd()
        }

        if (s.status == GameStatus.LOST) {
            LostDialog(
                canSecondChance = !s.secondChanceUsed,
                onAd = { rewarded { vm.secondChance() } },
                onNew = { vm.abandon(); onNewGame() },
                onHome = { vm.abandon(); onExit() },
            )
        }
        val win = s.win
        if (s.status == GameStatus.WON && win != null) {
            WinOverlay(s, win, onContinue = {
                val a = activity
                if (a != null) AdsManager.maybeShowInterstitial(a) { onExit() } else onExit()
            }, onShare = {
                val text = "🔥 SudoFlame'de ${s.difficulty.title} bulmacayı ${formatTime(s.seconds)} içinde çözdüm! " +
                    "Serim: ${win.streakAfter} gün. Beni geçebilir misin? " +
                    "https://play.google.com/store/apps/details?id=${ctx.packageName}"
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Paylaş"))
            })
        }
    }
}

@Composable
private fun RoundIcon(text: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(c.card).border(2.dp, c.line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 18.sp, color = c.text, fontWeight = FontWeight.Black) }
}

@Composable
private fun InfoCol(label: String, value: String, modifier: Modifier, color: Color = LocalAppColors.current.text) {
    Column(modifier) {
        Text(label, color = LocalAppColors.current.subtext, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun Board(s: GameState, highlight: Boolean, onSelect: (Int) -> Unit) {
    val c = LocalAppColors.current
    val sel = s.selected
    val selVal = if (sel >= 0) s.values[sel] else 0
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(c.boardBg)
            .border(3.dp, c.text.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until 9) {
                        val i = r * 9 + col
                        val related = highlight && sel >= 0 && (sel / 9 == r || sel % 9 == col ||
                            (sel / 27 == r / 3 && (sel % 9) / 3 == col / 3))
                        val same = highlight && selVal != 0 && s.values[i] == selVal
                        val bg = when {
                            i == sel -> c.cellSelected
                            s.isWrong(i) -> Brand.Error.copy(alpha = 0.15f)
                            same -> c.cellSame
                            related -> c.cellRelated
                            else -> Color.Transparent
                        }
                        Cell(s, i, bg, Modifier.weight(1f).fillMaxSize()) { onSelect(i) }
                    }
                }
            }
        }
        // Izgara çizgileri
        val thin = c.line; val thick = c.text.copy(alpha = 0.85f)
        Canvas(Modifier.fillMaxSize()) {
            val step = size.width / 9f
            for (k in 1 until 9) {
                val w = if (k % 3 == 0) 2.5.dp.toPx() else 1.dp.toPx()
                val col = if (k % 3 == 0) thick else thin
                drawLine(col, Offset(k * step, 0f), Offset(k * step, size.height), w)
                drawLine(col, Offset(0f, k * step), Offset(size.width, k * step), w)
            }
        }
    }
}

@Composable
private fun Cell(s: GameState, i: Int, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val v = s.values[i]
    val scale = remember { Animatable(1f) }
    LaunchedEffect(v, s.lastPlaced) {
        if (s.lastPlaced == i && v != 0) {
            scale.snapTo(1.45f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Box(modifier.background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (v != 0) {
            val color = when {
                s.givens[i] != 0 -> c.given
                s.isWrong(i) -> Brand.Error
                else -> c.entered
            }
            Text(
                "$v", color = color, fontSize = 24.sp,
                fontWeight = if (s.givens[i] != 0) FontWeight.Bold else FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
        } else if (s.notes[i] != 0) {
            Column(Modifier.fillMaxSize().padding(1.dp)) {
                for (r in 0 until 3) Row(Modifier.weight(1f)) {
                    for (cc in 0 until 3) {
                        val n = r * 3 + cc + 1
                        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (s.notes[i] and (1 shl n) != 0)
                                Text("$n", fontSize = 9.sp, color = c.subtext, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(emoji: String, label: String, badge: String? = null, active: Boolean = false, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (active) Brand.Violet.copy(alpha = 0.18f) else c.card)
                    .border(2.dp, if (active) Brand.Violet else c.line, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 24.sp) }
            if (badge != null) {
                Text(
                    badge, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp)
                        .clip(RoundedCornerShape(50)).background(if (active) Brand.Violet else Brand.Flame)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = c.subtext, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NumberPad(s: GameState, onNumber: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        for (n in 1..9) {
            val left = 9 - s.placedCount(n)
            Column(
                Modifier.weight(1f).padding(horizontal = 2.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (left > 0) c.card else Color.Transparent)
                    .then(if (left > 0) Modifier.border(2.dp, c.line, RoundedCornerShape(14.dp)).clickable { onNumber(n) } else Modifier)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (left > 0) {
                    Text("$n", fontSize = 28.sp, fontWeight = FontWeight.Black,
                        color = if (s.noteMode) c.subtext else Brand.Violet)
                    Text("$left", fontSize = 11.sp, color = c.subtext, fontWeight = FontWeight.Bold)
                } else {
                    Text("✓", fontSize = 22.sp, color = Brand.Mint, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun LostDialog(canSecondChance: Boolean, onAd: () -> Unit, onNew: () -> Unit, onHome: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xAA0E0620)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        AppCard(Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💔", fontSize = 64.sp)
                Text("Canların bitti!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = LocalAppColors.current.text)
                Spacer(Modifier.height(6.dp))
                Text("Pes etme, çok yaklaşmıştın.", color = LocalAppColors.current.subtext, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                if (canSecondChance) {
                    ChunkyButton("📺  Reklam izle, +1 can", Modifier.fillMaxWidth(), color = Brand.Flame, shadow = Color(0xFFC9520A), onClick = onAd)
                    Spacer(Modifier.height(10.dp))
                }
                ChunkyButton("Yeni Oyun", Modifier.fillMaxWidth(), onClick = onNew)
                Spacer(Modifier.height(10.dp))
                Text("Ana sayfaya dön", color = Brand.Violet, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable(onClick = onHome).padding(8.dp))
            }
        }
    }
}

@Composable
private fun WinOverlay(s: GameState, w: WinResult, onContinue: () -> Unit, onShare: () -> Unit) {
    val streakAnim = remember { Animatable(w.streakBefore.toFloat()) }
    LaunchedEffect(Unit) { streakAnim.animateTo(w.streakAfter.toFloat(), tween(900, delayMillis = 500)) }
    val flameIn = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(Modifier.fillMaxSize().background(Brand.violetBrush)) {
        Confetti()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(flameIn, enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn()) {
                Box(contentAlignment = Alignment.Center) { FlameIcon(130.dp) }
            }
            Text("${streakAnim.value.toInt()}", fontSize = 72.sp, fontWeight = FontWeight.Black, color = Brand.Sun)
            Text(
                if (w.streakAfter > w.streakBefore) "günlük seri! Alev büyüyor 🔥" else "günlük seri — bugün zaten yaktın!",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WinStat("⚡", "+${w.xpGained} XP")
                WinStat("⏱", formatTime(s.seconds))
                WinStat("🏆", "${w.score}")
            }
            if (w.newBestTime) {
                Spacer(Modifier.height(12.dp))
                Text("🎉 Yeni rekor süre!", color = Brand.Sun, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            if (w.questBonus) {
                Spacer(Modifier.height(8.dp))
                Text("✅ Tüm günlük görevler bitti: +50 XP", color = Brand.Mint, fontWeight = FontWeight.Black)
            }
            for (a in w.newAchievements) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(a.emoji, fontSize = 26.sp); Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Yeni başarı!", color = Brand.Sun, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(a.title, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            ChunkyButton("DEVAM", Modifier.fillMaxWidth(), color = Brand.Sun, shadow = Color(0xFFD99A00), textColor = Brand.Night, onClick = onContinue)
            Spacer(Modifier.height(12.dp))
            Text("📣 Arkadaşına meydan oku", color = Color.White, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable(onClick = onShare).padding(10.dp))
        }
    }
}

@Composable
private fun WinStat(emoji: String, value: String) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0x26FFFFFF)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}
