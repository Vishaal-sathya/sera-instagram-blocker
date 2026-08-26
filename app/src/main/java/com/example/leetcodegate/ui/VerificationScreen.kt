package com.example.leetcodegate.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import io.noties.markwon.Markwon
import androidx.core.content.ContextCompat
import com.example.leetcodegate.AppContainer
import com.example.leetcodegate.camera.CameraManager
import com.example.leetcodegate.camera.CameraPreview
import kotlinx.coroutines.launch

@Composable
fun VerificationScreen(viewModel: VerificationViewModel, container: AppContainer, onSuccess: () -> Unit, onCancel: () -> Unit) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is VerificationState.Camera -> {
            val context = LocalContext.current
            val cameraManager = remember { CameraManager(context) }
            
            CameraScreen(
                cameraManager = cameraManager, 
                onPhotoTaken = { file -> 
                    viewModel.processPhoto(file)
                },
                onError = { message ->
                    viewModel.setError(message)
                },
                onCancel = {
                    viewModel.reset()
                    onCancel()
                }
            )
        }
        is VerificationState.ProcessingOcr -> {
            LoadingScreen("Extracting LeetCode Problem...")
        }
        is VerificationState.ExplanationInput -> {
            ExplanationScreen(
                problemId = currentState.problemId,
                title = "Detected Problem Number ${currentState.problemId}",
                initialExplanation = currentState.explanation,
                errorMessage = currentState.error,
                onValidate = { explanation -> 
                    viewModel.submitExplanation(currentState.problemId, currentState.title, currentState.ocrText, explanation)
                },
                onCancel = {
                    viewModel.reset()
                    onCancel()
                },
                isValidating = false
            )
        }
        is VerificationState.ValidatingLlm -> {
            ExplanationScreen(
                problemId = currentState.problemId,
                title = "Detected Problem Number ${currentState.problemId}",
                initialExplanation = currentState.explanation,
                errorMessage = null,
                onValidate = { },
                onCancel = {
                    viewModel.reset()
                    onCancel()
                },
                isValidating = true
            )
        }
        is VerificationState.Success -> {
            ResultScreen(
                "[x] Solution Accepted",
                "You earned 5 minutes of instagram",
                currentState.feedback, 
                true, 
                "Acknowledge"
            ) {
                viewModel.reset()
                onSuccess()
            }
        }
        is VerificationState.Error -> {
            ResultScreen(
                "[!] Error",
                null,
                currentState.message, 
                false, 
                "Retake Photo"
            ) {
                viewModel.reset()
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ResultScreen(title: String, highlight: String?, message: String, isSuccess: Boolean, actionText: String, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.Start, 
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.headlineMedium, 
                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (highlight != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        highlight, 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(android.graphics.Color.parseColor("#201D1D"))
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                },
                update = { view ->
                    val markwon = Markwon.create(view.context)
                    markwon.setMarkdown(view, message)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun CameraScreen(cameraManager: CameraManager, onPhotoTaken: (java.io.File) -> Unit, onError: (String) -> Unit, onCancel: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        var isCapturing by remember { mutableStateOf(false) }
        
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(cameraManager = cameraManager, modifier = Modifier.fillMaxSize())
            
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            ) {
                Text("[ Cancel ]", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = {
                    if (isCapturing) return@Button
                    isCapturing = true
                    coroutineScope.launch {
                        try {
                            val photoFile = cameraManager.takePhoto()
                            onPhotoTaken(photoFile)
                        } catch (e: Exception) {
                            onError("Capture failed: ${e.message}")
                            isCapturing = false
                        }
                    }
                },
                enabled = !isCapturing,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {}
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    "Camera permission is required to capture LeetCode problems.", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text("Grant Permission", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
