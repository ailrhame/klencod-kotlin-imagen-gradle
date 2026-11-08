package com.example.imagen.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun SelectionBar(
    count: Int,
    onCancel: () -> Unit,
    actions: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel selection")
            }
            Text(
                "$count selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

@Composable
internal fun BarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(icon, contentDescription = label)
        }
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
internal fun SelectBadge(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen() {
    val context = LocalContext.current

    var images by remember { mutableStateOf<List<HordeImagenStorage.StoredImage>>(emptyList()) }
    var folders by remember { mutableStateOf<List<String>>(listOf("All")) }
    var currentFolder by remember { mutableStateOf("All") }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }

    var actionImage by remember { mutableStateOf<HordeImagenStorage.StoredImage?>(null) }
    var viewerImage by remember { mutableStateOf<HordeImagenStorage.StoredImage?>(null) }

    var newFolderName by remember { mutableStateOf("") }
    var renameFolderName by remember { mutableStateOf("") }

    var expandedFolders by remember { mutableStateOf(false) }

    fun refresh() {
        images = HordeImagenStorage.loadImages(context)
        folders = HordeImagenStorage.loadFolders(context)
        if (!folders.contains(currentFolder)) currentFolder = "All"
    }

    LaunchedEffect(Unit) { refresh() }

    val visibleImages = images.filter { image ->
        !image.deleted && (currentFolder == "All" || image.folder == currentFolder)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentFolder, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${visibleImages.size} images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!isMultiSelectMode) {
                    Box {
                        IconButton(onClick = { expandedFolders = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Switch folder")
                        }
                        DropdownMenu(expanded = expandedFolders, onDismissRequest = { expandedFolders = false }) {
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder) },
                                    onClick = {
                                        currentFolder = folder
                                        expandedFolders = false
                                        selectedIds = emptySet()
                                    }
                                )
                            }
                        }
                    }

                    TextButton(onClick = { isMultiSelectMode = true }) { Text("Select") }

                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New folder")
                    }

                    if (currentFolder != "All") {
                        IconButton(onClick = { renameFolderName = currentFolder; showRenameFolderDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename folder")
                        }
                        IconButton(onClick = { showDeleteFolderDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete folder")
                        }
                    }
                }
            }

            if (visibleImages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No images here yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Generate one and it will land here automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = visibleImages, key = { it.id }) { image ->
                        GalleryImageItem(
                            context = context,
                            image = image,
                            isSelected = selectedIds.contains(image.id),
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    selectedIds = if (selectedIds.contains(image.id)) selectedIds - image.id else selectedIds + image.id
                                } else {
                                    viewerImage = image
                                }
                            },
                            onLongClick = {
                                if (isMultiSelectMode) {
                                    selectedIds = if (selectedIds.contains(image.id)) selectedIds - image.id else selectedIds + image.id
                                } else {
                                    actionImage = image
                                    showItemDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isMultiSelectMode) {
            SelectionBar(
                count = selectedIds.size,
                onCancel = { selectedIds = emptySet(); isMultiSelectMode = false },
                actions = {
                    BarAction(Icons.Default.DriveFileMove, "Move") {
                        if (selectedIds.isNotEmpty()) showMoveDialog = true
                    }
                    BarAction(Icons.Default.Delete, "Delete") {
                        HordeImagenStorage.markDeleted(context, selectedIds, true)
                        selectedIds = emptySet()
                        isMultiSelectMode = false
                        refresh()
                    }
                }
            )
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, label = { Text("Folder name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        HordeImagenStorage.createFolder(context, newFolderName)
                        newFolderName = ""
                        refresh()
                    }
                    showCreateFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRenameFolderDialog) {
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(value = renameFolderName, onValueChange = { renameFolderName = it }, label = { Text("New folder name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val old = currentFolder
                    val new = renameFolderName.trim()
                    if (new.isNotBlank() && HordeImagenStorage.renameFolder(context, old, new)) {
                        currentFolder = new
                        refresh()
                    }
                    showRenameFolderDialog = false
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteFolderDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            title = { Text("Delete Folder") },
            text = { Text("Delete \"$currentFolder\"? Images inside it will be moved to Bin.") },
            confirmButton = {
                TextButton(onClick = {
                    HordeImagenStorage.deleteFolder(context, currentFolder)
                    currentFolder = "All"
                    refresh()
                    showDeleteFolderDialog = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMoveDialog) {
        val moveIds = selectedIds.ifEmpty { actionImage?.let { setOf(it.id) } ?: emptySet() }
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to Folder") },
            text = {
                Column {
                    folders.forEach { folder ->
                        TextButton(onClick = {
                            HordeImagenStorage.moveImages(context, moveIds, folder)
                            selectedIds = emptySet()
                            isMultiSelectMode = false
                            showMoveDialog = false
                            refresh()
                        }) { Text(folder) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") } }
        )
    }

    if (showItemDialog && actionImage != null) {
        val target = actionImage!!
        AlertDialog(
            onDismissRequest = { showItemDialog = false },
            title = { Text("Image Actions") },
            text = {
                Column {
                    Text(
                        text = target.prompt,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextButton(onClick = {
                        val file = HordeImagenStorage.getImageFile(context, target)
                        val ok = MediaSaver.saveToDevice(context, file)
                        Toast.makeText(context, if (ok) "Saved to Pictures/Horde Imagen" else "Save failed (storage permission needed)", Toast.LENGTH_SHORT).show()
                        showItemDialog = false
                    }) { Text("Download") }
                    TextButton(onClick = {
                        val file = HordeImagenStorage.getImageFile(context, target)
                        val ok = MediaSaver.shareImage(context, file)
                        if (!ok) Toast.makeText(context, "Share failed (check FileProvider setup)", Toast.LENGTH_SHORT).show()
                        showItemDialog = false
                    }) { Text("Share") }
                    TextButton(onClick = {
                        selectedIds = setOf(target.id)
                        showMoveDialog = true
                        showItemDialog = false
                    }) { Text("Move") }
                    TextButton(onClick = {
                        HordeImagenStorage.markDeleted(context, setOf(target.id), true)
                        showItemDialog = false
                        refresh()
                    }) { Text("Delete") }
                }
            },
            confirmButton = { TextButton(onClick = { showItemDialog = false }) { Text("Cancel") } }
        )
    }

    viewerImage?.let { image ->
        ImageViewerOverlay(context = context, image = image, onDismiss = { viewerImage = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryImageItem(
    context: Context,
    image: HordeImagenStorage.StoredImage,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bitmap = remember(image.id) {
        val file = HordeImagenStorage.getImageFile(context, image)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = image.prompt,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
            }
        }

        if (isMultiSelectMode) {
            SelectBadge(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerOverlay(
    context: Context,
    image: HordeImagenStorage.StoredImage,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showInfo by remember { mutableStateOf(false) }

    val bitmap = remember(image.id) {
        val file = HordeImagenStorage.getImageFile(context, image)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = image.prompt,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale > 1f) offset + pan else Offset.Zero
                            }
                        }
                        .combinedClickable(
                            onClick = { },
                            onDoubleClick = {
                                scale = if (scale > 1f) 1f else 2.5f
                                offset = Offset.Zero
                            }
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val ok = MediaSaver.shareImage(context, HordeImagenStorage.getImageFile(context, image))
                    if (!ok) Toast.makeText(context, "Share failed (check FileProvider setup)", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White) }
                IconButton(onClick = {
                    val ok = MediaSaver.saveToDevice(context, HordeImagenStorage.getImageFile(context, image))
                    Toast.makeText(context, if (ok) "Saved to Pictures/Horde Imagen" else "Save failed", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White) }
                IconButton(onClick = { showInfo = !showInfo }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                }
            }

            if (showInfo) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = image.prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromptHistoryScreen() {
    val context = LocalContext.current

    var prompts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedPrompts by remember { mutableStateOf(setOf<String>()) }

    var actionPrompt by remember { mutableStateOf<String?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { prompts = HordeImagenStorage.loadPrompts(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${prompts.size} prompts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!isMultiSelectMode) {
                    TextButton(onClick = { isMultiSelectMode = true }) { Text("Select") }
                }
            }

            if (prompts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No prompts yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(prompts) { prompt ->
                        val isSelected = selectedPrompts.contains(prompt)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            selectedPrompts = if (isSelected) selectedPrompts - prompt else selectedPrompts + prompt
                                        } else {
                                            HordeImagenStorage.pendingPrompt = prompt
                                            Toast.makeText(context, "Prompt ready in Generate screen", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onLongClick = {
                                        if (isMultiSelectMode) {
                                            selectedPrompts = if (isSelected) selectedPrompts - prompt else selectedPrompts + prompt
                                        } else {
                                            actionPrompt = prompt
                                            showItemDialog = true
                                        }
                                    }
                                ),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isMultiSelectMode) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    SelectBadge(selected = isSelected, onClick = {
                                        selectedPrompts = if (isSelected) selectedPrompts - prompt else selectedPrompts + prompt
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isMultiSelectMode) {
            SelectionBar(
                count = selectedPrompts.size,
                onCancel = { selectedPrompts = emptySet(); isMultiSelectMode = false },
                actions = {
                    BarAction(Icons.Default.Delete, "Delete") {
                        HordeImagenStorage.deletePrompts(context, selectedPrompts)
                        prompts = HordeImagenStorage.loadPrompts(context)
                        selectedPrompts = emptySet()
                        isMultiSelectMode = false
                    }
                }
            )
        }
    }

    if (showItemDialog && actionPrompt != null) {
        val target = actionPrompt!!
        AlertDialog(
            onDismissRequest = { showItemDialog = false },
            title = { Text("Prompt Actions") },
            text = {
                Column {
                    Text(
                        text = target,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextButton(onClick = {
                        HordeImagenStorage.pendingPrompt = target
                        Toast.makeText(context, "Prompt ready in Generate screen", Toast.LENGTH_SHORT).show()
                        showItemDialog = false
                    }) { Text("Use") }
                    TextButton(onClick = {
                        HordeImagenStorage.deletePrompts(context, setOf(target))
                        prompts = HordeImagenStorage.loadPrompts(context)
                        showItemDialog = false
                    }) { Text("Delete") }
                }
            },
            confirmButton = { TextButton(onClick = { showItemDialog = false }) { Text("Cancel") } }
        )
    }
}