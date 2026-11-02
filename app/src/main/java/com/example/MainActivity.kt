package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.OpeningScreen
import com.example.ui.screens.SecretSpaceGateScreen
import com.example.ui.screens.SecretSpaceHubScreen
import com.example.ui.screens.UsernameLoginDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppDestination.OPENING -> {
                    OpeningScreen(viewModel = viewModel)
                }
                AppDestination.CALCULATOR -> {
                    CalculatorScreen(viewModel = viewModel)
                }
                AppDestination.SECRET_GATE -> {
                    SecretSpaceGateScreen(viewModel = viewModel)
                }
                AppDestination.SECRET_HUB -> {
                    SecretSpaceHubScreen(viewModel = viewModel)
                }
            }

            // First-run username onboarding dialog
            UsernameLoginDialog(viewModel = viewModel)
        }
    }
}
