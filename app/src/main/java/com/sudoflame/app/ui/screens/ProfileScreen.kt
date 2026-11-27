package com.sudoflame.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.sudoflame.app.ui.IconBubble
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Restore
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudoflame.app.BuildConfig
import com.sudoflame.app.ads.AdsManager
import com.sudoflame.app.billing.BillingManager
import com.sudoflame.app.data.Achievements
import com.sudoflame.app.data.AppLinks
import com.sudoflame.app.data.AppLinks.PRIVACY_URL
import com.sudoflame.app.data.FriendRepository
import com.sudoflame.app.data.Levels
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.game.Difficulty
import com.sudoflame.app.ui.AppCard
import com.sudoflame.app.ui.ChunkyButton
import com.sudoflame.app.ui.ProgressBar
import com.sudoflame.app.ui.formatTime
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

private enum class Sub { NONE, ACHIEVEMENTS, STATS, SETTINGS, HOWTO, RULES, ABOUT }

@Composable
fun ProfileScreen() {
    var sub by remember { mutableStateOf(Sub.NONE) }
    BackHandler(enabled = sub != Sub.NONE) { sub = Sub.NONE }
    when (sub) {
        Sub.NONE -> ProfileMain { sub = it }
        Sub.ACHIEVEMENTS -> SubPage("Başarılar", { sub = Sub.NONE }) { AchievementsPage() }
        Sub.STATS -> SubPage("İstatistikler", { sub = Sub.NONE }) { StatsPage() }
        Sub.SETTINGS -> SubPage("Ayarlar", { sub = Sub.NONE }) { SettingsPage() }
        Sub.HOWTO -> SubPage("Nasıl Oynanır", { sub = Sub.NONE }) { TextPage(howTo) }
        Sub.RULES -> SubPage("Kurallar", { sub = Sub.NONE }) { TextPage(rules) }
        Sub.ABOUT -> SubPage("Hakkında", { sub = Sub.NONE }) { TextPage(about) }
    }
}

