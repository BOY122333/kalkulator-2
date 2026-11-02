package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.components.BinaryOctetCard
import com.example.ui.components.SubnetMetricItem
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calcState by viewModel.calcState.collectAsState()
    val remoteConfig by viewModel.userRepo.remoteConfigFlow.collectAsState()
    val historyList by viewModel.userRepo.calcHistoryFlow.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val tabTitles = listOf("Subnet IPv4", "VLSM Divider", "IPv6 Explorer", "Riwayat")
    val tabIcons = listOf(Icons.Default.Calculate, Icons.Default.Route, Icons.Default.Public, Icons.Default.History)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = remoteConfig.homeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kalkulator Jaringan & CIDR Lengkap",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Secret Space Entry Button
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.SECRET_GATE) },
                        modifier = Modifier
                            .testTag("secret_vault_button")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Buka Ruang Rahasia",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global Announcement Banner if active
            if (remoteConfig.announcementMessage.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = remoteConfig.announcementMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Primary Tabs
            PrimaryTabRow(
                selectedTabIndex = calcState.activeCalculatorTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = calcState.activeCalculatorTab == index,
                        onClick = { viewModel.setActiveCalculatorTab(index) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (calcState.activeCalculatorTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = title,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            when (calcState.activeCalculatorTab) {
                0 -> SubnetCalculatorTab(viewModel = viewModel)
                1 -> VlsmCalculatorTab(viewModel = viewModel)
                2 -> Ipv6ExplorerTab(viewModel = viewModel)
                3 -> HistoryTab(viewModel = viewModel, history = historyList)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubnetCalculatorTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val state by viewModel.calcState.collectAsState()
    val result = state.calculationResult
    val keyboardController = LocalSoftwareKeyboardController.current

    var dropdownExpanded by remember { mutableStateOf(false) }

    val presets = listOf("192.168.1.1", "10.0.0.1", "172.16.0.1", "8.8.8.8", "127.0.0.1", "169.254.1.1")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Quick Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable {
                                viewModel.setIpInput(preset)
                                viewModel.performSubnetCalculation(preset, state.cidrInput)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Input Alamat IP & Prefix CIDR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = state.ipInput,
                        onValueChange = { viewModel.setIpInput(it) },
                        label = { Text("Alamat IPv4") },
                        placeholder = { Text("Contoh: 192.168.1.1") },
                        isError = state.errorMessage != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            viewModel.performSubnetCalculation()
                        }),
                        trailingIcon = {
                            if (state.ipInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setIpInput("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ip_input_field")
                    )

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage!!,
                            color = RoseError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // CIDR Selector with Dropdown and Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "/${state.cidrInput} (${result?.subnetMask ?: "Subnet"})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Subnet Mask (CIDR)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                (32 downTo 0).forEach { prefix ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "/$prefix (${com.example.calculator.engine.SubnetCalculator.longToIpv4(com.example.calculator.engine.SubnetCalculator.cidrToMask(prefix))})",
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            viewModel.setCidrInput(prefix)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Quick slider for /0 to /32
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Geser Prefix", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "/${state.cidrInput}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = state.cidrInput.toFloat(),
                            onValueChange = { viewModel.setCidrInput(it.toInt()) },
                            valueRange = 0f..32f,
                            steps = 31,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.performSubnetCalculation()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hitung Subnet Sekarang")
                    }
                }
            }
        }

        // Calculation Results Section
        if (result != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hasil Perhitungan Subnet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val summary = buildString {
                                appendLine("--- HASIL NETVAULT IP CALCULATOR ---")
                                appendLine("IP Input: ${result.inputIp}/${result.cidr}")
                                appendLine("Network Address: ${result.networkAddress}")
                                appendLine("Subnet Mask: ${result.subnetMask}")
                                appendLine("Wildcard Mask: ${result.wildcardMask}")
                                appendLine("Broadcast Address: ${result.broadcastAddress}")
                                appendLine("First Usable IP: ${result.firstUsableIp}")
                                appendLine("Last Usable IP: ${result.lastUsableIp}")
                                appendLine("Rentang Host: ${result.ipRange}")
                                appendLine("Jumlah Host Usable: ${result.usableHosts}")
                                appendLine("Total Alamat: ${result.totalIps}")
                                appendLine("Tipe Alamat: ${result.addressType}")
                                appendLine("Kelas Historis: ${result.ipClass}")
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("NetVault Subnet Result", summary))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin Hasil")
                        }

                        IconButton(onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Hasil Subnet NetVault - ${result.inputIp}/${result.cidr}")
                                putExtra(Intent.EXTRA_TEXT, "Subnet: ${result.inputIp}/${result.cidr}\nNetwork: ${result.networkAddress}\nUsable: ${result.ipRange}\nHosts: ${result.usableHosts}\nMask: ${result.subnetMask}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Hasil Kalkulasi"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan Hasil")
                        }
                    }
                }
            }

            // Key Overview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubnetMetricItem(
                        label = "Host Usable",
                        value = "%,d".format(Locale.US, result.usableHosts),
                        badgeText = if (result.isPointToPoint) "RFC 3021 /31" else if (result.isSingleHost) "/32 Host" else null,
                        valueColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    SubnetMetricItem(
                        label = "Total Alamat IP",
                        value = "%,d".format(Locale.US, result.totalIps),
                        valueColor = CyanNeon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SubnetMetricItem(
                    label = "Rentang Alamat Host (Usable IP Range)",
                    value = result.ipRange,
                    badgeText = "CIDR /${result.cidr}",
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubnetMetricItem(
                        label = "Network Address",
                        value = result.networkAddress,
                        modifier = Modifier.weight(1f)
                    )
                    SubnetMetricItem(
                        label = "Broadcast Address",
                        value = result.broadcastAddress,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubnetMetricItem(
                        label = "Subnet Mask",
                        value = result.subnetMask,
                        modifier = Modifier.weight(1f)
                    )
                    SubnetMetricItem(
                        label = "Wildcard Mask",
                        value = result.wildcardMask,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubnetMetricItem(
                        label = "First Usable IP",
                        value = result.firstUsableIp,
                        modifier = Modifier.weight(1f)
                    )
                    SubnetMetricItem(
                        label = "Last Usable IP",
                        value = result.lastUsableIp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Scope & Classful Explanation Cards
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = result.addressType,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = result.ipClass,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = result.addressTypeDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = result.classNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Binary Octet Breakdown Table
            item {
                Text(
                    text = "Representasi Biner 32-Bit Per Oktet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                BinaryOctetCard(
                    title = "IP Address",
                    decimalValue = result.inputIp,
                    binaryValue = result.binaryIp,
                    highlightColor = MaterialTheme.colorScheme.primary
                )
            }

            item {
                BinaryOctetCard(
                    title = "Subnet Mask",
                    decimalValue = result.subnetMask,
                    binaryValue = result.binarySubnetMask,
                    highlightColor = EmeraldSuccess
                )
            }

            item {
                BinaryOctetCard(
                    title = "Network ID",
                    decimalValue = result.networkAddress,
                    binaryValue = result.binaryNetwork,
                    highlightColor = CyanNeon
                )
            }

            item {
                BinaryOctetCard(
                    title = "Broadcast ID",
                    decimalValue = result.broadcastAddress,
                    binaryValue = result.binaryBroadcast,
                    highlightColor = RoseError
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun VlsmCalculatorTab(viewModel: MainViewModel) {
    val state by viewModel.calcState.collectAsState()
    var newSubnetName by remember { mutableStateOf("") }
    var newSubnetHosts by remember { mutableStateOf("") }

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
                        text = "Jaringan Induk (Major Network)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.vlsmMajorIp,
                            onValueChange = { viewModel.setVlsmMajorNetwork(it, state.vlsmMajorCidr) },
                            label = { Text("IP Induk") },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = state.vlsmMajorCidr.toString(),
                            onValueChange = {
                                val c = it.toIntOrNull() ?: state.vlsmMajorCidr
                                viewModel.setVlsmMajorNetwork(state.vlsmMajorIp, c.coerceIn(0, 30))
                            },
                            label = { Text("Prefix (0-30)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Tambah Kebutuhan Subnet",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubnetName,
                            onValueChange = { newSubnetName = it },
                            placeholder = { Text("Nama Subnet") },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = newSubnetHosts,
                            onValueChange = { newSubnetHosts = it },
                            placeholder = { Text("Jumlah Host") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f)
                        )
                        IconButton(
                            onClick = {
                                val hosts = newSubnetHosts.toIntOrNull() ?: 0
                                if (hosts > 0) {
                                    viewModel.addVlsmSubnetRequest(newSubnetName, hosts)
                                    newSubnetName = ""
                                    newSubnetHosts = ""
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
                        }
                    }
                }
            }
        }

        // List of requested subnets
        item {
            Text(
                text = "Daftar Permintaan Subnet (${state.vlsmRequests.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.vlsmRequests.size) { idx ->
            val req = state.vlsmRequests[idx]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = req.first, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Dibutuhkan: ${req.second} host", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.removeVlsmSubnetRequest(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RoseError)
                    }
                }
            }
        }

        if (state.vlsmError != null) {
            item {
                Text(text = state.vlsmError!!, color = RoseError, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.vlsmResults.isNotEmpty()) {
            item {
                Text(
                    text = "Hasil Alokasi VLSM Otomatis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            }

            items(state.vlsmResults) { subnet ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${subnet.subnetIndex}. ${subnet.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(text = "/${subnet.prefix}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = "Network: ${subnet.networkAddress}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Subnet Mask: ${subnet.subnetMask}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Usable Range: ${subnet.firstUsable} - ${subnet.lastUsable}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                        Text(text = "Broadcast: ${subnet.broadcastAddress}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Alokasi Host: ${subnet.allocatedHosts} (Dibutuhkan: ${subnet.neededHosts})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun Ipv6ExplorerTab(viewModel: MainViewModel) {
    val state by viewModel.calcState.collectAsState()
    val result = state.ipv6Result

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
                        text = "Eksplorasi Alamat IPv6 (128-Bit)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = state.ipv6Input,
                        onValueChange = { viewModel.setIpv6Input(it) },
                        label = { Text("Alamat IPv6") },
                        placeholder = { Text("2001:db8:85a3::8a2e:370:7334") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Prefix Length: /${state.ipv6Prefix}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = state.ipv6Prefix.toFloat(),
                            onValueChange = { viewModel.setIpv6Input(state.ipv6Input, it.toInt()) },
                            valueRange = 0f..128f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { viewModel.performIpv6Calculation() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Analisis IPv6")
                    }

                    if (state.ipv6Error != null) {
                        Text(text = state.ipv6Error!!, color = RoseError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (result != null) {
            item {
                SubnetMetricItem(
                    label = "Bentuk Terkompresi (RFC 5952)",
                    value = result.compressedAddress,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
            item {
                SubnetMetricItem(
                    label = "Bentuk Terekspansi Penuh (8 Grup x 4 Hex)",
                    value = result.expandedAddress,
                    valueColor = CyanNeon
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubnetMetricItem(
                        label = "Tipe Alamat IPv6",
                        value = result.addressType,
                        modifier = Modifier.weight(1f)
                    )
                    SubnetMetricItem(
                        label = "Cakupan (Scope)",
                        value = result.addressScope,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                SubnetMetricItem(
                    label = "Ukuran Ruang Alamat Prefix /${result.prefix}",
                    value = result.totalAddressesExp,
                    valueColor = EmeraldSuccess
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun HistoryTab(viewModel: MainViewModel, history: List<com.example.calculator.model.CalculationHistoryItem>) {
    val context = LocalContext.current
    val calcState by viewModel.calcState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val filtered = history.filter {
        calcState.historySearchQuery.isBlank() ||
                it.ipWithCidr.contains(calcState.historySearchQuery, ignoreCase = true) ||
                it.networkAddress.contains(calcState.historySearchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = calcState.historySearchQuery,
                onValueChange = { viewModel.setHistorySearchQuery(it) },
                placeholder = { Text("Cari riwayat...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { viewModel.userRepo.clearCalculationHistory() }) {
                Text("Hapus Semua")
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (history.isEmpty()) "Belum ada riwayat kalkulasi." else "Tidak ada riwayat yang cocok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.ipWithCidr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = dateFormat.format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = "Network: ${item.networkAddress} | Mask: ${item.subnetMask}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            Text(text = "Rentang: ${item.usableRange} (${item.usableHosts} host)", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    val parts = item.ipWithCidr.split("/")
                                    if (parts.size == 2) {
                                        viewModel.setIpInput(parts[0])
                                        parts[1].toIntOrNull()?.let { viewModel.setCidrInput(it) }
                                        viewModel.setActiveCalculatorTab(0)
                                    }
                                }) {
                                    Icon(Icons.Default.Calculate, contentDescription = "Hitung Ulang")
                                }
                                IconButton(onClick = { viewModel.userRepo.deleteHistoryItem(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Riwayat", tint = RoseError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
