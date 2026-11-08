package com.example.imagen.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("horde_imagen_settings", Context.MODE_PRIVATE)

    var selectedEngine by remember { mutableStateOf("pollinations") }

    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var width by remember { mutableStateOf(1024) }
    var height by remember { mutableStateOf(1024) }
    var seed by remember { mutableStateOf("") }

    var pollinationsModel by remember { mutableStateOf("flux") }

    var workersModel by remember { mutableStateOf("@cf/black-forest-labs/flux-1-schnell") }
    var workersSteps by remember { mutableStateOf(20f) }
    var workersGuidance by remember { mutableStateOf(7.5f) }

    var isGenerating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        HordeImagenStorage.pendingPrompt?.let {
            prompt = it
            HordeImagenStorage.pendingPrompt = null
        }
    }

    val pollinationsModels = listOf("flux", "flux-realism", "flux-anime", "flux-3d", "flux-pro", "any-dark", "turbo")
    val workersModels = listOf(
        "@cf/black-forest-labs/flux-1-schnell" to "Flux Schnell",
        "@cf/stabilityai/stable-diffusion-xl-base-1.0" to "Stable Diffusion XL",
        "@cf/bytedance/stable-diffusion-xl-lightning" to "SDXL Lightning"
    )
    val dimensions = listOf(512, 768, 1024)
    val maxSteps = if (workersModel.contains("flux", true) || workersModel.contains("lightning", true)) 8 else 50

    val workersConfigured = prefs.getString("workers_token", "")?.isNotBlank() == true &&
            prefs.getString("workers_account", "")?.isNotBlank() == true

    val canGenerate = !isGenerating && prompt.isNotBlank() &&
            (selectedEngine == "pollinations" || workersConfigured)

    fun handleSuccess(bitmap: Bitmap, promptText: String) {
        HordeImagenStorage.savePrompt(context, promptText)
        val saved = HordeImagenStorage.saveGeneratedImage(context, bitmap, promptText)
        val file = saved?.let { HordeImagenStorage.getImageFile(context, it) }
        val deviceSaved = file?.let { MediaSaver.autoDownloadIfEnabled(context, it) } == true
        generatedBitmap = bitmap
        isGenerating = false
        statusMessage = when {
            saved == null -> "Done."
            deviceSaved -> "Done. Saved to Gallery and Device."
            else -> "Done. Saved to Gallery."
        }
    }

    fun handleFailure(err: String) {
        errorMessage = err
        isGenerating = false
        statusMessage = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedEngine == "pollinations",
                onClick = { selectedEngine = "pollinations" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Pollinations") }
            SegmentedButton(
                selected = selectedEngine == "workersai",
                onClick = { selectedEngine = "workersai" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Workers AI") }
        }

        if (selectedEngine == "workersai" && !workersConfigured) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Workers AI needs your Cloudflare token and account ID. Add them in Settings.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Describe your image",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    minLines = 3,
                    maxLines = 5
                )
                OutlinedTextField(
                    value = negativePrompt,
                    onValueChange = { negativePrompt = it },
                    label = { Text("Negative Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var expandedWidth by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedWidth,
                        onExpandedChange = { expandedWidth = !expandedWidth },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "${width}px",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Width") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWidth) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedWidth, onDismissRequest = { expandedWidth = false }) {
                            dimensions.forEach { dim ->
                                DropdownMenuItem(text = { Text("$dim") }, onClick = { width = dim; expandedWidth = false })
                            }
                        }
                    }

                    var expandedHeight by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedHeight,
                        onExpandedChange = { expandedHeight = !expandedHeight },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "${height}px",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Height") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHeight) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedHeight, onDismissRequest = { expandedHeight = false }) {
                            dimensions.forEach { dim ->
                                DropdownMenuItem(text = { Text("$dim") }, onClick = { height = dim; expandedHeight = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it.filter { c -> c.isDigit() } },
                    label = { Text("Seed (Optional)") },
                    placeholder = { Text("Random") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Model", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))

                if (selectedEngine == "pollinations") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pollinationsModels.forEach { model ->
                            FilterChip(
                                selected = pollinationsModel == model,
                                onClick = { pollinationsModel = model },
                                label = { Text(model) }
                            )
                        }
                    }
                } else {
                    var expandedWorkersModel by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedWorkersModel,
                        onExpandedChange = { expandedWorkersModel = !expandedWorkersModel },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        val friendlyName = workersModels.firstOrNull { it.first == workersModel }?.second ?: workersModel
                        OutlinedTextField(
                            value = friendlyName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWorkersModel) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedWorkersModel, onDismissRequest = { expandedWorkersModel = false }) {
                            workersModels.forEach { (id, name) ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { workersModel = id; expandedWorkersModel = false })
                            }
                        }
                    }

                    Text("Steps: ${workersSteps.toInt().coerceIn(1, maxSteps)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = workersSteps.coerceIn(1f, maxSteps.toFloat()),
                        onValueChange = { workersSteps = it.coerceIn(1f, maxSteps.toFloat()) },
                        valueRange = 1f..maxSteps.toFloat()
                    )

                    Text("Guidance: ${"%.1f".format(workersGuidance.coerceIn(1f, 8f))}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = workersGuidance.coerceIn(1f, 8f),
                        onValueChange = { workersGuidance = it.coerceIn(1f, 8f) },
                        valueRange = 1f..8f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMessage = null
                generatedBitmap = null
                isGenerating = true
                statusMessage = "Generating..."

                val finalPrompt = prompt.trim()
                val finalNegative = negativePrompt.trim()
                val finalSeed = seed.trim().toLongOrNull()

                if (selectedEngine == "pollinations") {
                    PollinationsApi.generate(
                        prompt = finalPrompt,
                        negativePrompt = finalNegative,
                        width = width,
                        height = height,
                        seed = finalSeed,
                        model = pollinationsModel,
                        onProgress = { statusMessage = it },
                        onSuccess = { bitmap -> handleSuccess(bitmap, finalPrompt) },
                        onError = { err -> handleFailure(err) }
                    )
                } else {
                    WorkersAiApi.generate(
                        token = prefs.getString("workers_token", "") ?: "",
                        account = prefs.getString("workers_account", "") ?: "",
                        model = workersModel,
                        prompt = finalPrompt,
                        negativePrompt = finalNegative,
                        width = width,
                        height = height,
                        steps = workersSteps.toInt().coerceIn(1, maxSteps),
                        guidance = workersGuidance.coerceIn(1f, 8f),
                        seed = finalSeed,
                        onProgress = { statusMessage = it },
                        onSuccess = { bitmap -> handleSuccess(bitmap, finalPrompt) },
                        onError = { err -> handleFailure(err) }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = canGenerate,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Generate Image", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (isGenerating) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(statusMessage)
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        generatedBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private object PollinationsApi {
    private const val BASE = "https://image.pollinations.ai/prompt/"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun generate(
        prompt: String, negativePrompt: String, width: Int, height: Int, seed: Long?, model: String,
        onProgress: (String) -> Unit, onSuccess: (Bitmap) -> Unit, onError: (String) -> Unit
    ) {
        Thread {
            try {
                val sb = StringBuilder(BASE)
                sb.append(URLEncoder.encode(prompt, "UTF-8"))
                sb.append("?width=").append(width)
                sb.append("&height=").append(height)
                sb.append("&model=").append(URLEncoder.encode(model, "UTF-8"))
                sb.append("&nologo=true")
                if (negativePrompt.isNotBlank()) sb.append("&negative_prompt=").append(URLEncoder.encode(negativePrompt, "UTF-8"))
                if (seed != null) sb.append("&seed=").append(seed)

                val conn = URL(sb.toString()).openConnection() as HttpURLConnection
                conn.connectTimeout = 60000
                conn.readTimeout = 90000
                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    mainHandler.post { onError("HTTP $code from Pollinations") }
                    return@Thread
                }
                val bytes = conn.inputStream.readBytes()
                conn.disconnect()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    mainHandler.post { onError("Could not decode image") }
                    return@Thread
                }
                mainHandler.post { onSuccess(bitmap) }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "Network error") }
            }
        }.start()
    }
}

private object WorkersAiApi {
    private const val BASE = "https://api.cloudflare.com/client/v4/accounts"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun generate(
        token: String, account: String, model: String, prompt: String, negativePrompt: String,
        width: Int, height: Int, steps: Int, guidance: Float, seed: Long?,
        onProgress: (String) -> Unit, onSuccess: (Bitmap) -> Unit, onError: (String) -> Unit
    ) {
        Thread {
            try {
                val conn = URL("$BASE/$account/ai/run/$model").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 60000
                conn.readTimeout = 120000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")

                val body = JSONObject()
                body.put("prompt", prompt)
                if (model.contains("flux", ignoreCase = true)) {
                    body.put("steps", steps)
                    if (seed != null) body.put("seed", seed)
                } else {
                    if (negativePrompt.isNotBlank()) body.put("negative_prompt", negativePrompt)
                    body.put("width", width)
                    body.put("height", height)
                    body.put("num_steps", steps)
                    body.put("guidance", guidance.toDouble())
                    if (seed != null) body.put("seed", seed)
                }

                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                val rawBytes = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.use { it.readBytes() } ?: ByteArray(0)
                conn.disconnect()

                if (code !in 200..299) {
                    val errText = String(rawBytes, Charsets.UTF_8)
                    val message = try {
                        JSONObject(errText).optJSONArray("errors")?.optJSONObject(0)?.optString("message") ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code"
                    }
                    mainHandler.post { onError(message) }
                    return@Thread
                }

                // Flux returns JSON {"result":{"image": base64}}; SDXL family returns raw image bytes
                val imageBytes: ByteArray = if (rawBytes.isNotEmpty() && rawBytes[0] == '{'.code.toByte()) {
                    val imageB64 = JSONObject(String(rawBytes, Charsets.UTF_8))
                        .optJSONObject("result")?.optString("image")
                    if (imageB64.isNullOrBlank()) {
                        mainHandler.post { onError("No image in response") }
                        return@Thread
                    }
                    Base64.decode(imageB64, Base64.DEFAULT)
                } else {
                    rawBytes
                }

                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null) {
                    mainHandler.post { onError("Could not decode image") }
                    return@Thread
                }
                mainHandler.post { onSuccess(bitmap) }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "Network error") }
            }
        }.start()
    }
}

