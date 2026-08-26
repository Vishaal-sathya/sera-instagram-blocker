package com.example.leetcodegate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.leetcodegate.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

sealed class VerificationState {
    object Camera : VerificationState()
    object ProcessingOcr : VerificationState()
    data class ExplanationInput(
        val problemId: String, 
        val title: String?, 
        val ocrText: String,
        val explanation: String = "",
        val error: String? = null
    ) : VerificationState()
    data class ValidatingLlm(
        val problemId: String, 
        val title: String?, 
        val ocrText: String, 
        val explanation: String
    ) : VerificationState()
    data class Success(val feedback: String) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

class VerificationViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<VerificationState>(VerificationState.Camera)
    val state: StateFlow<VerificationState> = _state.asStateFlow()

    fun processPhoto(file: File) {
        viewModelScope.launch {
            _state.value = VerificationState.ProcessingOcr
            try {
                val visionText = container.ocrEngine.extractText(file)
                android.util.Log.d("OCR_DEBUG", "RAW TEXT:\n${visionText.text}")
                val extracted = container.problemExtractor.extractProblemDetails(visionText)
                android.util.Log.d("OCR_DEBUG", "EXTRACTED: $extracted")
                
                if (extracted == null) {
                    _state.value = VerificationState.Error("Could not detect a LeetCode problem ID.")
                    return@launch
                }

                if (!extracted.isSolved) {
                    _state.value = VerificationState.Error("Problem not detected as Solved/Accepted. Make sure the success message is visible.")
                    return@launch
                }

                val problemId = extracted.id

                if (container.completedProblemStore.isCompleted(problemId)) {
                    _state.value = VerificationState.Error("You've already claimed time for problem $problemId! Pick a new one.")
                    return@launch
                }

                _state.value = VerificationState.ExplanationInput(problemId, extracted.title, visionText.text)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = VerificationState.Error("OCR Failed: ${e.message}")
            } finally {
                file.delete()
            }
        }
    }

    fun submitExplanation(problemId: String, title: String?, ocrText: String, explanation: String) {
        if (_state.value is VerificationState.ValidatingLlm) return
        
        viewModelScope.launch {
            _state.value = VerificationState.ValidatingLlm(problemId, title, ocrText, explanation)
            try {
                val config = container.settingsStore.getLlmConfigSync()
                val result = container.llmValidator.validateExplanation(config, problemId, explanation, ocrText)
                
                if (result.pass) {
                    // Check if LLM detected a different problem number
                    val llmId = result.detected_problem_number
                    if (llmId != null && llmId != problemId) {
                        _state.value = VerificationState.ExplanationInput(problemId, title, ocrText, explanation, "Rejected: The LLM detected problem $llmId, but we detected $problemId. Try taking a clearer photo.")
                        return@launch
                    }
                    
                    // Transaction safety: Save problem first before giving credit
                    try {
                        container.completedProblemStore.addCompleted(problemId)
                        container.creditManager.addCredit(300)
                        _state.value = VerificationState.Success(result.interview_feedback)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        container.completedProblemStore.removeCompleted(problemId)
                        _state.value = VerificationState.ExplanationInput(problemId, title, ocrText, explanation, "Database Error: Failed to add credit.")
                    }
                } else {
                    _state.value = VerificationState.ExplanationInput(problemId, title, ocrText, explanation, "Rejected: ${result.reason}\n\nFeedback: ${result.interview_feedback}")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = VerificationState.ExplanationInput(problemId, title, ocrText, explanation, "Network Error: ${e.message}")
            }
        }
    }

    fun setError(message: String) {
        _state.value = VerificationState.Error(message)
    }

    fun reset() {
        _state.value = VerificationState.Camera
    }
}

class VerificationViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VerificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VerificationViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
