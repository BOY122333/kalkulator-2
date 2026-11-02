package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.security.CryptoEngine
import com.example.storage.VaultRepository
import com.example.storage.model.FileLocationState
import com.example.storage.model.VaultFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.crypto.SecretKey

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val currentFile: String, val progressPercent: Int) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val errorMessage: String) : SyncState()
}

class CloudSyncManager(
    private val context: Context,
    private val vaultRepository: VaultRepository
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    var wifiOnly: Boolean = false

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false

        if (wifiOnly) {
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
        return true
    }

    /**
     * Uploads an encrypted file to the private cloud backend.
     * Only updates the state to SYNCED_BOTH after server confirms and SHA-256 matches.
     */
    suspend fun uploadFileToCloud(
        fileItem: VaultFileItem,
        token: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Koneksi jaringan tidak tersedia (atau dibatasi Wi-Fi)"))
        }

        val vaultFilesDir = File(context.filesDir, "vault_files")
        val encFile = File(vaultFilesDir, fileItem.storageFileName)
        if (!encFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Berkas lokal tidak ditemukan"))
        }

        _syncState.value = SyncState.Syncing(fileItem.originalDisplayName, 20)

        try {
            val requestFile = encFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", fileItem.storageFileName, requestFile)

            _syncState.value = SyncState.Syncing(fileItem.originalDisplayName, 50)

            val response = ApiClient.api.uploadEncryptedFile(
                token = "Bearer $token",
                filePart = body,
                fileId = fileItem.id,
                checksum = fileItem.sha256Checksum
            )

            if (response.isSuccessful) {
                // Verified upload success
                vaultRepository.updateLocationState(
                    fileId = fileItem.id,
                    newState = FileLocationState.SYNCED_BOTH,
                    cloudFileId = fileItem.id
                )
                _syncState.value = SyncState.Success("Berkas '${fileItem.originalDisplayName}' berhasil disinkronkan ke cloud.")
                Result.success(Unit)
            } else {
                val err = "Upload gagal: ${response.code()} ${response.message()}"
                _syncState.value = SyncState.Error(err)
                Result.failure(IllegalStateException(err))
            }
        } catch (e: Exception) {
            val err = "Gangguan koneksi cloud: ${e.message}"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Downloads an encrypted file from cloud and saves it locally.
     */
    suspend fun downloadFileFromCloud(
        fileItem: VaultFileItem,
        token: String,
        masterKey: SecretKey
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Koneksi jaringan tidak tersedia"))
        }

        _syncState.value = SyncState.Syncing(fileItem.originalDisplayName, 30)

        try {
            val response = ApiClient.api.downloadEncryptedFile(
                token = "Bearer $token",
                fileId = fileItem.id
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val vaultFilesDir = File(context.filesDir, "vault_files")
                val encFile = File(vaultFilesDir, fileItem.storageFileName)

                body.byteStream().use { input ->
                    FileOutputStream(encFile).use { output ->
                        val buffer = ByteArray(16384)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }

                // Verify integrity
                val tempPlain = File(context.cacheDir, "verify_${fileItem.id}.tmp")
                try {
                    encFile.inputStream().use { encIn ->
                        FileOutputStream(tempPlain).use { plainOut ->
                            CryptoEngine.decryptStream(encIn, plainOut, masterKey)
                        }
                    }
                    val downloadedSha = CryptoEngine.calculateSha256(tempPlain)
                    if (downloadedSha != fileItem.sha256Checksum) {
                        encFile.delete()
                        val mismatchErr = "Verifikasi integritas SHA-256 gagal! Berkas cloud mungkin rusak."
                        _syncState.value = SyncState.Error(mismatchErr)
                        return@withContext Result.failure(IllegalStateException(mismatchErr))
                    }
                } finally {
                    if (tempPlain.exists()) tempPlain.delete()
                }

                vaultRepository.updateLocationState(fileItem.id, FileLocationState.SYNCED_BOTH)
                _syncState.value = SyncState.Success("Berkas '${fileItem.originalDisplayName}' berhasil diunduh dan diverifikasi.")
                Result.success(Unit)
            } else {
                val err = "Gagal mengunduh: ${response.code()}"
                _syncState.value = SyncState.Error(err)
                Result.failure(IllegalStateException(err))
            }
        } catch (e: Exception) {
            val err = "Gagal transfer: ${e.message}"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}
