package com.example.leetcodegate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.leetcodegate.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onNavigateToSettings: () -> Unit,
    onNavigateToVerification: () -> Unit
) {
    val creditSeconds by container.creditManager.getCreditFlow().collectAsState(initial = 0)
    val completedProblems by container.completedProblemStore.completedProblems.collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LeetCodeGate") },
                actions = {
                    Button(onClick = onNavigateToSettings) {
                        Text("Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Time Remaining",
                style = MaterialTheme.typography.titleLarge
            )
            
            val minutes = creditSeconds / 60
            val seconds = creditSeconds % 60
            Text(
                text = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                color = if (creditSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Problems Conquered: ${completedProblems.size}",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onNavigateToVerification,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {
                Text("Earn Time (Take Photo)")
            }
        }
    }
}
