package com.glass.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent { ConfigScreen(refresh) }
    }

    // Ao voltar da tela de configurações, atualiza o status
    override fun onResume() {
        super.onResume()
        refresh++
    }
}

@Composable
fun ConfigScreen(refresh: Int) {
    val ctx = LocalContext.current
    val (current, ours) = remember(refresh) {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        if (ri == null || ri.activityInfo.packageName == "android") {
            Pair<String?, Boolean>(null, false)
        } else {
            Pair<String?, Boolean>(
                ri.loadLabel(ctx.packageManager).toString(),
                ri.activityInfo.packageName == ctx.packageName
            )
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1B3A), Color(0xFF6B5B95))))
            .systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Glass Launcher", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))

        Column(Modifier.fillMaxWidth().glass(24.dp).padding(20.dp)) {
            Text("Launcher padrão agora", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Text(
                current ?: "Nenhum definido (o Android vai perguntar)",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium
            )
            Text(
                if (ours) "O Glass Launcher está ativo." else "O Glass Launcher não é o padrão.",
                color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                try { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                catch (_: Exception) { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B6CFF))
        ) { Text("Escolher launcher padrão") }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { ctx.startActivity(Intent(ctx, HomeActivity::class.java)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))
        ) { Text("Abrir prévia sem definir como padrão") }

        Text(
            "Na tela de escolha, selecione o Glass Launcher ou o launcher original do celular.",
            color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
