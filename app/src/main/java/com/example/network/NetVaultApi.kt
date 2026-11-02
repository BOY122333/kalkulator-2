package com.example.network

import com.example.storage.model.AppRemoteConfig
import com.example.storage.model.UsageStatsRecord
import com.example.storage.model.VaultFileItem
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val username: String,
    val deviceId: String,
    val action: String = "LOGIN_OR_REGISTER" // "REGISTER", "LOGIN", "CHECK"
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val status: String,
    val message: String,
    val sessionToken: String? = null,
    val role: String = "USER",
    val registeredAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SyncMetadataRequest(
    val username: String,
    val localFiles: List<VaultFileItem>
)

@JsonClass(generateAdapter = true)
data class SyncMetadataResponse(
    val status: String,
    val cloudFiles: List<VaultFileItem>,
    val serverTime: Long
)

@JsonClass(generateAdapter = true)
data class TelemetryReport(
    val username: String,
    val deviceId: String,
    val calculationCount: Long,
    val uploadedBytes: Long,
    val downloadedBytes: Long,
    val appVersion: String
)

interface NetVaultApi {

    @POST("api/auth.php")
    suspend fun authenticate(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @GET("api/config.php")
    suspend fun getRemoteConfig(
        @Query("version") clientVersion: Int = 0
    ): Response<AppRemoteConfig>

    @POST("api/config.php")
    suspend fun updateRemoteConfig(
        @Header("Authorization") token: String,
        @Body config: AppRemoteConfig
    ): Response<Map<String, Any>>

    @POST("api/stats.php")
    suspend fun submitTelemetry(
        @Body telemetry: TelemetryReport
    ): Response<Map<String, Any>>

    @GET("api/stats.php")
    suspend fun getAdminUsageStats(
        @Header("Authorization") token: String
    ): Response<UsageStatsRecord>

    @POST("api/vault.php?action=sync_metadata")
    suspend fun syncMetadata(
        @Header("Authorization") token: String,
        @Body request: SyncMetadataRequest
    ): Response<SyncMetadataResponse>

    @Multipart
    @POST("api/vault.php?action=upload_file")
    suspend fun uploadEncryptedFile(
        @Header("Authorization") token: String,
        @Part filePart: MultipartBody.Part,
        @Query("fileId") fileId: String,
        @Query("checksum") checksum: String
    ): Response<Map<String, Any>>

    @Streaming
    @GET("api/vault.php?action=download_file")
    suspend fun downloadEncryptedFile(
        @Header("Authorization") token: String,
        @Query("fileId") fileId: String
    ): Response<ResponseBody>

    @POST("api/vault.php?action=delete_file")
    suspend fun deleteCloudFile(
        @Header("Authorization") token: String,
        @Query("fileId") fileId: String
    ): Response<Map<String, Any>>
}
