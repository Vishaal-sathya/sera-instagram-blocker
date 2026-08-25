package com.example.leetcodegate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationScreen(
    problemId: String,
    title: String?,
    initialExplanation: String,
    errorMessage: String?,
    onValidate: (String) -> Unit,
    onCancel: () -> Unit,
    isValidating: Boolean
) {
    var explanation by remember(initialExplanation) { mutableStateOf(initialExplanation) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Solution") },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val displayTitle = if (title != null) "$problemId. $title" else "Problem $problemId"
            Text(
                text = "Detected: $displayTitle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Explain your solution logic, time complexity, and space complexity to prove you solved it.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Explanation") },
                placeholder = { Text("e.g. I used a hash map to store complements...") },
                minLines = 10,
                enabled = !isValidating
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val color = if (explanation.length >= 30) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    text = "${explanation.length} / 30 min chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onValidate(explanation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = explanation.length >= 30 && !isValidating
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Validating...")
                } else {
                    Text("Submit for Verification")
                }
            }
        }
    }
}
