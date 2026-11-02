package com.example.storage

import android.content.Context
import com.example.calculator.model.CalculationHistoryItem
import com.example.security.CryptoEngine
import com.example.storage.model.AppRemoteConfig
import com.example.storage.model.AuditLogEntry
import com.example.storage.model.UsageStatsRecord
import com.example.storage.model.UserProfile
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class UserAndConfigRepository(
    private val context: Context,
    private val storageEngine: JsonStorageEngine
) {

    private val userProfileType = Types.newParameterizedType(UserProfile::class.java)
    private val userProfileFile = "user_profile.json"

    private val configType = Types.newParameterizedType(AppRemoteConfig::class.java)
    private val configFile = "app_remote_config.json"

    private val historyType = Types.newParameterizedType(List::class.java, CalculationHistoryItem::class.java)
    private val historyFile = "calc_history.json"

    private val statsType = Types.newParameterizedType(UsageStatsRecord::class.java)
    private val statsFile = "usage_stats.json"

    private val _userProfileFlow = MutableStateFlow(loadUserProfile())
    val userProfileFlow: StateFlow<UserProfile> = _userProfileFlow.asStateFlow()

    private val _remoteConfigFlow = MutableStateFlow(loadRemoteConfig())
    val remoteConfigFlow: StateFlow<AppRemoteConfig> = _remoteConfigFlow.asStateFlow()

    private val _calcHistoryFlow = MutableStateFlow(loadCalcHistory())
    val calcHistoryFlow: StateFlow<List<CalculationHistoryItem>> = _calcHistoryFlow.asStateFlow()

    private val _usageStatsFlow = MutableStateFlow(loadUsageStats())
    val usageStatsFlow: StateFlow<UsageStatsRecord> = _usageStatsFlow.asStateFlow()

    private fun loadUserProfile(): UserProfile {
        return storageEngine.read(userProfileFile, userProfileType) {
            val deviceId = getOrCreateDeviceId()
            UserProfile(
                username = "",
                deviceId = deviceId,
                role = "USER",
                sessionToken = null,
                isBiometricEnabled = false,
                privacyAccepted = false,
                registeredAt = 0L,
                lastLoginAt = 0L
            )
        }
    }

    private fun loadRemoteConfig(): AppRemoteConfig {
        return storageEngine.read(configFile, configType) {
            AppRemoteConfig()
        }
    }

    private fun loadCalcHistory(): List<CalculationHistoryItem> {
        return storageEngine.read(historyFile, historyType) { emptyList() }
    }

    private fun loadUsageStats(): UsageStatsRecord {
        return storageEngine.read(statsFile, statsType) {
            UsageStatsRecord()
        }
    }

    private fun getOrCreateDeviceId(): String {
        val idFile = File(context.filesDir, "device_identity.id")
        return if (idFile.exists()) {
            idFile.readText().trim()
        } else {
            val newId = UUID.randomUUID().toString()
            idFile.writeText(newId)
            newId
        }
    }

    // User Profile operations

    fun validateUsername(username: String): Result<String> {
        val trimmed = username.trim()
        if (trimmed.length < 3) {
            return Result.failure(IllegalArgumentException("Username minimal 3 karakter."))
        }
        if (trimmed.length > 20) {
            return Result.failure(IllegalArgumentException("Username maksimal 20 karakter."))
        }
        val regex = Regex("^[a-zA-Z0-9_]+$")
        if (!regex.matches(trimmed)) {
            return Result.failure(IllegalArgumentException("Username hanya boleh huruf, angka, dan garis bawah (_)."))
        }
        return Result.success(trimmed)
    }

    fun saveUserProfile(profile: UserProfile) {
        storageEngine.write(userProfileFile, userProfileType, profile)
        _userProfileFlow.value = profile
    }

    fun setPin(pin: String) {
        val salt = CryptoEngine.generateSalt(16)
        val hash = CryptoEngine.hashPin(pin, salt)
        val current = _userProfileFlow.value
        val updated = current.copy(
            pinSaltHex = CryptoEngine.bytesToHex(salt),
            pinHashHex = CryptoEngine.bytesToHex(hash)
        )
        saveUserProfile(updated)
        logAuditEvent("PIN_UPDATED", "Pengguna mengatur atau memperbarui PIN Ruang Rahasia.")
    }

    fun verifyPin(pin: String): Boolean {
        val current = _userProfileFlow.value
        if (current.pinSaltHex.isEmpty() || current.pinHashHex.isEmpty()) return false
        val salt = CryptoEngine.hexToBytes(current.pinSaltHex)
        val expectedHash = CryptoEngine.hexToBytes(current.pinHashHex)
        val isValid = CryptoEngine.verifyPin(pin, salt, expectedHash)
        if (isValid) {
            logAuditEvent("PIN_VERIFIED_SUCCESS", "Autentikasi PIN Ruang Rahasia berhasil.")
        } else {
            logAuditEvent("PIN_VERIFIED_FAIL", "Percobaan autentikasi PIN gagal.")
        }
        return isValid
    }

    fun hasPinSet(): Boolean {
        return _userProfileFlow.value.pinHashHex.isNotEmpty()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val current = _userProfileFlow.value
        saveUserProfile(current.copy(isBiometricEnabled = enabled))
        logAuditEvent("BIOMETRIC_TOGGLED", "Biometrik diubah menjadi: $enabled")
    }

    fun promoteToAdmin(secretAdminKey: String): Boolean {
        // Backend or secure local validation for role elevation
        if (secretAdminKey.trim() == "ADMIN_NETVAULT_SECRET_2026") {
            val current = _userProfileFlow.value
            saveUserProfile(current.copy(role = "ADMIN"))
            logAuditEvent("ROLE_ELEVATED", "Akun dinaikkan menjadi peran Administrator.")
            return true
        }
        logAuditEvent("ROLE_ELEVATION_FAILED", "Kunci verifikasi Administrator salah.")
        return false
    }

    // Config operations

    fun saveRemoteConfig(config: AppRemoteConfig) {
        storageEngine.write(configFile, configType, config)
        _remoteConfigFlow.value = config
        logAuditEvent("CONFIG_APPLIED", "Konfigurasi remote versi ${config.version} berhasil diterapkan.")
    }

    // Calculation History operations

    fun addCalculationHistory(item: CalculationHistoryItem) {
        val current = _calcHistoryFlow.value
        val updated = (listOf(item) + current).take(100) // Keep latest 100
        storageEngine.write(historyFile, historyType, updated)
        _calcHistoryFlow.value = updated

        // Increment calculation stat
        incrementCalculationStat()
    }

    fun clearCalculationHistory() {
        storageEngine.write(historyFile, historyType, emptyList<CalculationHistoryItem>())
        _calcHistoryFlow.value = emptyList()
    }

    fun deleteHistoryItem(id: String) {
        val current = _calcHistoryFlow.value
        val updated = current.filterNot { it.id == id }
        storageEngine.write(historyFile, historyType, updated)
        _calcHistoryFlow.value = updated
    }

    // Usage Stats & Audit Logging

    private fun incrementCalculationStat() {
        val stats = _usageStatsFlow.value
        val updated = stats.copy(totalCalculations = stats.totalCalculations + 1)
        saveUsageStats(updated)
    }

    fun logCloudTransfer(isUpload: Boolean, bytes: Long) {
        val stats = _usageStatsFlow.value
        val updated = if (isUpload) {
            stats.copy(
                totalCloudUploads = stats.totalCloudUploads + 1,
                totalCloudBytes = stats.totalCloudBytes + bytes
            )
        } else {
            stats.copy(
                totalCloudDownloads = stats.totalCloudDownloads + 1,
                totalCloudBytes = stats.totalCloudBytes + bytes
            )
        }
        saveUsageStats(updated)
    }

    fun logAuditEvent(eventType: String, details: String) {
        val current = _usageStatsFlow.value
        val username = _userProfileFlow.value.username.ifEmpty { "anonim" }
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            username = username,
            details = details,
            ipAddressObserved = "127.0.0.1 (Lokal)"
        )
        val updatedLogs = (listOf(entry) + current.auditLogs).take(200)
        saveUsageStats(current.copy(auditLogs = updatedLogs))
    }

    fun saveUsageStats(stats: UsageStatsRecord) {
        storageEngine.write(statsFile, statsType, stats)
        _usageStatsFlow.value = stats
    }
}
