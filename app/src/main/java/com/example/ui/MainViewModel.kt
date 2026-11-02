package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.engine.SubnetCalculator
import com.example.calculator.model.CalculationHistoryItem
import com.example.calculator.model.IPv6Result
import com.example.calculator.model.SubnetResult
import com.example.calculator.model.VlsmSubnet
import com.example.network.ApiClient
import com.example.network.AuthRequest
import com.example.network.CloudSyncManager
import com.example.network.SyncState
import com.example.security.BiometricHelper
import com.example.security.BiometricStatus
import com.example.security.CryptoEngine
import com.example.storage.AppVaultRepository
import com.example.storage.InstalledAppDetail
import com.example.storage.JsonStorageEngine
import com.example.storage.UserAndConfigRepository
import com.example.storage.VaultRepository
import com.example.storage.model.AppRemoteConfig
import com.example.storage.model.AppVaultItem
import com.example.storage.model.FileCategory
import com.example.storage.model.FileLocationState
import com.example.storage.model.UsageStatsRecord
import com.example.storage.model.UserProfile
import com.example.storage.model.VaultFileItem
import com.example.storage.model.VaultFolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.util.UUID
import javax.crypto.SecretKey

enum class AppDestination {
    OPENING,
    CALCULATOR,
    SECRET_GATE,
    SECRET_HUB
}

enum class SecretHubTab {
    VAULT_FILES,
    CLOUD_SYNC,
    APP_VAULT,
    USER_STATS,
    SETTINGS_CONFIG
}

