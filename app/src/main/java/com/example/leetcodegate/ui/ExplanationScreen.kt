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
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Verify Solution", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        TextButton(
                            onClick = onCancel
                        ) {
                            Text("[ Cancel ]", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Problem Number $problemId",
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
                label = { Text("Explanation", style = MaterialTheme.typography.labelLarge) },
                placeholder = { Text("e.g. I used a hash map to store complements...") },
                minLines = 10,
                enabled = !isValidating,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
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
                enabled = explanation.length >= 30 && !isValidating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Validating...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("Submit for Verification", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
