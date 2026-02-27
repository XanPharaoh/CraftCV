package com.craftcv.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.craftcv.app.ads.AdManager
import com.craftcv.app.ads.BannerAd
import com.craftcv.app.data.models.TailorResponse
import com.craftcv.app.ui.screens.*
import com.craftcv.app.ui.theme.CraftCVTheme
import com.craftcv.app.viewmodel.MainViewModel
import com.craftcv.app.viewmodel.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdManager.initialize(this)
        setContent {
            CraftCVTheme {
                CraftCVApp()
            }
        }
    }
}

@Composable
private fun CraftCVApp() {
    val viewModel: MainViewModel = viewModel()
    val snackbarHostState        = remember { SnackbarHostState() }
    val activity                 = LocalContext.current as Activity

    // Nav state — "loading" until we check DataStore
    var currentScreen by remember { mutableStateOf("loading") }

    // Determine start screen once on launch
    LaunchedEffect(Unit) {
        currentScreen = if (viewModel.hasSeenProfile()) "dashboard" else "profile"
    }

    // Collected states
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile   by viewModel.userProfile.collectAsStateWithLifecycle()
    val isPro         by viewModel.isPro.collectAsStateWithLifecycle()
    val usesRemaining by viewModel.usesRemaining.collectAsStateWithLifecycle()

    // Snackbar
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // System back button support
    BackHandler(enabled = currentScreen !in listOf("loading", "dashboard", "profile")) {
        currentScreen = when (currentScreen) {
            "results" -> { viewModel.resetState(); viewModel.resetCoverLetter(); "dashboard" }
            "history" -> "dashboard"
            "paywall" -> if (uiState is UiState.Success) "results" else "dashboard"
            else -> "dashboard"
        }
    }

    Scaffold(
        modifier     = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar    = {
            // Non-invasive banner ad — only for free users, hidden for Pro
            if (!isPro) BannerAd()
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
        when (currentScreen) {

            "loading" -> {
                // Blank while DataStore resolves — typically < 100ms
            }

            "profile" -> ProfileScreen(
                onProfileReady = { profile ->
                    viewModel.saveProfile(profile)
                    currentScreen = "dashboard"
                },
                onSkipToDashboard = {
                    viewModel.skipProfile()
                    currentScreen = "dashboard"
                },
            )

            "dashboard" -> DashboardScreen(
                viewModel         = viewModel,
                profile           = userProfile,
                isPro             = isPro,
                usesRemaining     = usesRemaining,
                onResultsReady    = { currentScreen = "results" },
                onPaywallRequired = { currentScreen = "paywall" },
                onEditProfile     = { currentScreen = "profile" },
                onHistory         = { currentScreen = "history" },
                activity          = activity,
            )

            "history" -> HistoryScreen(
                viewModel = viewModel,
                onBack    = { currentScreen = "dashboard" },
            )

            "results" -> {
                val tailorData = (uiState as? UiState.Success)?.response ?: TailorResponse()
                ResultsScreen(
                    tailorData     = tailorData,
                    viewModel      = viewModel,
                    resumeText     = viewModel.lastResumeText,
                    jobDescription = viewModel.lastJobDesc,
                    isPro          = isPro,
                    onUpgradeClick = { currentScreen = "paywall" },
                    onBack         = {
                        viewModel.resetState()
                        viewModel.resetCoverLetter()
                        currentScreen = "dashboard"
                    },
                    activity       = activity,
                )
            }

            "paywall" -> {
                val isPurchasePending by viewModel.billingManager.isPurchasePending.collectAsStateWithLifecycle()
                val billingError      by viewModel.billingManager.billingError.collectAsStateWithLifecycle()

                // Navigate away as soon as billing confirms Pro
                LaunchedEffect(isPro) {
                    if (isPro) currentScreen = if (uiState is UiState.Success) "results" else "dashboard"
                }

                PaywallScreen(
                    onUpgrade             = { planIndex -> viewModel.upgradeToPro(activity, planIndex) },
                    onBack                = { currentScreen = if (uiState is UiState.Success) "results" else "dashboard" },
                    isPurchasePending     = isPurchasePending,
                    billingError          = billingError,
                    onDismissBillingError = { viewModel.billingManager.clearBillingError() },
                )
            }
        }
        }
    }
}
