package com.glass.launcher

import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent { HomeScreen(refresh, homeSignal) }
    }

    override fun onResume() {
        super.onResume()
        refresh++
    }

    // Botão Home pressionado com o launcher já aberto: volta à primeira página
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal++
    }
}

/** Efeito vidro: fundo translúcido com borda de luz. */
fun Modifier.glass(radius: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x1AFFFFFF))))
        .border(1.dp, Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x1AFFFFFF))), shape)
}

@Composable
fun HomeScreen(refresh: Int, homeSignal: Int) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    LaunchedEffect(refresh) { apps = withContext(Dispatchers.Default) { loadApps(ctx) } }

    val dock = remember(apps) { pickDock(apps) }
    val pages = remember(apps) {
        val rest = apps.filter { a -> dock.none { it.pkg == a.pkg } }
        listOf(rest.take(16)) + rest.drop(16).chunked(24)
    }
    val pager = rememberPagerState { pages.size }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(homeSignal) { searching = false; pager.scrollToPage(0) }

    val open: (AppInfo) -> Unit = { searching = false; ctx.openApp(it) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            HorizontalPager(pager, Modifier.weight(1f).padding(top = 12.dp)) { i ->
                Page(pages[i], i == 0, open)
            }
            if (pages.size > 1) Dots(pages.size, pager.currentPage)
            SearchPill { searching = true }
            Dock(dock, open)
        }
        if (searching) SearchOverlay(apps, open) { searching = false }
    }
}

@Composable
fun Page(items: List<AppInfo>, first: Boolean, onOpen: (AppInfo) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (first) ClockWidget()
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { AppIcon(it, Modifier.weight(1f), 60.dp, true, onOpen) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, modifier: Modifier, size: Dp, showLabel: Boolean, onOpen: (AppInfo) -> Unit) {
    Column(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onOpen(app) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.225f))
        )
        if (showLabel) {
            Text(
                app.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = Shadow(Color(0x66000000), blurRadius = 4f)),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ClockWidget() {
    val locale = Locale.getDefault()
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(5_000) } }
    val time = remember(now) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "Hm"), locale).format(now)
    }
    val day = remember(now) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(now)
    }
    Column(Modifier.fillMaxWidth().glass(26.dp).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(day, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Text(time, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun Dots(count: Int, current: Int) {
    Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            Box(
                Modifier.padding(3.dp).size(7.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 1f else 0.4f))
            )
        }
    }
}

@Composable
fun SearchPill(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            "Pesquisar",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.glass(16.dp).clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun Dock(apps: List<AppInfo>, onOpen: (AppInfo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
            .glass(34.dp).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        apps.forEach { AppIcon(it, Modifier, 58.dp, false, onOpen) }
    }
}

@Composable
fun SearchOverlay(apps: List<AppInfo>, onOpen: (AppInfo) -> Unit, onClose: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)

    Column(
        Modifier.fillMaxSize()
            .background(Color(0xCC000000))
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding().imePadding().padding(16.dp)
    ) {
        BasicTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth().glass(16.dp).padding(14.dp).focusRequester(focus)
        )
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(apps.filter { it.label.contains(q, ignoreCase = true) }) { app ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(app) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
