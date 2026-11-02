package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricHelper
import com.example.security.BiometricStatus
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.RoseError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretSpaceGateScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pinInput by viewModel.pinInput.collectAsState()
    val errorMessage by viewModel.gateErrorMessage.collectAsState()
    val lockoutRemaining by viewModel.lockoutRemainingSeconds.collectAsState()
    val userProfile by viewModel.userRepo.userProfileFlow.collectAsState()

    val hasPin = viewModel.userRepo.hasPinSet()
    val isBiometricAvailable = BiometricHelper.checkBiometricStatus(context) is BiometricStatus.Available

    BackHandler {
        viewModel.navigateTo(AppDestination.CALCULATOR)
    }

    // Auto prompt biometric on entry if enabled and available
    LaunchedEffect(Unit) {
        if (hasPin && userProfile.isBiometricEnabled && isBiometricAvailable) {
            BiometricHelper.showBiometricPrompt(
                context = context,
                onSuccess = { viewModel.unlockWithBiometricSuccess() },
                onError = { /* fallback to pin */ },
                onNegativeClick = { /* fallback to pin */ }
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Autentikasi Ruang Rahasia") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppDestination.CALCULATOR) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Kalkulator")
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (!hasPin) "Buat 6 Digit PIN Vault Baru" else "Masukkan PIN Ruang Rahasia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (!hasPin)
                    "Kunci enkripsi AES-256 akan diturunkan secara aman dari PIN ini."
                else
                    "Akses dilindungi enkripsi Android Keystore & PBKDF2.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // PIN Dot Indicators (6 digits)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 0 until 6) {
                    val filled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.5.dp,
                                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    )
                }
            }

            // Error or Lockout Message
            if (lockoutRemaining > 0) {
                Text(
                    text = "Terkunci sementara! Harap tunggu $lockoutRemaining detik.",
                    color = RoseError,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = RoseError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Numeric Keypad
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                keypad.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when (key) {
                                            "BIO" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            "DEL" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable(enabled = lockoutRemaining == 0) {
                                        when (key) {
                                            "BIO" -> {
                                                if (hasPin && isBiometricAvailable) {
                                                    BiometricHelper.showBiometricPrompt(
                                                        context = context,
                                                        onSuccess = { viewModel.unlockWithBiometricSuccess() },
                                                        onError = { viewModel.onPinClear() },
                                                        onNegativeClick = { /* stay on pin */ }
                                                    )
                                                }
                                            }
                                            "DEL" -> viewModel.onPinBackspace()
                                            else -> viewModel.onPinDigit(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "BIO" -> Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Autentikasi Sidik Jari",
                                        tint = if (isBiometricAvailable) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    "DEL" -> Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Hapus Angka",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    else -> Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
