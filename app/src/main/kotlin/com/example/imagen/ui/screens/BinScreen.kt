package com.example.imagen.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private sealed class BinListItem {
    data class FolderItem(val name: String, val count: Int) : BinListItem()
    data class ImageItem(val image: HordeImagenStorage.StoredImage) : BinListItem()
}

private data class PermanentDeleteTarget(
    val folders: Set<String>,
    val imageIds: Set<String>,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BinScreen() {
    val context = LocalContext.current

    var deletedFolders by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var deletedImages by remember { mutableStateOf<List<HordeImagenStorage.StoredImage>>(emptyList()) }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedFolders by remember { mutableStateOf(setOf<String>()) }
    var selectedImages by remember { mutableStateOf(setOf<String>()) }

    var dialogItem by remember { mutableStateOf<BinListItem?>(null) }
    var permanentTarget by remember { mutableStateOf<PermanentDeleteTarget?>(null) }

    fun refresh() {
        deletedFolders = BinStorageHelper.getDeletedFolders(context)
        deletedImages = BinStorageHelper.getDeletedImages(context)
    }

    LaunchedEffect(Unit) { refresh() }

    val binItems = remember(deletedFolders, deletedImages) {
        val items = mutableListOf<BinListItem>()
        deletedFolders.forEach { items.add(BinListItem.FolderItem(it.first, it.second)) }
        deletedImages.forEach { items.add(BinListItem.ImageItem(it)) }
        items
    }

    val selectedCount = selectedFolders.size + selectedImages.size

    fun toggleFolder(name: String) {
        selectedFolders = if (selectedFolders.contains(name)) selectedFolders - name else selectedFolders + name
    }

    fun toggleImage(id: String) {
        selectedImages = if (selectedImages.contains(id)) selectedImages - id else selectedImages + id
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${binItems.size} items in Bin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!isMultiSelectMode) {
                    TextButton(onClick = { isMultiSelectMode = true }) { Text("Select") }
                }
            }

            if (binItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Bin is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    items(
                        items = binItems,
                        key = { item ->
                            when (item) {
                                is BinListItem.FolderItem -> "folder_${item.name}"
                                is BinListItem.ImageItem -> "image_${item.image.id}"
                            }
                        },
                        span = { item ->
                            when (item) {
                                is BinListItem.FolderItem -> GridItemSpan(maxLineSpan)
                                is BinListItem.ImageItem -> GridItemSpan(1)
                            }
                        }
                    ) { item ->
                        when (item) {
                            is BinListItem.FolderItem -> {
                                val isSelected = selectedFolders.contains(item.name)
                                BinFolderCard(
                                    name = item.name,
                                    count = item.count,
                                    isSelected = isSelected,
                                    isMultiSelectMode = isMultiSelectMode,
                                    onClick = { if (isMultiSelectMode) toggleFolder(item.name) else dialogItem = item },
                                    onLongClick = { if (isMultiSelectMode) toggleFolder(item.name) else dialogItem = item }
                                )
                            }
                            is BinListItem.ImageItem -> {
                                val isSelected = selectedImages.contains(item.image.id)
                                BinImageTile(
                                    context = context,
                                    image = item.image,
                                    isSelected = isSelected,
                                    isMultiSelectMode = isMultiSelectMode,
                                    onClick = { if (isMultiSelectMode) toggleImage(item.image.id) else dialogItem = item },
                                    onLongClick = { if (isMultiSelectMode) toggleImage(item.image.id) else dialogItem = item }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMultiSelectMode) {
            SelectionBar(
                count = selectedCount,
                onCancel = { selectedFolders = emptySet(); selectedImages = emptySet(); isMultiSelectMode = false },
                actions = {
                    BarAction(Icons.Default.Restore, "Restore") {
                        if (selectedCount > 0) {
                            BinStorageHelper.restoreSelected(context, selectedFolders, selectedImages)
                            selectedFolders = emptySet()
                            selectedImages = emptySet()
                            isMultiSelectMode = false
                            refresh()
                        }
                    }
                    BarAction(Icons.Default.DeleteForever, "Delete") {
                        if (selectedCount > 0) {
                            permanentTarget = PermanentDeleteTarget(selectedFolders, selectedImages, "$selectedCount item(s)")
                        }
                    }
                }
            )
        }
    }

    dialogItem?.let { item ->
        val title = when (item) {
            is BinListItem.FolderItem -> item.name
            is BinListItem.ImageItem -> "Image Actions"
        }
        val description = when (item) {
            is BinListItem.FolderItem -> "This folder contains ${item.count} deleted image(s)."
            is BinListItem.ImageItem -> item.image.prompt
        }

        AlertDialog(
            onDismissRequest = { dialogItem = null },
            title = { Text(title) },
            text = {
                Column {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextButton(onClick = {
                        when (item) {
                            is BinListItem.FolderItem -> BinStorageHelper.restoreFolder(context, item.name)
                            is BinListItem.ImageItem -> BinStorageHelper.restoreImages(context, setOf(item.image.id))
                        }
                        selectedFolders = emptySet()
                        selectedImages = emptySet()
                        isMultiSelectMode = false
                        dialogItem = null
                        refresh()
                    }) { Text("Restore") }
                    TextButton(onClick = {
                        permanentTarget = when (item) {
                            is BinListItem.FolderItem -> PermanentDeleteTarget(setOf(item.name), emptySet(), "folder \"${item.name}\"")
                            is BinListItem.ImageItem -> PermanentDeleteTarget(emptySet(), setOf(item.image.id), "this image")
                        }
                        dialogItem = null
                    }) { Text("Delete Permanently") }
                }
            },
            confirmButton = { TextButton(onClick = { dialogItem = null }) { Text("Cancel") } }
        )
    }

    permanentTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { permanentTarget = null },
            title = { Text("Delete Permanently") },
            text = { Text("This cannot be undone.\n\nDelete ${target.description}?") },
            confirmButton = {
                TextButton(onClick = {
                    BinStorageHelper.deleteSelectedPermanently(context, target.folders, target.imageIds)
                    selectedFolders = emptySet()
                    selectedImages = emptySet()
                    isMultiSelectMode = false
                    permanentTarget = null
                    refresh()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { permanentTarget = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BinFolderCard(
    name: String,
    count: Int,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Deleted folder: $name", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isMultiSelectMode) {
                SelectBadge(selected = isSelected, onClick = onClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BinImageTile(
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
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
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