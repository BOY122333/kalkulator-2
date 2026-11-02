package com.example.storage.model

import com.squareup.moshi.JsonClass

enum class FileCategory {
    PHOTO, VIDEO, DOCUMENT, TEXT, ARCHIVE, OTHER
}

enum class FileLocationState {
    LOCAL_ONLY,
    CLOUD_ONLY,
    SYNCED_BOTH,
    SYNC_PENDING
}

@JsonClass(generateAdapter = true)
data class VaultFileItem(
    val id: String,
    val storageFileName: String, // internal encrypted file name (UUID.dat)
    val originalDisplayName: String,
    val mimeType: String,
    val category: String, // PHOTO, VIDEO, DOCUMENT, TEXT, ARCHIVE, OTHER
    val sizeBytes: Long,
    val sha256Checksum: String,
    val folderId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isTrash: Boolean = false,
    val trashedAt: Long? = null,
    val locationState: String = FileLocationState.LOCAL_ONLY.name,
    val cloudFileId: String? = null,
    val cloudVersion: Int = 1
)

@JsonClass(generateAdapter = true)
data class VaultFolder(
    val id: String,
    val name: String,
    val createdAt: Long,
    val parentFolderId: String? = null
)

@JsonClass(generateAdapter = true)
data class VaultRegistry(
    val files: List<VaultFileItem> = emptyList(),
    val folders: List<VaultFolder> = emptyList(),
    val maxQuotaBytes: Long = 1073741824L // 1 GB default
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val username: String,
    val deviceId: String,
    val role: String = "USER", // "USER", "ADMIN", "OWNER"
    val sessionToken: String? = null,
    val isBiometricEnabled: Boolean = false,
    val pinSaltHex: String = "",
    val pinHashHex: String = "",
    val privacyAccepted: Boolean = false,
    val registeredAt: Long = 0L,
    val lastLoginAt: Long = 0L,
    val offlineMode: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AppVaultItem(
    val packageName: String,
    val appName: String,
    val notificationPolicy: String = "ACTIVE", // "ACTIVE", "SILENT", "DISABLED_GUIDE"
    val addedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AppRemoteConfig(
    val version: Int = 1,
    val appDisplayName: String = "NetVault",
    val homeTitle: String = "Kalkulator IP Subnet & Vault",
    val appDescription: String = "Kalkulator IP Subnet Profesional, Private Vault Terenkripsi AES-256, dan App Vault Terintegrasi.",
    val primaryColorHex: String = "#0284C7",
    val accentColorHex: String = "#38BDF8",
    val announcementMessage: String = "NetVault v1.0 siap digunakan. Akses Ruang Rahasia terlindungi enkripsi AES-256.",
    val helpText: String = "Gunakan tab Subnet untuk menghitung CIDR /0-/32. Akses Ruang Rahasia dengan PIN atau Biometrik.",
    val maxUserStorageMb: Int = 1024,
    val openingMode: String = "NONE", // "NONE", "IMAGE", "ANIMATION"
    val openingDurationMs: Long = 2500L,
    val openingSkipAllowed: Boolean = true,
    val openingAssetUrl: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AuditLogEntry(
    val id: String,
    val timestamp: Long,
    val eventType: String,
    val username: String,
    val details: String,
    val ipAddressObserved: String = "127.0.0.1"
)

@JsonClass(generateAdapter = true)
data class UsageStatsRecord(
    val registeredUsersCount: Int = 1,
    val activeUsersDaily: Int = 1,
    val activeUsersWeekly: Int = 1,
    val activeUsersMonthly: Int = 1,
    val totalCalculations: Long = 0L,
    val totalCloudUploads: Long = 0L,
    val totalCloudDownloads: Long = 0L,
    val totalCloudBytes: Long = 0L,
    val auditLogs: List<AuditLogEntry> = emptyList()
)
