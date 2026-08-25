package com.example.leetcodegate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                title = currentState.title,
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
                title = currentState.title,
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
            ResultScreen("Success!", "You've earned 5 minutes of Instagram.", true, "Awesome!") {
                viewModel.reset()
                onSuccess()
            }
        }
        is VerificationState.Error -> {
            ResultScreen("Error", currentState.message, false, "Retake Photo") {
                viewModel.reset()
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
fun ResultScreen(title: String, message: String, isSuccess: Boolean, actionText: String, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAction) {
                Text(actionText)
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
            
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.99f)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                drawRect(color = Color.Black.copy(alpha = 0.6f))
                
                val frameWidth = canvasWidth * 0.8f
                val frameHeight = 150.dp.toPx()
                val left = (canvasWidth - frameWidth) / 2f
                val top = canvasHeight * 0.2f
                
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
                
                drawRoundRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            
            Text(
                text = "Align problem title inside the box",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("Cancel")
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required to capture LeetCode problems.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