data class CalculatorUiState(
    val ipInput: String = "192.168.1.1",
    val cidrInput: Int = 24,
    val calculationResult: SubnetResult? = null,
    val errorMessage: String? = null,
    // VLSM state
    val vlsmMajorIp: String = "192.168.0.0",
    val vlsmMajorCidr: Int = 24,
    val vlsmRequests: List<Pair<String, Int>> = listOf(
        "Sales LAN" to 50,
        "Engineering" to 25,
        "Server Farm" to 12,
        "WAN Link" to 2
    ),
    val vlsmResults: List<VlsmSubnet> = emptyList(),
    val vlsmError: String? = null,
    // IPv6 state
    val ipv6Input: String = "2001:db8:85a3::8a2e:370:7334",
    val ipv6Prefix: Int = 64,
    val ipv6Result: IPv6Result? = null,
    val ipv6Error: String? = null,
    // Active calc tab: 0: Subnet, 1: VLSM, 2: IPv6, 3: History
    val activeCalculatorTab: Int = 0,
    val historySearchQuery: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storageEngine = JsonStorageEngine(application)
    val userRepo = UserAndConfigRepository(application, storageEngine)
    val vaultRepo = VaultRepository(application, storageEngine)
    val appVaultRepo = AppVaultRepository(application, storageEngine)
    val cloudSyncManager = CloudSyncManager(application, vaultRepo)

    // Master Key derived from user profile or device Keystore
    private var cachedMasterKey: SecretKey? = null

    // Destination & Hub navigation
    private val _currentScreen = MutableStateFlow(AppDestination.CALCULATOR)
    val currentScreen: StateFlow<AppDestination> = _currentScreen.asStateFlow()

    private val _activeHubTab = MutableStateFlow(SecretHubTab.VAULT_FILES)
    val activeHubTab: StateFlow<SecretHubTab> = _activeHubTab.asStateFlow()

    // Calculator UI State
    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    // Secret Gate Lockout & Authentication state
    private val _isSecretSpaceUnlocked = MutableStateFlow(false)
    val isSecretSpaceUnlocked: StateFlow<Boolean> = _isSecretSpaceUnlocked.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _gateErrorMessage = MutableStateFlow<String?>(null)
    val gateErrorMessage: StateFlow<String?> = _gateErrorMessage.asStateFlow()

    private val _failedAttemptsCount = MutableStateFlow(0)
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    private val _lockoutRemainingSeconds = MutableStateFlow(0)
    val lockoutRemainingSeconds: StateFlow<Int> = _lockoutRemainingSeconds.asStateFlow()

    private var lockoutJob: Job? = null

    // First-Run Username Onboarding Dialog
    private val _showUsernamePrompt = MutableStateFlow(false)
    val showUsernamePrompt: StateFlow<Boolean> = _showUsernamePrompt.asStateFlow()

    private val _onboardingUsernameInput = MutableStateFlow("")
    val onboardingUsernameInput: StateFlow<String> = _onboardingUsernameInput.asStateFlow()

    private val _onboardingError = MutableStateFlow<String?>(null)
    val onboardingError: StateFlow<String?> = _onboardingError.asStateFlow()

    private val _privacyAgreed = MutableStateFlow(false)
    val privacyAgreed: StateFlow<Boolean> = _privacyAgreed.asStateFlow()

    // Private Vault state
    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _vaultSearchQuery = MutableStateFlow("")
    val vaultSearchQuery: StateFlow<String> = _vaultSearchQuery.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    private val _showTrashOnly = MutableStateFlow(false)
    val showTrashOnly: StateFlow<Boolean> = _showTrashOnly.asStateFlow()

    private val _previewFile = MutableStateFlow<File?>(null)
    val previewFile: StateFlow<File?> = _previewFile.asStateFlow()

    // App Vault installed apps
    private val _installedApps = MutableStateFlow<List<InstalledAppDetail>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppDetail>> = _installedApps.asStateFlow()

    private val _appVaultSearchQuery = MutableStateFlow("")
    val appVaultSearchQuery: StateFlow<String> = _appVaultSearchQuery.asStateFlow()

    // Status snackbar / toast message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Filtered Vault Files
    val filteredVaultFiles: StateFlow<List<VaultFileItem>> = combine(
        vaultRepo.registryFlow,
        _selectedCategory,
        _vaultSearchQuery,
        _currentFolderId,
        _showTrashOnly
    ) { registry, category, query, folderId, trashOnly ->
        registry.files.filter { item ->
            val matchesTrash = if (trashOnly) item.isTrash else !item.isTrash
            val matchesFolder = if (trashOnly) true else item.folderId == folderId
            val matchesCat = if (category == null) true else item.category == category.name
            val matchesQuery = if (query.isBlank()) true else {
                item.originalDisplayName.contains(query, ignoreCase = true)
            }
            matchesTrash && matchesFolder && matchesCat && matchesQuery
        }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial calculation
        performSubnetCalculation("192.168.1.1", 24)
        performVlsmCalculation()
        performIpv6Calculation("2001:db8:85a3::8a2e:370:7334", 64)

        // Check if username onboarding is needed
        val user = userRepo.userProfileFlow.value
        if (user.username.isEmpty() || !user.privacyAccepted) {
            _showUsernamePrompt.value = true
        } else {
            initMasterKey(user.username, user.deviceId)
        }

        // Check opening config
        val config = userRepo.remoteConfigFlow.value
        if (config.openingMode != "NONE") {
            _currentScreen.value = AppDestination.OPENING
        }

        // Load installed launchable apps for App Vault
        loadInstalledApps()
    }

    private fun initMasterKey(username: String, deviceId: String) {
        val seed = "$username-$deviceId-NetVault-SecretSalt-2026"
        val salt = seed.toByteArray(Charsets.UTF_8).take(16).toByteArray()
        val paddedSalt = if (salt.size < 16) salt + ByteArray(16 - salt.size) else salt
        cachedMasterKey = CryptoEngine.deriveKeyFromSecret(seed, paddedSalt)
    }

    fun getMasterKey(): SecretKey {
        return cachedMasterKey ?: run {
            val user = userRepo.userProfileFlow.value
            initMasterKey(user.username.ifEmpty { "default_user" }, user.deviceId)
            cachedMasterKey!!
        }
    }

    // Navigation
    fun navigateTo(dest: AppDestination) {
        if (dest == AppDestination.SECRET_HUB && !_isSecretSpaceUnlocked.value) {
            _currentScreen.value = AppDestination.SECRET_GATE
        } else {
            _currentScreen.value = dest
        }
    }

    fun setHubTab(tab: SecretHubTab) {
        _activeHubTab.value = tab
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    // Opening
    fun skipOpening() {
        _currentScreen.value = AppDestination.CALCULATOR
    }

    // Onboarding / Login
    fun setOnboardingUsername(input: String) {
        _onboardingUsernameInput.value = input
        _onboardingError.value = null
    }

    fun setPrivacyAgreed(agreed: Boolean) {
        _privacyAgreed.value = agreed
    }

    fun submitUsernameOnboarding() {
        val validation = userRepo.validateUsername(_onboardingUsernameInput.value)
        if (validation.isFailure) {
            _onboardingError.value = validation.exceptionOrNull()?.message
            return
        }
        if (!_privacyAgreed.value) {
            _onboardingError.value = "Anda wajib menyetujui kebijakan privasi untuk melanjutkan."
            return
        }

        val validUsername = validation.getOrThrow()
        val currentProfile = userRepo.userProfileFlow.value
        val updated = currentProfile.copy(
            username = validUsername,
            privacyAccepted = true,
            registeredAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        userRepo.saveUserProfile(updated)
        initMasterKey(validUsername, currentProfile.deviceId)
        _showUsernamePrompt.value = false
        _toastMessage.value = "Selamat datang, $validUsername! NetVault siap digunakan."

        // Attempt online check / registration in background
        viewModelScope.launch {
            try {
                val res = ApiClient.api.authenticate(
                    AuthRequest(
                        username = validUsername,
                        deviceId = currentProfile.deviceId
                    )
                )
                if (res.isSuccessful && res.body()?.sessionToken != null) {
                    val body = res.body()!!
                    userRepo.saveUserProfile(
                        userRepo.userProfileFlow.value.copy(
                            sessionToken = body.sessionToken,
                            role = body.role
                        )
                    )
                }
            } catch (_: Exception) {
                // Offline fallback - allowed as per requirement #5
            }
        }
    }

    // Calculator operations
    fun setIpInput(ip: String) {
        _calcState.value = _calcState.value.copy(ipInput = ip, errorMessage = null)
    }

    fun setCidrInput(cidr: Int) {
        _calcState.value = _calcState.value.copy(cidrInput = cidr)
        performSubnetCalculation(_calcState.value.ipInput, cidr)
    }

    fun performSubnetCalculation(ip: String = _calcState.value.ipInput, cidr: Int = _calcState.value.cidrInput) {
        val result = SubnetCalculator.calculate(ip, cidr)
        if (result.isSuccess) {
            val subnetRes = result.getOrThrow()
            _calcState.value = _calcState.value.copy(
                calculationResult = subnetRes,
                errorMessage = null
            )
            // Add to history
            val historyItem = CalculationHistoryItem(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                ipWithCidr = "${subnetRes.inputIp}/${subnetRes.cidr}",
                networkAddress = subnetRes.networkAddress,
                usableRange = subnetRes.ipRange,
                usableHosts = subnetRes.usableHosts,
                subnetMask = subnetRes.subnetMask
            )
            userRepo.addCalculationHistory(historyItem)
        } else {
            _calcState.value = _calcState.value.copy(
                calculationResult = null,
                errorMessage = result.exceptionOrNull()?.message ?: "Perhitungan gagal"
            )
        }
    }

    fun setActiveCalculatorTab(tabIndex: Int) {
        _calcState.value = _calcState.value.copy(activeCalculatorTab = tabIndex)
    }

    fun setHistorySearchQuery(query: String) {
        _calcState.value = _calcState.value.copy(historySearchQuery = query)
    }

    // VLSM operations
    fun setVlsmMajorNetwork(ip: String, cidr: Int) {
        _calcState.value = _calcState.value.copy(vlsmMajorIp = ip, vlsmMajorCidr = cidr)
    }

    fun addVlsmSubnetRequest(name: String, hosts: Int) {
        val current = _calcState.value.vlsmRequests
        _calcState.value = _calcState.value.copy(vlsmRequests = current + (name to hosts))
        performVlsmCalculation()
    }

    fun removeVlsmSubnetRequest(index: Int) {
        val current = _calcState.value.vlsmRequests.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _calcState.value = _calcState.value.copy(vlsmRequests = current)
            performVlsmCalculation()
        }
    }

    fun performVlsmCalculation() {
        val state = _calcState.value
        val res = SubnetCalculator.calculateVlsm(
            majorNetwork = state.vlsmMajorIp,
            majorCidr = state.vlsmMajorCidr,
            subnetsNeeded = state.vlsmRequests
        )
        if (res.isSuccess) {
            _calcState.value = _calcState.value.copy(vlsmResults = res.getOrThrow(), vlsmError = null)
        } else {
            _calcState.value = _calcState.value.copy(vlsmError = res.exceptionOrNull()?.message)
        }
    }

    // IPv6 operations
    fun setIpv6Input(ipv6: String, prefix: Int = _calcState.value.ipv6Prefix) {
        _calcState.value = _calcState.value.copy(ipv6Input = ipv6, ipv6Prefix = prefix)
        performIpv6Calculation(ipv6, prefix)
    }

    fun performIpv6Calculation(ipv6: String = _calcState.value.ipv6Input, prefix: Int = _calcState.value.ipv6Prefix) {
        val res = SubnetCalculator.calculateIpv6(ipv6, prefix)
        if (res.isSuccess) {
            _calcState.value = _calcState.value.copy(ipv6Result = res.getOrThrow(), ipv6Error = null)
        } else {
            _calcState.value = _calcState.value.copy(ipv6Error = res.exceptionOrNull()?.message)
        }
    }

    // Secret Gate: PIN & Biometrics
    fun onPinDigit(digit: String) {
        if (_lockoutRemainingSeconds.value > 0) return
        if (_pinInput.value.length < 6) {
            val updated = _pinInput.value + digit
            _pinInput.value = updated
            _gateErrorMessage.value = null
            if (updated.length == 6) {
                verifyPinSubmission(updated)
            }
        }
    }

    fun onPinBackspace() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _gateErrorMessage.value = null
        }
    }

    fun onPinClear() {
        _pinInput.value = ""
        _gateErrorMessage.value = null
    }

    private fun verifyPinSubmission(pin: String) {
        if (!userRepo.hasPinSet()) {
            // First time setup: set this PIN
            userRepo.setPin(pin)
            _isSecretSpaceUnlocked.value = true
            _currentScreen.value = AppDestination.SECRET_HUB
            _pinInput.value = ""
            _toastMessage.value = "PIN Ruang Rahasia berhasil dibuat!"
            return
        }

        if (userRepo.verifyPin(pin)) {
            _isSecretSpaceUnlocked.value = true
            _failedAttemptsCount.value = 0
            _currentScreen.value = AppDestination.SECRET_HUB
            _pinInput.value = ""
            _gateErrorMessage.value = null
        } else {
            val failed = _failedAttemptsCount.value + 1
            _failedAttemptsCount.value = failed
            _pinInput.value = ""
            if (failed >= 5) {
                startLockoutTimer(30)
                _gateErrorMessage.value = "PIN salah 5x. Akses terkunci selama 30 detik demi keamanan."
            } else {
                _gateErrorMessage.value = "PIN salah! Sisa percobaan: ${5 - failed}"
            }
        }
    }

    private fun startLockoutTimer(seconds: Int) {
        lockoutJob?.cancel()
        _lockoutRemainingSeconds.value = seconds
        lockoutJob = viewModelScope.launch {
            var s = seconds
            while (s > 0) {
                delay(1000)
                s--
                _lockoutRemainingSeconds.value = s
            }
            _failedAttemptsCount.value = 0
            _gateErrorMessage.value = null
        }
    }

    fun unlockWithBiometricSuccess() {
        _isSecretSpaceUnlocked.value = true
        _failedAttemptsCount.value = 0
        _currentScreen.value = AppDestination.SECRET_HUB
        _pinInput.value = ""
        _gateErrorMessage.value = null
        userRepo.logAuditEvent("BIOMETRIC_AUTH_SUCCESS", "Autentikasi biometrik Ruang Rahasia berhasil.")
    }

    fun lockSecretSpace() {
        _isSecretSpaceUnlocked.value = false
        _pinInput.value = ""
        _currentScreen.value = AppDestination.CALCULATOR
        userRepo.logAuditEvent("VAULT_LOCKED", "Ruang Rahasia dikunci.")
    }

    // Vault File Operations
    fun setSelectedCategory(category: FileCategory?) {
        _selectedCategory.value = category
    }

    fun setVaultSearchQuery(q: String) {
        _vaultSearchQuery.value = q
    }

    fun setCurrentFolder(folderId: String?) {
        _currentFolderId.value = folderId
    }

    fun setShowTrashOnly(trash: Boolean) {
        _showTrashOnly.value = trash
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            val key = getMasterKey()
            val result = vaultRepo.importFile(uri, _currentFolderId.value, key)
            if (result.isSuccess) {
                val item = result.getOrThrow()
                _toastMessage.value = "Berkas '${item.originalDisplayName}' berhasil dienkripsi dan disimpan di Vault."
                userRepo.logAuditEvent("FILE_IMPORTED", "Berkas diimpor: ${item.originalDisplayName} (${item.sizeBytes} B)")
            } else {
                _toastMessage.value = "Gagal mengimpor: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun exportVaultFile(fileItem: VaultFileItem, outputStream: OutputStream) {
        viewModelScope.launch {
            val key = getMasterKey()
            val res = vaultRepo.exportFile(fileItem, outputStream, key)
            if (res.isSuccess) {
                _toastMessage.value = "Berkas '${fileItem.originalDisplayName}' berhasil diekspor."
                userRepo.logAuditEvent("FILE_EXPORTED", "Berkas diekspor: ${fileItem.originalDisplayName}")
            } else {
                _toastMessage.value = "Gagal ekspor: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun previewVaultFile(fileItem: VaultFileItem) {
        viewModelScope.launch {
            val key = getMasterKey()
            val res = vaultRepo.getDecryptedPreviewFile(fileItem, key)
            if (res.isSuccess) {
                _previewFile.value = res.getOrThrow()
            } else {
                _toastMessage.value = "Tidak dapat mempratinjau: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun closePreview() {
        _previewFile.value = null
    }

    fun moveFileToTrash(fileId: String) {
        vaultRepo.moveToTrash(fileId)
        _toastMessage.value = "Berkas dipindahkan ke Tempat Sampah."
        userRepo.logAuditEvent("FILE_TRASHED", "File ID $fileId dipindahkan ke Tempat Sampah.")
    }

    fun restoreFileFromTrash(fileId: String) {
        vaultRepo.restoreFromTrash(fileId)
        _toastMessage.value = "Berkas dipulihkan dari Tempat Sampah."
        userRepo.logAuditEvent("FILE_RESTORED", "File ID $fileId dipulihkan.")
    }

    fun permanentlyDeleteFile(fileId: String) {
        vaultRepo.permanentlyDeleteFile(fileId)
        _toastMessage.value = "Berkas dihapus permanen dari perangkat."
        userRepo.logAuditEvent("FILE_PERM_DELETED", "File ID $fileId dihapus permanen.")
    }

    fun emptyTrash() {
        val count = vaultRepo.emptyTrash()
        _toastMessage.value = "$count berkas di tempat sampah berhasil dibersihkan."
        userRepo.logAuditEvent("TRASH_EMPTIED", "$count berkas di tempat sampah dibersihkan permanen.")
    }

    fun createFolder(name: String) {
        vaultRepo.createFolder(name, _currentFolderId.value)
        _toastMessage.value = "Folder '$name' berhasil dibuat."
    }

    fun renameFile(fileId: String, newName: String) {
        vaultRepo.renameFile(fileId, newName)
        _toastMessage.value = "Nama berkas diubah menjadi '$newName'."
    }

    // Cloud Sync Operations
    fun uploadToCloud(fileItem: VaultFileItem) {
        val user = userRepo.userProfileFlow.value
        val token = user.sessionToken ?: "demo_token_${user.username}"
        viewModelScope.launch {
            val res = cloudSyncManager.uploadFileToCloud(fileItem, token)
            if (res.isSuccess) {
                userRepo.logCloudTransfer(isUpload = true, bytes = fileItem.sizeBytes)
                userRepo.logAuditEvent("CLOUD_UPLOAD_SUCCESS", "Upload berhasil: ${fileItem.originalDisplayName}")
                _toastMessage.value = "Upload ke cloud selesai & diverifikasi."
            } else {
                _toastMessage.value = "Gagal upload: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun downloadFromCloud(fileItem: VaultFileItem) {
        val user = userRepo.userProfileFlow.value
        val token = user.sessionToken ?: "demo_token_${user.username}"
        viewModelScope.launch {
            val key = getMasterKey()
            val res = cloudSyncManager.downloadFileFromCloud(fileItem, token, key)
            if (res.isSuccess) {
                userRepo.logCloudTransfer(isUpload = false, bytes = fileItem.sizeBytes)
                userRepo.logAuditEvent("CLOUD_DOWNLOAD_SUCCESS", "Download berhasil: ${fileItem.originalDisplayName}")
                _toastMessage.value = "Unduhan cloud selesai & integritas terverifikasi."
            } else {
                _toastMessage.value = "Gagal unduh: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    // App Vault
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = appVaultRepo.getInstalledLaunchableApps()
        }
    }

    fun setAppVaultSearchQuery(q: String) {
        _appVaultSearchQuery.value = q
    }

    fun addAppToVault(app: InstalledAppDetail) {
        appVaultRepo.addAppToVault(app.packageName, app.appName)
        _toastMessage.value = "'${app.appName}' ditambahkan ke App Vault."
        userRepo.logAuditEvent("APP_VAULT_ADDED", "Aplikasi ditambahkan ke vault: ${app.packageName}")
    }

    fun removeAppFromVault(packageName: String) {
        appVaultRepo.removeAppFromVault(packageName)
        _toastMessage.value = "Aplikasi dihapus dari App Vault."
        userRepo.logAuditEvent("APP_VAULT_REMOVED", "Aplikasi dihapus dari vault: $packageName")
    }

    fun launchVaultApp(packageName: String) {
        val res = appVaultRepo.launchApp(packageName)
        if (res.isFailure) {
            _toastMessage.value = "Gagal membuka aplikasi: ${res.exceptionOrNull()?.message}"
        } else {
            userRepo.logAuditEvent("APP_LAUNCHED_FROM_VAULT", "Membuka aplikasi dari Vault: $packageName")
        }
    }

    fun updateAppNotificationPolicy(packageName: String, policy: String) {
        appVaultRepo.updateNotificationPolicy(packageName, policy)
        _toastMessage.value = "Kebijakan notifikasi diperbarui: $policy"
    }

    // Settings & Global Config
    fun updateRemoteConfigFromUser(newConfig: AppRemoteConfig) {
        userRepo.saveRemoteConfig(newConfig)
        _toastMessage.value = "Pengaturan aplikasi berhasil disimpan."
    }

    fun elevateToAdmin(key: String): Boolean {
        return userRepo.promoteToAdmin(key)
    }
}
