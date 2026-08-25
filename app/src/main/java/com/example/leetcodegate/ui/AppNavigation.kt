package com.example.leetcodegate.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.leetcodegate.AppContainer

@Composable
fun AppNavigation(container: AppContainer, startDestination: String = "home") {
    val navController = rememberNavController()
    val activity = LocalContext.current as? Activity
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                container = container,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToVerification = { navController.navigate("verification") }
            )
        }
        composable("settings") {
            SettingsScreen(
                container = container,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("verification") {
            val viewModel: VerificationViewModel = viewModel(
                factory = VerificationViewModelFactory(container)
            )
            VerificationScreen(
                viewModel = viewModel,
                container = container,
                onSuccess = {
                    if (startDestination == "verification") {
                        activity?.finish()
                    } else {
                        navController.popBackStack("home", inclusive = false)
                    }
                },
                onCancel = {
                    if (startDestination == "verification") {
                        activity?.finish()
                    } else {
                        navController.popBackStack("home", inclusive = false)
                    }
                }
            )
        }
    }
}
