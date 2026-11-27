package com.sudoflame.app.ui

import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudoflame.app.data.Progress
import com.sudoflame.app.data.Today
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Basınca aşağı çöken, alt gölgeli "tok" buton. */
@Composable
fun ChunkyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Brand.Violet,
    shadow: Color = Brand.VioletDark,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val drop by animateFloatAsState(if (pressed) 4f else 0f, tween(60), label = "drop")
    val c = if (enabled) color else Color(0xFFCFC8DE)
    val s = if (enabled) shadow else Color(0xFFB3ABC6)
    Box(
        modifier
            .height(height + 5.dp)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = 5.dp)
                .clip(RoundedCornerShape(18.dp)).background(s)
        )
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = drop.dp)
                .clip(RoundedCornerShape(18.dp)).background(c),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, brush: Brush? = null, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(c.card))
            .then(if (brush == null) Modifier.border(2.dp, c.line, RoundedCornerShape(24.dp)) else Modifier)
    ) { content() }
}

/** Nefes alan alev rozeti. */
@Composable
fun FlameIcon(size: Dp, lit: Boolean = true, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "flame")
    val s by t.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s"
    )
    Text(
        "🔥", fontSize = (size.value * 0.8f).sp,
        modifier = modifier.scale(if (lit) s else 1f).graphicsLayer { alpha = if (lit) 1f else 0.35f }
    )
}

@Composable
fun StatChip(emoji: String, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = LocalAppColors.current
    Row(
        modifier.clip(RoundedCornerShape(50)).background(c.card).border(2.dp, c.line, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = tint, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

private val dayNames = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")

/** Bu haftanın günleri: alev (oynandı), buz (donduruldu), boş. */
@Composable
fun WeekStrip(p: Progress, light: Boolean = true) {
    val today = Today.epochDay()
    val dow = java.time.LocalDate.ofEpochDay(today).dayOfWeek.value // 1 = Pazartesi
    val monday = today - (dow - 1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val day = monday + i
            val played = day in p.playedDays
            val frozen = day in p.frozenDays
            val isToday = day == today
            val txt = if (light) Color.White else LocalAppColors.current.text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayNames[i], color = txt.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(
                            when {
                                played -> Brush.verticalGradient(listOf(Brand.Sun, Brand.Flame))
                                frozen -> Brand.iceBrush
                                else -> Brush.linearGradient(listOf(txt.copy(alpha = 0.15f), txt.copy(alpha = 0.15f)))
                            }
                        )
                        .then(if (isToday) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when { played -> "🔥"; frozen -> "🧊"; day > today -> ""; isToday -> "•"; else -> "" },
                        fontSize = 15.sp, color = txt
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, brush: Brush = Brand.flameBrush, track: Color = Color(0x33FFFFFF)) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "pb")
    Box(modifier.height(12.dp).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(f).height(12.dp).clip(RoundedCornerShape(50)).background(brush))
    }
}

/** Kazanma ekranındaki konfeti yağmuru. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = Random(42)
        List(90) {
            floatArrayOf(r.nextFloat(), r.nextFloat() * -1f, 0.4f + r.nextFloat() * 0.8f, r.nextFloat() * 360f, r.nextInt(6).toFloat())
        }
    }
    val colors = listOf(Brand.Flame, Brand.Sun, Brand.Violet, Brand.Mint, Brand.Sky, Brand.FlameDeep)
    val t = rememberInfiniteTransition(label = "confetti")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "t")
    Canvas(modifier.fillMaxSize()) {
        for (p in pieces) {
            val y = ((p[1] + time * p[2] * 1.6f) % 1.2f) * size.height
            val x = (p[0] + 0.04f * sin((time * 12f + p[3]).toDouble()).toFloat()) * size.width
            val a = Math.toRadians((p[3] + time * 720f).toDouble())
            val w = 18f * cos(a).toFloat().let { if (kotlin.math.abs(it) < 0.2f) 0.2f else it }
            drawRect(colors[p[4].toInt()], topLeft = Offset(x, y), size = Size(kotlin.math.abs(w), 26f))
        }
    }
}

/** Degrade zeminli, yuvarlatılmış kare içinde beyaz vektör ikon. */
@Composable
fun IconBubble(icon: ImageVector, brush: Brush, size: Dp = 52.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(brush), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.52f))
    }
}

fun formatTime(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
