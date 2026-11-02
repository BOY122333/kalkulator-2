package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.RoseError

@Composable
fun UsernameLoginDialog(
    viewModel: MainViewModel
) {
    val showPrompt by viewModel.showUsernamePrompt.collectAsState()
    val usernameInput by viewModel.onboardingUsernameInput.collectAsState()
    val errorMsg by viewModel.onboardingError.collectAsState()
    val privacyAgreed by viewModel.privacyAgreed.collectAsState()

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { /* mandatory on first launch */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Selamat Datang di NetVault",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Silakan masukkan username unik Anda untuk mulai menggunakan kalkulator dan mengaitkan Ruang Rahasia privat Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { viewModel.setOnboardingUsername(it) },
                        label = { Text("Username") },
                        placeholder = { Text("Contoh: cyber_admin") },
                        singleLine = true,
                        isError = errorMsg != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = RoseError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Privacy & Security Disclosure Note
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pemberitahuan Keamanan & Privasi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = "• Username digunakan sebagai pengenal lokal dan sesi sinkronisasi cloud privat.\n• Kunci enkripsi AES-256 berkas diturunkan dari kredensial aman di perangkat Anda.\n• Kami tidak pernah mencatat konten file, foto, dokumen, atau PIN ke analitik.\n• Aplikasi dapat beroperasi penuh secara offline tanpa internet.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = privacyAgreed,
                            onCheckedChange = { viewModel.setPrivacyAgreed(it) },
                            modifier = Modifier.testTag("privacy_checkbox")
                        )
                        Text(
                            text = "Saya memahami dan menyetujui kebijakan privasi & pengumpulan data.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitUsernameOnboarding() },
                    modifier = Modifier.testTag("login_submit_button")
                ) {
                    Text("Lanjutkan ke Aplikasi")
                }
            }
        )
    }
}
