package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.TechNavy900
import kotlinx.coroutines.delay

@Composable
fun OpeningScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.userRepo.remoteConfigFlow.collectAsState()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val duration = config.openingDurationMs.coerceAtLeast(1000L).toInt()
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration, easing = LinearEasing)
        )
        viewModel.skipOpening()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TechNavy900)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .border(2.dp, CyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = config.appDisplayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Kalkulator IP Subnet & Private Vault",
                style = MaterialTheme.typography.bodyMedium,
                color = CyanNeon.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyanNeon,
                trackColor = Color(0xFF1E293B)
            )

            if (config.openingSkipAllowed) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.skipOpening() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Lewati Intro")
                }
            }
        }
    }
}
