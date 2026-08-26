package com.example.leetcodegate.ui

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.leetcodegate.AppContainer
import com.example.leetcodegate.accessibility.InstagramAccessibilityService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val llmConfig by container.settingsStore.llmConfig.collectAsState(initial = null)
    
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    if (llmConfig != null && apiKey.isEmpty() && baseUrl.isEmpty() && model.isEmpty()) {
        apiKey = llmConfig!!.apiKey
        baseUrl = llmConfig!!.baseUrl
        model = llmConfig!!.model
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        TextButton(onClick = onNavigateBack) {
                            Text("[ Back ]", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            var isAccessibilityEnabled by remember { mutableStateOf(false) }
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        val enabledServices = Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                        )
                        isAccessibilityEnabled = enabledServices?.contains(context.packageName) == true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Text("Accessibility Service", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Status: ")
                Text(
                    text = if (isAccessibilityEnabled) "Enabled" else "Disabled",
                    color = if (isAccessibilityEnabled) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Open Accessibility Settings", style = MaterialTheme.typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Nvidia NIM Configuration", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        container.settingsStore.updateConfig(apiKey, baseUrl, model)
                        Toast.makeText(context, "Configuration Saved!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Configuration", style = MaterialTheme.typography.labelLarge)
            }
            
        }
    }
}
