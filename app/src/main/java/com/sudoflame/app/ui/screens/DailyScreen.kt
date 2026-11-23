package com.sudoflame.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudoflame.app.ads.BannerAd
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.data.Today
import com.sudoflame.app.game.SudokuGenerator
import com.sudoflame.app.ui.AppCard
import com.sudoflame.app.ui.ChunkyButton
import com.sudoflame.app.ui.ProgressBar
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val tr = Locale("tr")

@Composable
fun DailyScreen(onPlayDay: (Long) -> Unit) {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    val today = Today.epochDay()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableLongStateOf(today) }

    val days = month.lengthOfMonth()
    val doneInMonth = (1..days).count { month.atDay(it).toEpochDay() in p.dailyDone }
    val medal = when {
        doneInMonth >= days -> "👑"
        doneInMonth >= 20 -> "🥇"
        doneInMonth >= 10 -> "🥈"
        doneInMonth >= 1 -> "🥉"
        else -> "🎖️"
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Başlık: aylık rozet
            Box(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Brand.Violet, Brand.Night)))
                    .statusBarsPadding().padding(20.dp)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Günlük Mücadeleler", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.size(120.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0x66FFC93C), Color.Transparent))),
                        contentAlignment = Alignment.Center
                    ) { Text(medal, fontSize = 72.sp) }
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, tr).replaceFirstChar { it.uppercase() }} rozeti",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(doneInMonth / days.toFloat(), Modifier.fillMaxWidth(0.7f))
                    Spacer(Modifier.height(4.dp))
                    Text("⭐ $doneInMonth / $days", color = Brand.Sun, fontWeight = FontWeight.Black)
                }
            }

            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", fontSize = 34.sp, color = c.text, modifier = Modifier.clickable { month = month.minusMonths(1) }.padding(horizontal = 12.dp))
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, tr).replaceFirstChar { it.uppercase() }} ${month.year}",
                        Modifier.weight(1f), color = c.text, fontWeight = FontWeight.Black, fontSize = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    val canNext = month < YearMonth.now()
                    Text("›", fontSize = 34.sp, color = if (canNext) c.text else c.line,
                        modifier = Modifier.clickable(enabled = canNext) { month = month.plusMonths(1) }.padding(horizontal = 12.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (d in listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")) {
                        Text(d, Modifier.weight(1f), color = c.subtext, fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val offset = month.atDay(1).dayOfWeek.value - 1
                val cells = offset + days
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (k in 0 until 7) {
                            val dayNum = r * 7 + k - offset + 1
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                                if (dayNum in 1..days) {
                                    val ep = month.atDay(dayNum).toEpochDay()
                                    val done = ep in p.dailyDone
                                    val future = ep > today
                                    val isSel = ep == selected
                                    Box(
                                        Modifier.fillMaxSize().clip(CircleShape)
                                            .background(
                                                when {
                                                    done -> Brush.verticalGradient(listOf(Brand.Sun, Brand.Flame))
                                                    isSel -> Brush.linearGradient(listOf(Brand.Violet, Brand.Violet))
                                                    else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                }
                                            )
                                            .then(if (isSel && done) Modifier.border(3.dp, Brand.Violet, CircleShape) else Modifier)
                                            .clickable(enabled = !future) { selected = ep },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (done) "⭐" else "$dayNum",
                                            color = when { isSel -> Color.White; future -> c.line; else -> c.text },
                                            fontWeight = FontWeight.Bold, fontSize = if (done) 18.sp else 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                val date = LocalDate.ofEpochDay(selected)
                val diff = SudokuGenerator.dailyDifficulty(selected)
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(diff.emoji, fontSize = 32.sp)
                        Column(Modifier.weight(1f)) {
                            Text("${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, tr)} bulmacası", fontWeight = FontWeight.Black, color = c.text)
                            Text("${diff.title} · +${diff.xp + 25} XP · arkadaşlarınla aynı bulmaca!", color = c.subtext, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val doneSel = selected in p.dailyDone
                ChunkyButton(if (doneSel) "TEKRAR OYNA" else "OYNA", Modifier.fillMaxWidth()) { onPlayDay(selected) }
                Spacer(Modifier.height(16.dp))
            }
        }
        BannerAd()
    }
}
