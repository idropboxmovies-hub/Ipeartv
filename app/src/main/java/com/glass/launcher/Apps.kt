package com.glass.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val label: String,
    val pkg: String,
    val component: ComponentName,
    val icon: ImageBitmap
)

/** Lista todos os apps que aparecem na gaveta, em ordem alfabética. */
fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                pkg = it.activityInfo.packageName,
                component = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                icon = it.loadIcon(pm).toBitmap(144, 144).asImageBitmap()
            )
        }
        .sortedBy { it.label.lowercase() }
}

fun Context.openApp(app: AppInfo) {
    val i = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    try { startActivity(i) } catch (_: Exception) { }
}

/** Dock: telefone, navegador, mensagens e música; completa com os primeiros apps se faltar. */
fun pickDock(apps: List<AppInfo>): List<AppInfo> {
    val hints = listOf(
        listOf("dialer", "incallui", ".phone"),
        listOf("chrome", "browser", "firefox"),
        listOf("messag", "mms", "sms"),
        listOf("music", "spotify")
    )
    val picked = hints
        .mapNotNull { h -> apps.firstOrNull { a -> h.any { a.pkg.contains(it, true) } } }
        .distinctBy { it.pkg }
    return (picked + apps.filter { a -> picked.none { it.pkg == a.pkg } }).take(4)
}
