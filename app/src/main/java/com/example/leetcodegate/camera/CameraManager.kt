package com.example.leetcodegate.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraManager(private val context: Context) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    init {
        // Clean up any abandoned photos from previous sessions to prevent memory leaks
        context.cacheDir.listFiles { file ->
            file.name.startsWith("leetcode_capture_")
        }?.forEach { it.delete() }
    }

    suspend fun getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            continuation.resume(provider)
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun setupCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProvider = getCameraProvider()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            imageCapture = null
            Log.e("CameraManager", "Use case binding failed", e)
        }
    }

    fun unbindAll() {
        cameraProvider?.unbindAll()
        imageCapture = null
    }

    suspend fun takePhoto(): File = suspendCancellableCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("Camera not initialized"))
            return@suspendCancellableCoroutine
        }

        val photoFile = File.createTempFile("leetcode_capture_", ".jpg", context.cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        continuation.invokeOnCancellation {
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (continuation.isActive) {
                        continuation.resume(photoFile)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    if (photoFile.exists()) {
                        photoFile.delete()
                    }
                    if (continuation.isActive) {
                        continuation.resumeWithException(exc)
                    }
                }
            }
        )
    }
}
