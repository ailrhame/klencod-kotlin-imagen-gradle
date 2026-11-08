package com.example.imagen.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("horde_imagen_settings", Context.MODE_PRIVATE)

    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var autoDownload by remember { mutableStateOf(prefs.getBoolean("auto_download", false)) }
    var autoEmptyBin by remember { mutableStateOf(prefs.getBoolean("auto_empty_bin", false)) }
    var daysUntilDeletion by remember { mutableStateOf(prefs.getInt("auto_empty_days", 7).toFloat()) }

    var workersToken by remember { mutableStateOf(prefs.getString("workers_token", "") ?: "") }
    var workersAccount by remember { mutableStateOf(prefs.getString("workers_account", "") ?: "") }
    var showWorkersDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cloudflare Workers AI", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Only needed for the Workers AI engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = workersAccount,
                    onValueChange = { workersAccount = it; prefs.edit().putString("workers_account", it).apply() },
                    label = { Text("Account ID") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = workersToken,
                    onValueChange = { workersToken = it; prefs.edit().putString("workers_token", it).apply() },
                    label = { Text("API Token") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showWorkersDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, "How to get token")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingRow("Dark Mode", isDarkMode) {
            isDarkMode = it
            prefs.edit().putBoolean("dark_mode", it).apply()
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingRow("Auto-Download to device", autoDownload) {
            autoDownload = it
            prefs.edit().putBoolean("auto_download", it).apply()
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingRow("Auto-Empty recycle bin", autoEmptyBin) {
            autoEmptyBin = it
            prefs.edit().putBoolean("auto_empty_bin", it).apply()
        }

        if (autoEmptyBin) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    "Delete after ${daysUntilDeletion.toInt()} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = daysUntilDeletion,
                    onValueChange = {
                        daysUntilDeletion = it
                        prefs.edit().putInt("auto_empty_days", it.toInt()).apply()
                    },
                    valueRange = 1f..90f
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showWorkersDialog) {
        AlertDialog(
            onDismissRequest = { showWorkersDialog = false },
            title = { Text("How to get Workers AI credentials?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1️⃣ Create a free account at https://dash.cloudflare.com/sign-up")
                    Text("2️⃣ Your Account ID is on the right side of any dashboard page (Overview).")
                    Text("3️⃣ Go to My Profile → API Tokens → Create Token → 'Workers AI' template → Create.")
                    Text("4️⃣ Copy the token once (it won't be shown again). Paste it above.")
                    Text("The free tier is generous (neurons-based) and includes Flux Schnell, SDXL, and Lightning models.")
                }
            },
            confirmButton = { TextButton(onClick = { showWorkersDialog = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}