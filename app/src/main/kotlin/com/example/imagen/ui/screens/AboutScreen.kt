package com.example.imagen.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.imagen.rememberAppIcon

@Composable
fun AboutScreen() {
    val appIcon = rememberAppIcon()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            appIcon?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Imagen", style = MaterialTheme.typography.headlineMedium)
            Text(
                "v1.1.0 • Pollinations + Workers AI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionCard(
            title = "About",
            lines = listOf(
                "Made By Jess",
                "Coded & compiled on Code Assist",
                "UI toolkit: Jetpack Compose",
                "Language: Kotlin",
                "Min SDK: API 21 • Android 5.0",
                "Target SDK: API 36 • Android 16",
                "Image generation: Pollinations • Cloudflare Workers AI"
            )
        )

        SectionCard(
            title = "Permissions",
            lines = listOf(
                "• Internet (Wi-Fi / mobile data): connect to generation engines and download images.",
                "• Storage (Android 9 and below only): save images to the public Pictures folder when you use Download or Auto-Download. Android 10+ needs no permission for this.",
                "• No ads, no trackers, no other permissions.",
                "• Your API tokens never leave this device except to authenticate with the chosen engine."
            )
        )

        SectionCard(
            title = "Changelog",
            lines = listOf(
                "v1.1.0",
                "• Swapped Horde for Pollinations + Cloudflare Workers AI engines.",
                "• Engine switcher with per-engine parameters.",
                "• Full UI restyle: bold titles, dividers, floating selection bars, folder cards.",
                "v1.0.0 — first release",
                "• Generate, Gallery with folders, Prompt History, Bin, Settings, About.",
                "• Fullscreen viewer, auto-download, auto-empty Bin, dark/light theme."
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SectionCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}