object HordeImagenStorage {
    var pendingPrompt: String? = null
    private const val IMAGES_DIR = "horde_images"
    private const val IMAGES_DB = "images.json"
    private const val PROMPTS_DB = "prompts.json"
    private const val FOLDERS_DB = "folders.json"

    data class StoredImage(
        val id: String, val fileName: String, val prompt: String, val folder: String,
        val deleted: Boolean, val timestamp: Long, val deletedAt: Long = 0L
    )

    fun saveGeneratedImage(context: Context, bitmap: Bitmap, prompt: String): StoredImage? {
        return try {
            val dir = File(context.filesDir, IMAGES_DIR); dir.mkdirs()
            val id = UUID.randomUUID().toString(); val fileName = "$id.png"; val file = File(dir, fileName)
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val item = StoredImage(id, fileName, prompt, "All", false, System.currentTimeMillis(), 0L)
            val images = loadImages(context).toMutableList(); images.add(0, item); saveImages(context, images); item
        } catch (e: Exception) { null }
    }

    fun getImageFile(context: Context, image: StoredImage): File = File(File(context.filesDir, IMAGES_DIR), image.fileName)

    fun loadImages(context: Context): List<StoredImage> {
        return try {
            val file = File(context.filesDir, IMAGES_DB); if (!file.exists()) return emptyList()
            val array = JSONArray(file.readText()); val result = mutableListOf<StoredImage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val image = StoredImage(
                    obj.optString("id"), obj.optString("fileName"), obj.optString("prompt"),
                    obj.optString("folder", "All"), obj.optBoolean("deleted", false),
                    obj.optLong("timestamp", 0L), obj.optLong("deletedAt", 0L)
                )
                if (getImageFile(context, image).exists()) result.add(image)
            }
            result
        } catch (e: Exception) { emptyList() }
    }

    fun saveImages(context: Context, images: List<StoredImage>) {
        try {
            val array = JSONArray()
            images.forEach {
                val obj = JSONObject()
                obj.put("id", it.id); obj.put("fileName", it.fileName); obj.put("prompt", it.prompt)
                obj.put("folder", it.folder); obj.put("deleted", it.deleted)
                obj.put("timestamp", it.timestamp); obj.put("deletedAt", it.deletedAt)
                array.put(obj)
            }
            File(context.filesDir, IMAGES_DB).writeText(array.toString())
        } catch (e: Exception) { }
    }

    fun markDeleted(context: Context, ids: Set<String>, deleted: Boolean) {
        val now = System.currentTimeMillis()
        saveImages(context, loadImages(context).map {
            if (ids.contains(it.id)) {
                if (deleted) it.copy(deleted = true, deletedAt = now) else it.copy(deleted = false, deletedAt = 0L)
            } else it
        })
    }

    fun moveImages(context: Context, ids: Set<String>, folder: String) {
        saveImages(context, loadImages(context).map { if (ids.contains(it.id)) it.copy(folder = folder) else it })
    }

    fun loadFolders(context: Context): List<String> {
        return try {
            val file = File(context.filesDir, FOLDERS_DB)
            if (!file.exists()) { val d = listOf("All"); saveFolders(context, d); return d }
            val array = JSONArray(file.readText()); val folders = mutableListOf<String>()
            for (i in 0 until array.length()) folders.add(array.getString(i))
            if (!folders.contains("All")) folders.add(0, "All") else { folders.remove("All"); folders.add(0, "All") }
            folders.distinct()
        } catch (e: Exception) { listOf("All") }
    }

    fun saveFolders(context: Context, folders: List<String>) {
        try {
            val safe = folders.distinct().toMutableList()
            if (!safe.contains("All")) safe.add(0, "All") else { safe.remove("All"); safe.add(0, "All") }
            val array = JSONArray(); safe.forEach { array.put(it) }
            File(context.filesDir, FOLDERS_DB).writeText(array.toString())
        } catch (e: Exception) { }
    }

    fun createFolder(context: Context, name: String): Boolean {
        val n = name.trim(); if (n.isBlank()) return false
        val f = loadFolders(context); if (f.any { it.equals(n, true) }) return false
        saveFolders(context, f + n); return true
    }

    fun renameFolder(context: Context, oldName: String, newName: String): Boolean {
        val n = newName.trim()
        if (oldName == "All" || n.isBlank() || oldName == n) return false
        val f = loadFolders(context).toMutableList(); if (f.any { it.equals(n, true) }) return false
        val idx = f.indexOfFirst { it == oldName }; if (idx == -1) return false
        f[idx] = n; saveFolders(context, f)
        saveImages(context, loadImages(context).map { if (it.folder == oldName) it.copy(folder = n) else it })
        return true
    }

    fun deleteFolder(context: Context, name: String): Boolean {
        if (name == "All") return false
        val now = System.currentTimeMillis()
        val f = loadFolders(context).toMutableList(); f.remove(name); saveFolders(context, f)
        saveImages(context, loadImages(context).map { if (it.folder == name) it.copy(deleted = true, deletedAt = now) else it })
        return true
    }

    fun autoEmptyBinIfNeeded(context: Context) {
        try {
            val prefs = context.getSharedPreferences("horde_imagen_settings", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("auto_empty_bin", false)) return
            val days = prefs.getInt("auto_empty_days", 7)
            val cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
            val images = loadImages(context)
            val expired = images.filter { it.deleted && it.deletedAt in 1L..cutoff }
            if (expired.isEmpty()) return
            expired.forEach { val file = getImageFile(context, it); if (file.exists()) file.delete() }
            saveImages(context, images.filterNot { expired.contains(it) })
        } catch (e: Exception) { }
    }

    fun savePrompt(context: Context, prompt: String) {
        val t = prompt.trim(); if (t.isBlank()) return
        val p = loadPrompts(context).toMutableList(); p.remove(t); p.add(0, t)
        if (p.size > 200) p.removeAt(p.lastIndex)
        savePrompts(context, p)
    }

    fun loadPrompts(context: Context): List<String> {
        return try {
            val file = File(context.filesDir, PROMPTS_DB); if (!file.exists()) return emptyList()
            val array = JSONArray(file.readText()); val result = mutableListOf<String>()
            for (i in 0 until array.length()) result.add(array.getString(i))
            result.distinct()
        } catch (e: Exception) { emptyList() }
    }

    fun deletePrompts(context: Context, promptsToDelete: Set<String>) {
        savePrompts(context, loadPrompts(context).filter { !promptsToDelete.contains(it) })
    }

    private fun savePrompts(context: Context, prompts: List<String>) {
        try {
            val array = JSONArray(); prompts.forEach { array.put(it) }
            File(context.filesDir, PROMPTS_DB).writeText(array.toString())
        } catch (e: Exception) { }
    }
}

object MediaSaver {
    private const val FOLDER_NAME = "Horde Imagen"

    fun needsPermission(): Boolean = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q

    fun hasStoragePermission(context: Context): Boolean =
        if (needsPermission()) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

    @Suppress("DEPRECATION")
    fun saveToDevice(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/" + FOLDER_NAME)
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                true
            } else {
                if (!hasStoragePermission(context)) return false
                val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), FOLDER_NAME)
                dir.mkdirs(); file.copyTo(File(dir, file.name), overwrite = true); true
            }
        } catch (e: Exception) { false }
    }

    fun autoDownloadIfEnabled(context: Context, file: File): Boolean {
        val prefs = context.getSharedPreferences("horde_imagen_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_download", false)) return false
        return saveToDevice(context, file)
    }

    fun shareImage(context: Context, file: File): Boolean {
        return try {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share image").apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
            true
        } catch (e: Exception) { false }
    }
}