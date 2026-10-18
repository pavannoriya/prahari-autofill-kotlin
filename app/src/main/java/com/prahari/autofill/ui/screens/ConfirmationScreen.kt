package com.prahari.autofill.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prahari.autofill.ui.navigation.FillSession

@Composable
fun ConfirmationScreen(
    success: Boolean,
    message: String,
    onFillNext: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            if (success) "✅" else "❌",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (success) "Fill Ho Gaya!" else "Kuch Gadbad Hui",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (success) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Center: ${FillSession.centerName}", style = MaterialTheme.typography.titleSmall)
                    Text("Members: ${FillSession.memberCount}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("10 Points:", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(FillSession.answers) { a ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text("${a.point}: ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    a.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (a.answer == "Yes") MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = onFillNext, modifier = Modifier.fillMaxWidth()) {
            Text("📝 Agla Center Fill Karo")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) {
            Text("🏠 Home Jao")
        }
    }
}
