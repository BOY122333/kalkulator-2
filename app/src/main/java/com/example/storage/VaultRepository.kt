package com.example.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.security.CryptoEngine
import com.example.storage.model.FileCategory
import com.example.storage.model.FileLocationState
import com.example.storage.model.VaultFileItem
import com.example.storage.model.VaultFolder
import com.example.storage.model.VaultRegistry
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.crypto.SecretKey

class VaultRepository(
    private val context: Context,
    private val storageEngine: JsonStorageEngine
) {

    private val registryType = Types.newParameterizedType(VaultRegistry::class.java)
    private val registryFileName = "vault_registry.json"

    private val vaultFilesDir = File(context.filesDir, "vault_files").apply {
        if (!exists()) mkdirs()
    }

    private val _registryFlow = MutableStateFlow(loadRegistry())
    val registryFlow: StateFlow<VaultRegistry> = _registryFlow.asStateFlow()

    private fun loadRegistry(): VaultRegistry {
        return storageEngine.read(registryFileName, registryType) {
            VaultRegistry(
                files = emptyList(),
                folders = emptyList(),
                maxQuotaBytes = 1073741824L // 1GB
            )
        }
    }

    private fun saveRegistry(registry: VaultRegistry) {
        storageEngine.write(registryFileName, registryType, registry)
        _registryFlow.value = registry
    }

    /**
     * Imports a file from Uri (SAF or PhotoPicker), encrypts it with AES-256-GCM,
     * computes SHA-256 of the plain file, and registers it in the vault.
     */
    suspend fun importFile(
        uri: Uri,
        folderId: String? = null,
        masterKey: SecretKey
    ): Result<VaultFileItem> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var displayName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: detectMimeType(displayName)
            val category = categorizeMimeType(mimeType, displayName)

            val currentRegistry = _registryFlow.value
            val currentUsage = currentRegistry.files.filter { !it.isTrash }.sumOf { it.sizeBytes }
            if (currentUsage + fileSize > currentRegistry.maxQuotaBytes) {
                return@withContext Result.failure(
                    IllegalStateException("Kapasitas penyimpanan Ruang Rahasia penuh (${currentUsage / (1024 * 1024)}MB / ${currentRegistry.maxQuotaBytes / (1024 * 1024)}MB)")
                )
            }

            val fileId = UUID.randomUUID().toString()
            val storageFileName = "$fileId.enc"
            val targetFile = File(vaultFilesDir, storageFileName)

            // Encrypt and simultaneously calculate SHA-256
            var actualBytes = 0L
            val tempPlainFile = File(context.cacheDir, "temp_import_$fileId.tmp")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempPlainFile).use { fos ->
                        val buffer = ByteArray(16384)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                            actualBytes += read
                        }
                        fos.flush()
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Tidak dapat membaca berkas"))

                val checksum = CryptoEngine.calculateSha256(tempPlainFile)

                // Encrypt from temp to target vault file
                FileInputStream(tempPlainFile).use { plainIn ->
                    FileOutputStream(targetFile).use { encOut ->
                        CryptoEngine.encryptStream(plainIn, encOut, masterKey)
                        encOut.flush()
                    }
                }

                val item = VaultFileItem(
                    id = fileId,
                    storageFileName = storageFileName,
                    originalDisplayName = displayName,
                    mimeType = mimeType,
                    category = category.name,
                    sizeBytes = actualBytes,
                    sha256Checksum = checksum,
                    folderId = folderId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isTrash = false,
                    locationState = FileLocationState.LOCAL_ONLY.name
                )

                val updatedFiles = currentRegistry.files + item
                saveRegistry(currentRegistry.copy(files = updatedFiles))
                Result.success(item)
            } finally {
                if (tempPlainFile.exists()) tempPlainFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts an internal file and writes it to an OutputStream (e.g. for SAF export or sharing).
     */
    suspend fun exportFile(
        fileItem: VaultFileItem,
        outputStream: OutputStream,
        masterKey: SecretKey
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encFile = File(vaultFilesDir, fileItem.storageFileName)
            if (!encFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Berkas terenkripsi tidak ditemukan di penyimpanan lokal"))
            }

            FileInputStream(encFile).use { encIn ->
                CryptoEngine.decryptStream(encIn, outputStream, masterKey)
                outputStream.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens decrypted stream to temporary cache file for preview.
     */
    suspend fun getDecryptedPreviewFile(
        fileItem: VaultFileItem,
        masterKey: SecretKey
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val encFile = File(vaultFilesDir, fileItem.storageFileName)
            if (!encFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Berkas lokal tidak ditemukan"))
            }

            val ext = fileItem.originalDisplayName.substringAfterLast(".", "tmp")
            val previewFile = File(context.cacheDir, "preview_${fileItem.id}.$ext")
            previewFile.deleteOnExit()

            FileInputStream(encFile).use { encIn ->
                FileOutputStream(previewFile).use { previewOut ->
                    CryptoEngine.decryptStream(encIn, previewOut, masterKey)
                    previewOut.flush()
                }
            }
            Result.success(previewFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Moves a file to Trash / Recycle Bin.
     */
    fun moveToTrash(fileId: String) {
        val current = _registryFlow.value
        val updated = current.files.map {
            if (it.id == fileId) {
                it.copy(isTrash = true, trashedAt = System.currentTimeMillis())
            } else it
        }
        saveRegistry(current.copy(files = updated))
    }

    /**
     * Restores a file from Trash.
     */
    fun restoreFromTrash(fileId: String) {
        val current = _registryFlow.value
        val updated = current.files.map {
            if (it.id == fileId) {
                it.copy(isTrash = false, trashedAt = null)
            } else it
        }
        saveRegistry(current.copy(files = updated))
    }

    /**
     * Permanently deletes a file locally.
     */
    fun permanentlyDeleteFile(fileId: String): Boolean {
        val current = _registryFlow.value
        val fileItem = current.files.find { it.id == fileId } ?: return false

        val encFile = File(vaultFilesDir, fileItem.storageFileName)
        if (encFile.exists()) {
            encFile.delete()
        }

        val updated = current.files.filterNot { it.id == fileId }
        saveRegistry(current.copy(files = updated))
        return true
    }

    /**
     * Empties all items in the Trash.
     */
    fun emptyTrash(): Int {
        val current = _registryFlow.value
        val trashed = current.files.filter { it.isTrash }
        trashed.forEach {
            val encFile = File(vaultFilesDir, it.storageFileName)
            if (encFile.exists()) encFile.delete()
        }
        val remaining = current.files.filterNot { it.isTrash }
        saveRegistry(current.copy(files = remaining))
        return trashed.size
    }

    /**
     * Renames a file's display name.
     */
    fun renameFile(fileId: String, newName: String) {
        val current = _registryFlow.value
        val updated = current.files.map {
            if (it.id == fileId) it.copy(originalDisplayName = newName.trim(), updatedAt = System.currentTimeMillis())
            else it
        }
        saveRegistry(current.copy(files = updated))
    }

    /**
     * Moves file to a folder.
     */
    fun moveFile(fileId: String, targetFolderId: String?) {
        val current = _registryFlow.value
        val updated = current.files.map {
            if (it.id == fileId) it.copy(folderId = targetFolderId, updatedAt = System.currentTimeMillis())
            else it
        }
        saveRegistry(current.copy(files = updated))
    }

    /**
     * Updates file sync location state.
     */
    fun updateLocationState(fileId: String, newState: FileLocationState, cloudFileId: String? = null) {
        val current = _registryFlow.value
        val updated = current.files.map {
            if (it.id == fileId) {
                it.copy(
                    locationState = newState.name,
                    cloudFileId = cloudFileId ?: it.cloudFileId,
                    updatedAt = System.currentTimeMillis()
                )
            } else it
        }
        saveRegistry(current.copy(files = updated))
    }

    /**
     * Creates a new folder.
     */
    fun createFolder(name: String, parentFolderId: String? = null): VaultFolder {
        val current = _registryFlow.value
        val newFolder = VaultFolder(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAt = System.currentTimeMillis(),
            parentFolderId = parentFolderId
        )
        val updatedFolders = current.folders + newFolder
        saveRegistry(current.copy(folders = updatedFolders))
        return newFolder
    }

    /**
     * Deletes a folder (and moves contained files to root).
     */
    fun deleteFolder(folderId: String) {
        val current = _registryFlow.value
        val updatedFolders = current.folders.filterNot { it.id == folderId }
        val updatedFiles = current.files.map {
            if (it.folderId == folderId) it.copy(folderId = null) else it
        }
        saveRegistry(current.copy(files = updatedFiles, folders = updatedFolders))
    }

    private fun detectMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "doc", "docx" -> "application/msword"
            else -> "application/octet-stream"
        }
    }

    private fun categorizeMimeType(mime: String, fileName: String): FileCategory {
        val lowerMime = mime.lowercase()
        val ext = fileName.substringAfterLast(".", "").lowercase()

        return when {
            lowerMime.startsWith("image/") -> FileCategory.PHOTO
            lowerMime.startsWith("video/") -> FileCategory.VIDEO
            lowerMime == "application/pdf" || ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> FileCategory.DOCUMENT
            lowerMime.startsWith("text/") || ext in listOf("txt", "md", "csv", "log") -> FileCategory.TEXT
            lowerMime.contains("zip") || lowerMime.contains("tar") || ext in listOf("zip", "rar", "7z", "gz", "tar") -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }
}
