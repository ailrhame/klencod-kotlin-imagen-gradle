package com.example.imagen.ui.screens

import android.content.Context

object BinStorageHelper {

    fun getDeletedFolders(context: Context): List<Pair<String, Int>> {
        val activeFolders = HordeImagenStorage.loadFolders(context).toSet()
        val deletedImages = HordeImagenStorage.loadImages(context).filter { it.deleted }

        return deletedImages
            .filter { !activeFolders.contains(it.folder) }
            .groupBy { it.folder }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
    }

    fun getDeletedImages(context: Context): List<HordeImagenStorage.StoredImage> {
        val activeFolders = HordeImagenStorage.loadFolders(context).toSet()

        return HordeImagenStorage.loadImages(context)
            .filter { it.deleted && activeFolders.contains(it.folder) }
            .sortedByDescending { it.timestamp }
    }

    fun restoreImages(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return
        HordeImagenStorage.markDeleted(context, ids, false)
    }

    fun restoreFolder(context: Context, folder: String) {
        val folders = HordeImagenStorage.loadFolders(context).toMutableList()

        if (!folders.contains(folder)) {
            folders.add(folder)
            HordeImagenStorage.saveFolders(context, folders)
        }

        val images = HordeImagenStorage.loadImages(context).map { image ->
            if (image.folder == folder && image.deleted) {
                image.copy(deleted = false)
            } else {
                image
            }
        }

        HordeImagenStorage.saveImages(context, images)
    }

    fun deleteImagesPermanently(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return

        val images = HordeImagenStorage.loadImages(context)

        images
            .filter { ids.contains(it.id) }
            .forEach { image ->
                val file = HordeImagenStorage.getImageFile(context, image)
                if (file.exists()) {
                    file.delete()
                }
            }

        HordeImagenStorage.saveImages(
            context,
            images.filterNot { ids.contains(it.id) }
        )
    }

    fun deleteFolderPermanently(context: Context, folder: String) {
        val images = HordeImagenStorage.loadImages(context)

        val folderImages = images.filter { it.deleted && it.folder == folder }

        folderImages.forEach { image ->
            val file = HordeImagenStorage.getImageFile(context, image)
            if (file.exists()) {
                file.delete()
            }
        }

        HordeImagenStorage.saveImages(
            context,
            images.filterNot { it.deleted && it.folder == folder }
        )

        val folders = HordeImagenStorage.loadFolders(context).toMutableList()
        folders.remove(folder)
        HordeImagenStorage.saveFolders(context, folders)
    }

    fun restoreSelected(
        context: Context,
        folders: Set<String>,
        imageIds: Set<String>
    ) {
        folders.forEach { folder ->
            restoreFolder(context, folder)
        }

        restoreImages(context, imageIds)
    }

    fun deleteSelectedPermanently(
        context: Context,
        folders: Set<String>,
        imageIds: Set<String>
    ) {
        folders.forEach { folder ->
            deleteFolderPermanently(context, folder)
        }

        deleteImagesPermanently(context, imageIds)
    }
}