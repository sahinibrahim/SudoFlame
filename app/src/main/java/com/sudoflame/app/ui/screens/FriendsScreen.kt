package com.sudoflame.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudoflame.app.ads.BannerAd
import com.sudoflame.app.data.FriendRepository
import com.sudoflame.app.data.FriendState
import com.sudoflame.app.data.FriendStreak
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.ui.AppCard
import com.sudoflame.app.ui.ChunkyButton
import com.sudoflame.app.ui.FlameIcon
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen() {
    val c = LocalAppColors.current
    val state by FriendRepository.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<FriendStreak?>(null) }

    LaunchedEffect(Unit) { if (state !is FriendState.Ready) FriendRepository.connect() }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp)) {
                Text("Arkadaş Serileri", fontSize = 26.sp, fontWeight = FontWeight.Black, color = c.text)
                Text(
                    "İkiniz de aynı gün bir bulmaca çözerseniz ortak aleviniz büyür. Biri unutursa ikiniz de kaybedersiniz!",
                    color = c.subtext
                )
                Spacer(Modifier.height(16.dp))

                when (val s = state) {
                    FriendState.NotConfigured -> AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛠️", fontSize = 48.sp)
                            Text("Arkadaş özelliği yakında!", fontWeight = FontWeight.Black, color = c.text, fontSize = 18.sp)
                            Text(
                                "(Geliştirici notu: Firebase google-services.json dosyasını app/ klasörüne ekleyin.)",
                                color = c.subtext, textAlign = TextAlign.Center, fontSize = 13.sp
                            )
                        }
                    }
                    FriendState.Loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Brand.Violet)
                    }
                    is FriendState.Error -> AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📡", fontSize = 48.sp)
                            Text(s.message, color = c.text, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            ChunkyButton("Tekrar Dene", Modifier.fillMaxWidth()) { scope.launch { FriendRepository.connect() } }
                        }
                    }
                    is FriendState.Ready -> {
                        // Benim kodum
                        AppCard(Modifier.fillMaxWidth(), brush = Brand.mintBrush) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SENİN ARKADAŞ KODUN", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text(s.myCode, color = Color.White, fontWeight = FontWeight.Black, fontSize = 40.sp, letterSpacing = 6.sp,
                                    modifier = Modifier.clickable { copy(ctx, s.myCode) })
                                Spacer(Modifier.height(10.dp))
                                ChunkyButton("📨  Arkadaşını Davet Et", Modifier.fillMaxWidth(), color = Color.White, shadow = Color(0xFFBFEFE2), textColor = Color(0xFF0B8F6E)) {
                                    invite(ctx, s.myCode)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        // Kod ekle
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Arkadaş ekle", fontWeight = FontWeight.Black, color = c.text, fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = code, onValueChange = { code = it.uppercase().take(8) },
                                        modifier = Modifier.weight(1f), singleLine = true,
                                        placeholder = { Text("Örn: K7P2QX") },
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    ChunkyButton(if (busy) "…" else "Ekle", Modifier.width(90.dp), enabled = !busy && code.length >= 6) {
                                        busy = true
                                        scope.launch {
                                            val err = FriendRepository.addFriend(code)
                                            busy = false
                                            if (err == null) { code = ""; Toast.makeText(ctx, "🎉 Arkadaş eklendi! Yarından itibaren ortak serinizi başlatın.", Toast.LENGTH_LONG).show() }
                                            else Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (s.friends.isEmpty()) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👯", fontSize = 56.sp)
                                Text("Henüz arkadaşın yok", fontWeight = FontWeight.Black, color = c.text)
                                Text("Kodunu paylaş, birlikte alev yakın!", color = c.subtext)
                            }
                        }
                        for (f in s.friends) {
                            FriendRow(f, onNudge = { nudge(ctx, f) }, onLongPress = { removeTarget = f })
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
            BannerAd()
        }
        removeTarget?.let { f ->
            Overlay(onDismiss = { removeTarget = null }) {
                Text("${f.friendName} arkadaşlıktan çıkarılsın mı?", fontWeight = FontWeight.Black, color = c.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                ChunkyButton("Çıkar", Modifier.fillMaxWidth(), color = Brand.Error, shadow = Color(0xFFB3202F)) {
                    scope.launch { FriendRepository.removeFriend(f.pairId) }
                    removeTarget = null
                }
            }
        }
    }
}

@Composable
private fun FriendRow(f: FriendStreak, onNudge: () -> Unit, onLongPress: () -> Unit) {
    val c = LocalAppColors.current
    val both = f.iPlayedToday && f.friendPlayedToday
    AppCard(Modifier.fillMaxWidth().clickable(onClick = onLongPress)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Brand.violetBrush),
                contentAlignment = Alignment.Center
            ) { Text(f.friendName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.friendName, fontWeight = FontWeight.Black, color = c.text, fontSize = 17.sp)
                Row {
                    Check("Sen", f.iPlayedToday); Spacer(Modifier.width(8.dp)); Check(f.friendName.take(10), f.friendPlayedToday)
                }
                Text("Kişisel seri: 🔥${f.friendPersonalStreak}", color = c.subtext, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlameIcon(28.dp, lit = both || f.streak > 0)
                    Text("${f.streak}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Brand.Flame)
                }
                if (!f.friendPlayedToday) {
                    Text(
                        "👋 Dürt", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Flame)
                            .clickable(onClick = onNudge).padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Check(name: String, ok: Boolean) {
    val c = LocalAppColors.current
    Text(
        (if (ok) "✅ " else "⏳ ") + name, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        color = if (ok) Brand.Mint else c.subtext,
        modifier = Modifier.border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

private fun copy(ctx: Context, code: String) {
    val cm = ctx.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("kod", code))
    Toast.makeText(ctx, "Kod kopyalandı", Toast.LENGTH_SHORT).show()
}

private fun storeLink(ctx: Context) = "https://play.google.com/store/apps/details?id=${ctx.packageName}"

private fun invite(ctx: Context, code: String) {
    val name = ProgressRepository.current.playerName
    val text = "🔥 $name seni SudoFlame'de ortak seriye davet ediyor! Uygulamayı indir, Arkadaşlar sekmesine " +
        "bu kodu gir: $code\n${storeLink(ctx)}"
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Davet et"))
}

private fun nudge(ctx: Context, f: FriendStreak) {
    val text = "👋 ${f.friendName}! Ortak serimiz ${f.streak} gün 🔥 Bugünkü bulmacanı çözmeyi unutma, alevimiz sönmesin! ${storeLink(ctx)}"
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Dürt"))
}
