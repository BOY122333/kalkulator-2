package com.example.storage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.storage.model.AppVaultItem
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class InstalledAppDetail(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
)

class AppVaultRepository(
    private val context: Context,
    private val storageEngine: JsonStorageEngine
) {

    private val listType = Types.newParameterizedType(List::class.java, AppVaultItem::class.java)
    private val appVaultFileName = "app_vault.json"

    private val _vaultAppsFlow = MutableStateFlow(loadVaultApps())
    val vaultAppsFlow: StateFlow<List<AppVaultItem>> = _vaultAppsFlow.asStateFlow()

    private fun loadVaultApps(): List<AppVaultItem> {
        return storageEngine.read(appVaultFileName, listType) { emptyList() }
    }

    private fun saveVaultApps(list: List<AppVaultItem>) {
        storageEngine.write(appVaultFileName, listType, list)
        _vaultAppsFlow.value = list
    }

    /**
     * Retrieves all launchable user applications using the standard launcher intent query.
     */
    suspend fun getInstalledLaunchableApps(): List<InstalledAppDetail> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        resolveInfos.mapNotNull { resolveInfo ->
            val pkgName = resolveInfo.activityInfo.packageName
            // Exclude self
            if (pkgName == context.packageName) return@mapNotNull null

            val appName = resolveInfo.loadLabel(packageManager)?.toString() ?: pkgName
            val icon = resolveInfo.loadIcon(packageManager)
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            InstalledAppDetail(
                packageName = pkgName,
                appName = appName,
                versionName = try {
                    packageManager.getPackageInfo(pkgName, 0).versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                },
                isSystemApp = isSystem,
                icon = icon
            )
        }.distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Adds an application to the App Vault.
     */
    fun addAppToVault(packageName: String, appName: String, notificationPolicy: String = "ACTIVE") {
        val current = _vaultAppsFlow.value
        if (current.none { it.packageName == packageName }) {
            val updated = current + AppVaultItem(
                packageName = packageName,
                appName = appName,
                notificationPolicy = notificationPolicy,
                addedAt = System.currentTimeMillis()
            )
            saveVaultApps(updated)
        }
    }

    /**
     * Removes an application from the App Vault.
     */
    fun removeAppFromVault(packageName: String) {
        val current = _vaultAppsFlow.value
        val updated = current.filterNot { it.packageName == packageName }
        saveVaultApps(updated)
    }

    /**
     * Updates notification preference policy for an app in App Vault.
     */
    fun updateNotificationPolicy(packageName: String, policy: String) {
        val current = _vaultAppsFlow.value
        val updated = current.map {
            if (it.packageName == packageName) it.copy(notificationPolicy = policy) else it
        }
        saveVaultApps(updated)
    }

    /**
     * Launches an app securely through standard Android Intent.
     */
    fun launchApp(packageName: String): Result<Unit> {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Aplikasi tidak dapat dibuka atau tidak memiliki aktivitas launcher."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks device compatibility for advanced Android sandbox features (e.g. Android 15 Private Space).
     */
    fun getDeviceSandboxCompatibilityInfo(): String {
        val sdk = Build.VERSION.SDK_INT
        return when {
            sdk >= 35 -> "Android 15+ terdeteksi: Perangkat ini mendukung integrasi native 'Private Space' di level sistem operasi. Aplikasi sensitif dapat dikelompokkan ke ruang privat Android terpisah."
            sdk in 29..34 -> "Android 10 - 14 terdeteksi: Mendukung isolasi Profil Kerja (Work Profile) dan penguncian akses. Menyembunyikan aplikasi dari antarmuka luar membutuhkan launcher kustom pihak ketiga atau izin Administrator Perangkat resmi."
            else -> "Android versi lama: Proteksi dilakukan melalui penguncian akses gerbang Vault di dalam aplikasi ini."
        }
    }
}
