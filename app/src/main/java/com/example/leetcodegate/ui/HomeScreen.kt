package com.example.leetcodegate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Column {
                TopAppBar(
                    title = { 
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.leetcodegate.R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .height(64.dp)
                                .offset(x = (-12).dp, y = 2.dp)
                        )
                    },
                    actions = {
                        TextButton(onClick = onNavigateToSettings) {
                            Text("[ Settings ]", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "[+] Time Remaining",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            val minutes = creditSeconds / 60
            val seconds = creditSeconds % 60
            Text(
                text = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                color = if (creditSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "[+] Problems Conquered: ${completedProblems.size}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onNavigateToVerification,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Earn Time", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