@Composable
private fun ProfileMain(open: (Sub) -> Unit) {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    val price by BillingManager.price.collectAsState()
    val activity = LocalActivity.current
    val ctx = LocalContext.current
    val (league, lEmoji) = Levels.league(p.level)
    val (inLvl, need) = Levels.progressInLevel(p.xp)

    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp)) {
        // Profil başlığı
        AppCard(Modifier.fillMaxWidth(), brush = Brand.violetBrush) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Brand.flameBrush), contentAlignment = Alignment.Center) {
                    Text(p.playerName.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.playerName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("$lEmoji $league Lig · Seviye ${p.level}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(inLvl / need.toFloat(), Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigTile(Modifier.weight(1f), "🔥", "${p.streak}", "Seri")
            BigTile(Modifier.weight(1f), "🏆", "${p.totalWins}", "Galibiyet")
            BigTile(Modifier.weight(1f), "⭐", "${p.dailyDone.size}", "Günlük")
        }
        Spacer(Modifier.height(12.dp))
        val unlocked = Achievements.unlocked(p).size
        MenuGroup {
            MenuItem(Icons.Rounded.EmojiEvents, Brand.sunsetBrush, "Başarılar", "$unlocked/${Achievements.all.size}") { open(Sub.ACHIEVEMENTS) }
            MenuItem(Icons.Rounded.BarChart, Brand.skyBrush, "İstatistikler") { open(Sub.STATS) }
        }
        Spacer(Modifier.height(12.dp))
        MenuGroup {
            MenuItem(Icons.Rounded.Settings, Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFE63946))), "Ayarlar") { open(Sub.SETTINGS) }
            MenuItem(Icons.Rounded.School, Brand.sunsetBrush, "Nasıl Oynanır") { open(Sub.HOWTO) }
            MenuItem(Icons.AutoMirrored.Rounded.MenuBook, Brand.mintBrush, "Kurallar") { open(Sub.RULES) }
        }
        Spacer(Modifier.height(12.dp))
        MenuGroup {
            MenuItem(Icons.Rounded.Lock, Brand.violetBrush, "Gizlilik Politikası") {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
            }
            if (AdsManager.privacyOptionsRequired) {
                MenuItem(Icons.Rounded.Shield, Brand.skyBrush, "Gizlilik Tercihleri") { activity?.let { AdsManager.showPrivacyOptions(it) } }
            }
            MenuItem(Icons.Rounded.Star, Brand.sunsetBrush, "Bizi Değerlendir") {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.store(ctx))))
            }
            MenuItem(Icons.Rounded.Info, Brand.skyBrush, "Hakkında") { open(Sub.ABOUT) }
        }
        Spacer(Modifier.height(12.dp))
        if (!p.adsRemoved) {
            MenuGroup {
                MenuItem(Icons.Rounded.Block, Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFC81D4E))), "Reklamları Kaldır", price ?: "") {
                    val a = activity ?: return@MenuItem
                    if (!BillingManager.buyRemoveAds(a)) {
                        Toast.makeText(ctx, "Mağaza şu an kullanılamıyor. Biraz sonra tekrar dene.", Toast.LENGTH_SHORT).show()
                    }
                }
                MenuItem(Icons.Rounded.Restore, Brand.mintBrush, "Satın Alımı Geri Yükle") {
                    BillingManager.restore()
                    Toast.makeText(ctx, "Kontrol ediliyor…", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Text("💜 Reklamsız sürümü kullanıyorsun. Teşekkürler!", color = Brand.Violet, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BigTile(modifier: Modifier, emoji: String, value: String, label: String) {
    val c = LocalAppColors.current
    AppCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, color = c.text)
            Text(label, color = c.subtext, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) { Column { content() } }
}

@Composable
private fun MenuItem(icon: ImageVector, brush: Brush, title: String, trailing: String = "", onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon, brush, 42.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.text)
        if (trailing.isNotEmpty()) Text(trailing, color = c.subtext, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.subtext, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(c.card).border(2.dp, c.line, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = c.text, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.text)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AchievementsPage() {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    for (a in Achievements.all) {
        val ok = a.check(p)
        AppCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (ok) Brand.flameBrush else Brush.linearGradient(listOf(c.line, c.line))),
                    contentAlignment = Alignment.Center
                ) { Text(if (ok) a.emoji else "🔒", fontSize = 26.sp, modifier = Modifier.graphicsLayer { alpha = if (ok) 1f else 0.6f }) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.title, fontWeight = FontWeight.Black, color = if (ok) c.text else c.subtext, fontSize = 17.sp)
                    Text(a.desc, color = c.subtext, fontSize = 13.sp)
                }
                if (ok) Text("✓", color = Brand.Mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StatsPage() {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    val rate = if (p.totalGames > 0) (100 * p.totalWins / p.totalGames).coerceAtMost(100) else 0
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            StatLine("Oynanan oyun", "${p.totalGames}")
            StatLine("Kazanılan", "${p.totalWins}")
            StatLine("Kazanma oranı", "%$rate")
            StatLine("Hatasız oyun", "${p.perfectWins}")
            StatLine("En yüksek puan", "${p.bestScore}")
            StatLine("Mevcut seri", "🔥 ${p.streak}")
            StatLine("En uzun seri", "🏆 ${p.bestStreak}")
            StatLine("Toplam XP", "⚡ ${p.xp}")
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Zorluğa göre", fontWeight = FontWeight.Black, color = c.text, fontSize = 18.sp)
    Spacer(Modifier.height(6.dp))
    for (d in Difficulty.entries) {
        AppCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(d.emoji, fontSize = 24.sp); Spacer(Modifier.width(10.dp))
                Text(d.title, Modifier.weight(1f), fontWeight = FontWeight.Black, color = c.text)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${p.winsByDiff[d] ?: 0} galibiyet", color = c.text, fontWeight = FontWeight.Bold)
                    Text("En iyi: ${p.bestTimeByDiff[d]?.let { formatTime(it) } ?: "—"}", color = c.subtext, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, Modifier.weight(1f), color = c.subtext, fontWeight = FontWeight.SemiBold)
        Text(value, color = c.text, fontWeight = FontWeight.Black)
    }
    HorizontalDivider(color = c.line)
}

@Composable
private fun SettingsPage() {
    val c = LocalAppColors.current
    val p by ProgressRepository.state.collectAsState()
    var name by remember { mutableStateOf(p.playerName) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Oyuncu adın", fontWeight = FontWeight.Black, color = c.text)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(name, { name = it.take(16) }, Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            ChunkyButton("Kaydet", Modifier.fillMaxWidth(), height = 46.dp, enabled = name.isNotBlank() && name != p.playerName) {
                ProgressRepository.update { it.copy(playerName = name.trim()) }
                scope.launch { FriendRepository.pushProfile() }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Toggle("📳 Hatada titreşim", p.vibrate) { v -> ProgressRepository.update { it.copy(vibrate = v) } }
            Toggle("🔦 Satır/sütun vurgula", p.highlight) { v -> ProgressRepository.update { it.copy(highlight = v) } }
            Toggle("🧹 Notları otomatik temizle", p.autoRemoveNotes) { v -> ProgressRepository.update { it.copy(autoRemoveNotes = v) } }
            Toggle("🔔 Akşam seri hatırlatıcısı", p.reminder) { v -> ProgressRepository.update { it.copy(reminder = v) } }
        }
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = c.text, fontWeight = FontWeight.SemiBold)
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Brand.Violet))
    }
}

