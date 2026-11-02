package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.network.SyncState
import com.example.storage.InstalledAppDetail
import com.example.storage.model.AppRemoteConfig
import com.example.storage.model.FileCategory
import com.example.storage.model.FileLocationState
import com.example.storage.model.VaultFileItem
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.SecretHubTab
import com.example.ui.components.StorageGaugeCard
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretSpaceHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeHubTab.collectAsState()
    val userProfile by viewModel.userRepo.userProfileFlow.collectAsState()

    BackHandler {
        viewModel.lockSecretSpace()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ruang Rahasia NetVault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "User: ${userProfile.username} (${userProfile.role})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.lockSecretSpace() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kunci & Keluar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.lockSecretSpace() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Kunci Vault", tint = RoseError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = activeTab == SecretHubTab.VAULT_FILES,
                    onClick = { viewModel.setHubTab(SecretHubTab.VAULT_FILES) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
                    label = { Text("Vault") }
                )
                NavigationBarItem(
                    selected = activeTab == SecretHubTab.CLOUD_SYNC,
                    onClick = { viewModel.setHubTab(SecretHubTab.CLOUD_SYNC) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Cloud") },
                    label = { Text("Cloud") }
                )
                NavigationBarItem(
                    selected = activeTab == SecretHubTab.APP_VAULT,
                    onClick = { viewModel.setHubTab(SecretHubTab.APP_VAULT) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "App Vault") },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = activeTab == SecretHubTab.USER_STATS,
                    onClick = { viewModel.setHubTab(SecretHubTab.USER_STATS) },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = "Statistik") },
                    label = { Text("Statistik") }
                )
                NavigationBarItem(
                    selected = activeTab == SecretHubTab.SETTINGS_CONFIG,
                    onClick = { viewModel.setHubTab(SecretHubTab.SETTINGS_CONFIG) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Setelan") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                SecretHubTab.VAULT_FILES -> PrivateVaultTab(viewModel = viewModel)
                SecretHubTab.CLOUD_SYNC -> CloudSyncTab(viewModel = viewModel)
                SecretHubTab.APP_VAULT -> AppVaultTab(viewModel = viewModel)
                SecretHubTab.USER_STATS -> AdminStatsTab(viewModel = viewModel)
                SecretHubTab.SETTINGS_CONFIG -> SettingsConfigTab(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivateVaultTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val registry by viewModel.vaultRepo.registryFlow.collectAsState()
    val files by viewModel.filteredVaultFiles.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.vaultSearchQuery.collectAsState()
    val showTrashOnly by viewModel.showTrashOnly.collectAsState()
    val previewFile by viewModel.previewFile.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var fileToRename by remember { mutableStateOf<VaultFileItem?>(null) }
    var newFileNameInput by remember { mutableStateOf("") }
    var fileToDeletePermanent by remember { mutableStateOf<VaultFileItem?>(null) }

    // Android SAF File Picker / Photo Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.importFileFromUri(uri)
        }
    }

    // SAF Document Export Launcher
    var pendingExportFile by remember { mutableStateOf<VaultFileItem?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null && pendingExportFile != null) {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                viewModel.exportVaultFile(pendingExportFile!!, os)
            }
            pendingExportFile = null
        }
    }

    val usedBytes = registry.files.filter { !it.isTrash }.sumOf { it.sizeBytes }

    Scaffold(
        floatingActionButton = {
            if (!showTrashOnly) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("import_file_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Impor Berkas ke Vault")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Storage Gauge
                StorageGaugeCard(
                    usedBytes = usedBytes,
                    totalBytes = registry.maxQuotaBytes,
                    fileCount = registry.files.filter { !it.isTrash }.size
                )
            }

            // Search and Trash toggles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setVaultSearchQuery(it) },
                        placeholder = { Text("Cari berkas di Vault...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Buat Folder")
                    }
                    IconButton(
                        onClick = { viewModel.setShowTrashOnly(!showTrashOnly) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showTrashOnly) RoseError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Tempat Sampah",
                            tint = if (showTrashOnly) RoseError else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Trash Banner if active
            if (showTrashOnly) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tempat Sampah (Recycle Bin)",
                                fontWeight = FontWeight.Bold,
                                color = RoseError
                            )
                            Button(
                                onClick = { viewModel.emptyTrash() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                            ) {
                                Text("Kosongkan Sampah")
                            }
                        }
                    }
                }
            } else {
                // Category Filter Chips
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) },
                            label = { Text("Semua") }
                        )
                        FileCategory.values().forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.setSelectedCategory(cat) },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }

            // File items
            if (files.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showTrashOnly) "Tempat sampah kosong." else "Vault kosong. Tekan tombol + untuk mengimpor berkas privat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(files, key = { it.id }) { item ->
                    VaultFileCard(
                        item = item,
                        isTrashMode = showTrashOnly,
                        onPreview = { viewModel.previewVaultFile(item) },
                        onExport = {
                            pendingExportFile = item
                            exportLauncher.launch(item.originalDisplayName)
                        },
                        onRename = {
                            fileToRename = item
                            newFileNameInput = item.originalDisplayName
                        },
                        onTrash = { viewModel.moveFileToTrash(item.id) },
                        onRestore = { viewModel.restoreFileFromTrash(item.id) },
                        onPermanentDelete = { fileToDeletePermanent = item },
                        onUpload = { viewModel.uploadToCloud(item) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }

    // Dialog: Create Folder
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Buat Folder Baru") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nama Folder") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName)
                        newFolderName = ""
                        showCreateFolderDialog = false
                    }
                }) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog: Rename File
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Ubah Nama Berkas") },
            text = {
                OutlinedTextField(
                    value = newFileNameInput,
                    onValueChange = { newFileNameInput = it },
                    label = { Text("Nama Baru") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileNameInput.isNotBlank()) {
                        viewModel.renameFile(fileToRename!!.id, newFileNameInput)
                        fileToRename = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) { Text("Batal") }
            }
        )
    }

    // Dialog: Permanent Delete Confirmation
    if (fileToDeletePermanent != null) {
        AlertDialog(
            onDismissRequest = { fileToDeletePermanent = null },
            title = { Text("Hapus Permanen?") },
            text = {
                Text(
                    "Berkas '${fileToDeletePermanent!!.originalDisplayName}' akan dihapus secara permanen dari perangkat. Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteFile(fileToDeletePermanent!!.id)
                        fileToDeletePermanent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) { Text("Hapus Permanen") }
            },
            dismissButton = {
                TextButton(onClick = { fileToDeletePermanent = null }) { Text("Batal") }
            }
        )
    }

    // Dialog: Preview File
    if (previewFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closePreview() },
            title = { Text("Pratinjau Berkas") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val ext = previewFile!!.extension.lowercase()
                    if (ext in listOf("jpg", "jpeg", "png", "gif", "webp")) {
                        AsyncImage(
                            model = previewFile,
                            contentDescription = "Pratinjau Gambar",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else if (ext in listOf("txt", "json", "log", "md")) {
                        val text = try { previewFile!!.readText().take(2000) } catch (_: Exception) { "Gagal membaca teks" }
                        Text(text = text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(text = "Pratinjau langsung tidak tersedia untuk format ini ($ext). Gunakan opsi 'Ekspor / Bagikan' untuk membuka dengan aplikasi eksternal.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.closePreview() }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun VaultFileCard(
    item: VaultFileItem,
    isTrashMode: Boolean,
    onPreview: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onUpload: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val sizeKb = item.sizeBytes / 1024

    val categoryIcon = when (item.category) {
        FileCategory.PHOTO.name -> Icons.Default.Image
        FileCategory.VIDEO.name -> Icons.Default.Videocam
        FileCategory.DOCUMENT.name -> Icons.Default.Description
        FileCategory.TEXT.name -> Icons.Default.InsertDriveFile
        else -> Icons.Default.InsertDriveFile
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$sizeKb KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = dateFormat.format(Date(item.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Cloud Status Chip
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    val (statusText, statusColor) = when (item.locationState) {
                        FileLocationState.SYNCED_BOTH.name -> "Tersinkron Cloud" to EmeraldSuccess
                        FileLocationState.CLOUD_ONLY.name -> "Hanya di Cloud" to CyanNeon
                        else -> "Hanya Lokal" to MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu Berkas")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (isTrashMode) {
                        DropdownMenuItem(
                            text = { Text("Pulihkan") },
                            onClick = { onRestore(); menuExpanded = false },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Permanen", color = RoseError) },
                            onClick = { onPermanentDelete(); menuExpanded = false },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseError) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Pratinjau") },
                            onClick = { onPreview(); menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor / Bagikan") },
                            onClick = { onExport(); menuExpanded = false },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Upload ke Cloud") },
                            onClick = { onUpload(); menuExpanded = false },
                            leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ubah Nama") },
                            onClick = { onRename(); menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Pindahkan ke Sampah", color = RoseError) },
                            onClick = { onTrash(); menuExpanded = false },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncTab(viewModel: MainViewModel) {
    val syncState by viewModel.cloudSyncManager.syncState.collectAsState()
    val registry by viewModel.vaultRepo.registryFlow.collectAsState()
    var wifiOnly by remember { mutableStateOf(viewModel.cloudSyncManager.wifiOnly) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sinkronisasi Cloud Privat Tanpa SQL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "File diunggah dalam keadaan terenkripsi AES-256. Metadata disimpan dalam format JSON atomik di server privat tanpa database SQL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Hanya Sinkron via Wi-Fi", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = {
                                wifiOnly = it
                                viewModel.cloudSyncManager.wifiOnly = it
                            }
                        )
                    }

                    // Sync State Status Banner
                    when (val s = syncState) {
                        is SyncState.Syncing -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Menyinkronkan: ${s.currentFile} (${s.progressPercent}%)",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is SyncState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Text(text = s.message, color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is SyncState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RoseError.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Text(text = s.errorMessage, color = RoseError, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        SyncState.Idle -> {}
                    }

                    Button(
                        onClick = {
                            val localOnlyFiles = registry.files.filter { !it.isTrash && it.locationState == FileLocationState.LOCAL_ONLY.name }
                            localOnlyFiles.forEach { viewModel.uploadToCloud(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sinkronkan Semua Berkas Lokal")
                    }
                }
            }
        }

        item {
            Text(
                text = "Daftar Berkas Cloud (${registry.files.count { it.locationState == FileLocationState.SYNCED_BOTH.name }})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(registry.files.filter { !it.isTrash }) { file ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = file.originalDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "SHA-256: ${file.sha256Checksum.take(16)}...", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Status: ${file.locationState}", style = MaterialTheme.typography.labelSmall, color = if (file.locationState == FileLocationState.SYNCED_BOTH.name) EmeraldSuccess else MaterialTheme.colorScheme.primary)
                    }

                    if (file.locationState == FileLocationState.LOCAL_ONLY.name) {
                        IconButton(onClick = { viewModel.uploadToCloud(file) }) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { viewModel.downloadFromCloud(file) }) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Unduh Ulang", tint = EmeraldSuccess)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppVaultTab(viewModel: MainViewModel) {
    val vaultApps by viewModel.appVaultRepo.vaultAppsFlow.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val searchQuery by viewModel.appVaultSearchQuery.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val compatibilityInfo = remember { viewModel.appVaultRepo.getDeviceSandboxCompatibilityInfo() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Honest Android Sandboxing Disclosure Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Transparansi Arsitektur Keamanan Android",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = compatibilityInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Catatan Teknis: NetVault melindungi pembukaan aplikasi dengan gerbang autentikasi. Sesuai kebijakan keamanan Android, aplikasi tidak dapat dihilangkan secara sepihak dari Pengaturan Sistem tanpa interaksi sistem / root.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aplikasi Terproteksi (${vaultApps.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Aplikasi")
                }
            }
        }

        if (vaultApps.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada aplikasi yang ditambahkan ke App Vault.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(vaultApps) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Notifikasi: ${app.notificationPolicy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Row {
                            IconButton(onClick = { viewModel.launchVaultApp(app.packageName) }) {
                                Icon(Icons.Default.Launch, contentDescription = "Buka", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.removeAppFromVault(app.packageName) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RoseError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val filtered = installedApps.filter {
            searchQuery.isBlank() ||
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pilih Aplikasi Terpasang") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setAppVaultSearchQuery(it) },
                        placeholder = { Text("Cari aplikasi...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addAppToVault(app)
                                        showAddDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(text = app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun AdminStatsTab(viewModel: MainViewModel) {
    val userProfile by viewModel.userRepo.userProfileFlow.collectAsState()
    val stats by viewModel.userRepo.usageStatsFlow.collectAsState()
    var adminKeyInput by remember { mutableStateOf("") }
    var elevationError by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (userProfile.role != "ADMIN" && userProfile.role != "OWNER") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Akses Khusus Administrator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Menu statistik dan pemantauan pengguna hanya dapat diakses oleh peran Administrator yang terautentikasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = adminKeyInput,
                            onValueChange = { adminKeyInput = it },
                            label = { Text("Kunci Otorisasi Admin") },
                            placeholder = { Text("ADMIN_NETVAULT_SECRET_2026") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (elevationError != null) {
                            Text(text = elevationError!!, color = RoseError, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val success = viewModel.elevateToAdmin(adminKeyInput)
                                if (!success) {
                                    elevationError = "Kunci otorisasi administrator tidak valid!"
                                } else {
                                    elevationError = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verifikasi Hak Administrator")
                        }
                    }
                }
            }
        } else {
            // Admin Metrics Dashboard
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Metrik Aktivitas Pengguna (Agregat)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Pengguna Terdaftar:", style = MaterialTheme.typography.bodySmall)
                            Text("${stats.registeredUsersCount}", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pengguna Aktif (DAU/WAU/MAU):", style = MaterialTheme.typography.bodySmall)
                            Text("${stats.activeUsersDaily} / ${stats.activeUsersWeekly} / ${stats.activeUsersMonthly}", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Perhitungan Subnet:", style = MaterialTheme.typography.bodySmall)
                            Text("${stats.totalCalculations} kali", fontWeight = FontWeight.Bold, color = CyanNeon)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aktivitas Cloud Upload / Download:", style = MaterialTheme.typography.bodySmall)
                            Text("${stats.totalCloudUploads} up / ${stats.totalCloudDownloads} down", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume Data Cloud Terenkripsi:", style = MaterialTheme.typography.bodySmall)
                            Text("${stats.totalCloudBytes / 1024} KB", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Transparent Privacy & Retention Policy
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Transparansi Kebijakan & Retensi Data", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "1. Tidak ada isi berkas, foto, video, atau dokumen yang pernah dicatat ke analitik atau log.\n2. Alamat IP bersifat dinamis dan hanya dipakai server saat verifikasi transmisi.\n3. Log keamanan disimpan maksimal 90 hari untuk tujuan audit operasional.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Security Audit Logs
            item {
                Text(
                    text = "Riwayat Aktivitas Keamanan (Audit Log)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(stats.auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.eventType, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = log.details, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Pengguna: ${log.username} (${log.ipAddressObserved})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsConfigTab(viewModel: MainViewModel) {
    val config by viewModel.userRepo.remoteConfigFlow.collectAsState()
    val userProfile by viewModel.userRepo.userProfileFlow.collectAsState()

    var appTitleInput by remember { mutableStateOf(config.homeTitle) }
    var announcementInput by remember { mutableStateOf(config.announcementMessage) }
    var openingMode by remember { mutableStateOf(config.openingMode) }
    var openingDurationInput by remember { mutableStateOf(config.openingDurationMs.toString()) }
    var biometricEnabled by remember { mutableStateOf(userProfile.isBiometricEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Konfigurasi Identitas & Tampilan Aplikasi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = appTitleInput,
                        onValueChange = { appTitleInput = it },
                        label = { Text("Judul Halaman Utama") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = announcementInput,
                        onValueChange = { announcementInput = it },
                        label = { Text("Pesan Pengumuman Global") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Pengaturan Opening / Intro Startup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NONE", "IMAGE", "ANIMATION").forEach { mode ->
                            FilterChip(
                                selected = openingMode == mode,
                                onClick = { openingMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = openingDurationInput,
                        onValueChange = { openingDurationInput = it },
                        label = { Text("Durasi Opening (milidetik)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val duration = openingDurationInput.toLongOrNull() ?: 2500L
                            val updated = config.copy(
                                homeTitle = appTitleInput,
                                announcementMessage = announcementInput,
                                openingMode = openingMode,
                                openingDurationMs = duration,
                                lastUpdated = System.currentTimeMillis()
                            )
                            viewModel.updateRemoteConfigFromUser(updated)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Konfigurasi")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pengaturan Keamanan & Kredensial",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Buka Ruang Rahasia dengan Biometrik", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                viewModel.userRepo.setBiometricEnabled(it)
                            }
                        )
                    }

                    Text(
                        text = "Device ID Unik: ${userProfile.deviceId}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
