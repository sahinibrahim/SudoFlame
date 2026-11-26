package com.sudoflame.app

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sudoflame.app.ads.AdsManager
import com.sudoflame.app.data.FriendRepository
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.data.Today
import com.sudoflame.app.game.Difficulty
import com.sudoflame.app.ui.ChunkyButton
import com.sudoflame.app.ui.FlameIcon
import com.sudoflame.app.ui.screens.DailyScreen
import com.sudoflame.app.ui.screens.FriendsScreen
import com.sudoflame.app.ui.screens.GameScreen
import com.sudoflame.app.ui.screens.GameViewModel
import com.sudoflame.app.ui.screens.HomeScreen
import com.sudoflame.app.ui.screens.ProfileScreen
import com.sudoflame.app.ui.theme.Brand
import com.sudoflame.app.ui.theme.LocalAppColors
import com.sudoflame.app.ui.theme.SudoFlameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdsManager.gatherConsentAndInit(this)
        setContent { SudoFlameTheme { Root() } }
    }

    override fun onResume() {
        super.onResume()
        ProgressRepository.refreshDay()
    }
}

private data class Tab(val route: String, val icon: ImageVector, val label: String)

private val tabs = listOf(
    Tab("home", Icons.Rounded.Home, "Ana Sayfa"),
    Tab("daily", Icons.Rounded.CalendarMonth, "Günlük"),
    Tab("friends", Icons.Rounded.Group, "Arkadaşlar"),
    Tab("me", Icons.Rounded.Person, "Ben"),
)

@Composable
private fun Root() {
    val p by ProgressRepository.state.collectAsState()
    if (!p.onboarded) { Onboarding(); return }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        FriendRepository.connect()
    }

    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val c = LocalAppColors.current

    Column(Modifier.fillMaxSize().background(c.bg)) {
        NavHost(
            nav, startDestination = "home", modifier = Modifier.weight(1f),
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
        ) {
            composable("home") {
                HomeScreen(
                    onPlay = { mode, d -> nav.navigate("game/$mode/${d.name}/-1") },
                    onDaily = { nav.navigate("game/daily/EASY/${Today.epochDay()}") },
                    onFriends = { nav.switchTab("friends") },
                )
            }
            composable("daily") { DailyScreen(onPlayDay = { day -> nav.navigate("game/daily/EASY/$day") }) }
            composable("friends") { FriendsScreen() }
            composable("me") { ProfileScreen() }
            composable(
                "game/{mode}/{diff}/{day}",
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("diff") { type = NavType.StringType },
                    navArgument("day") { type = NavType.LongType },
                )
            ) { e ->
                val mode = e.arguments?.getString("mode") ?: "new"
                val diff = runCatching { Difficulty.valueOf(e.arguments?.getString("diff") ?: "EASY") }.getOrDefault(Difficulty.EASY)
                val day = e.arguments?.getLong("day") ?: -1L
                val vm: GameViewModel = viewModel()
                LaunchedEffect(Unit) { vm.start(mode, diff, day) }
                GameScreen(
                    vm,
                    onExit = { nav.popBackStack() },
                    onNewGame = {
                        nav.popBackStack()
                        nav.navigate("game/new/${vm.state.difficulty.name}/-1")
                    },
                )
            }
        }
        if (!route.startsWith("game")) BottomBar(route) { nav.switchTab(it) }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.background(c.card).navigationBarsPadding()) {
        HorizontalDivider(color = c.line, thickness = 2.dp)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            for (t in tabs) {
                val sel = current == t.route
                val s by animateFloatAsState(if (sel) 1.18f else 1f, label = "tab")
                Column(
                    Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSelect(t.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.scale(s).clip(RoundedCornerShape(50))
                            .background(if (sel) Brand.Violet.copy(alpha = 0.14f) else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(t.icon, contentDescription = t.label, tint = if (sel) Brand.Violet else c.subtext, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        t.label, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                        color = if (sel) Brand.Violet else c.subtext
                    )
                }
            }
        }
    }
}

@Composable
private fun Onboarding() {
    var name by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Brand.violetBrush).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            FlameIcon(120.dp)
            Row {
                Text("Sudo", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Flame", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Brand.Sun)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Her gün bir bulmaca çöz, alevini büyüt.\nArkadaşlarınla ortak seri yak! 🔥",
                color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, fontSize = 17.sp
            )
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(16) }, singleLine = true,
                label = { Text("Adın ne?") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Brand.Sun, unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Brand.Sun, unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Brand.Sun,
                )
            )
            Spacer(Modifier.weight(1f))
            ChunkyButton("HADİ BAŞLAYALIM", Modifier.fillMaxWidth(), color = Brand.Sun, shadow = Color(0xFFD99A00), textColor = Brand.Night) {
                ProgressRepository.update { it.copy(onboarded = true, playerName = name.trim().ifBlank { "Oyuncu" }) }
            }
        }
    }
}