@Composable
private fun TextPage(items: List<Pair<String, String>>) {
    val c = LocalAppColors.current
    for ((title, body) in items) {
        AppCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Black, color = c.text, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(body, color = c.subtext)
            }
        }
    }
}

private val rules = listOf(
    "🎯 Amaç" to "9×9'luk tabloyu öyle doldur ki her satırda, her sütunda ve her 3×3'lük kutuda 1'den 9'a kadar her rakam tam bir kez olsun.",
    "🔢 Başlangıç rakamları" to "Koyu renkli rakamlar bulmacanın parçasıdır, değiştirilemez.",
    "❤️ Canlar" to "Her bulmacada 3 canın var. Yanlış rakam bir can götürür. Canlar bitince bir kez reklam izleyerek ikinci şans alabilirsin.",
    "✅ Tek çözüm" to "Tüm SudoFlame bulmacalarının tek bir doğru çözümü vardır. Tahmine gerek yok, mantık yeter!",
)

private val howTo = listOf(
    "1⃣ Hücre seç" to "Doldurmak istediğin boş kareye dokun. Aynı satır, sütun ve kutu vurgulanır.",
    "2⃣ Rakam gir" to "Alttaki tuşlardan bir rakama dokun. Tuşun altındaki küçük sayı o rakamdan kaç tane kaldığını gösterir.",
    "✏️ Notlar" to "Emin değilsen Not modunu aç ve olası rakamları küçük not olarak yaz. Doğru rakam girince ilgili notlar kendiliğinden silinir.",
    "💡 İpucu" to "Her oyunda 2 ücretsiz ipucun var. Bitince kısa bir reklamla yeni ipucu kazanabilirsin.",
    "🔥 Seri" to "Her gün en az 1 bulmaca çöz, alevin büyüsün. Bir gün kaçırırsan Seri Dondurucu 🧊 seni korur.",
    "🤝 Arkadaş serisi" to "Arkadaşlar sekmesinden kodunu paylaş. İkiniz de aynı gün oynarsanız ortak seriniz artar.",
    "📅 Günlük mücadele" to "Her gün herkes aynı bulmacayı çözer. Tamamladığın günler takvimde yıldız olur, ayın rozetini topla!",
    "🧩 Strateji" to "Önce en dolu satır, sütun ya da kutuya bak. Bir hücreye yalnızca tek rakam sığıyorsa onu yaz.",
)

private val about = listOf(
    "SudoFlame" to "Sürüm ${BuildConfig.VERSION_NAME}\nHer gün biraz beyin egzersizi, her gün biraz daha büyük bir alev. 🔥",
    "İletişim" to "Öneri ve hatalar için: ${AppLinks.SUPPORT_EMAIL}",
)
