package com.prahari.autofill.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.prahari.autofill.PrahariApp
import kotlinx.coroutines.launch

/**
 * Prahari Connect — Prahari login + apni Gemini API key
 * Password phone me kabhi save NAHI hota, sirf JWT token SecureStorage me
 */
@Composable
fun PrahariConnectScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(app.secureStorage.getPrahariUsername() ?: "") }
    var password by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf(app.secureStorage.getGeminiKey() ?: "") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Prahari Connect", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Phone me InstaSafe VPN ON hona chahiye",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = username, onValueChange = { username = it },
            label = { Text("Prahari Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it },
            label = { Text("Prahari Password (save nahi hoga)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = geminiKey, onValueChange = { geminiKey = it },
            label = { Text("Apni Gemini API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true)

        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.primary) }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    error = "Username aur password dalo"; return@Button
                }
                if (geminiKey.isBlank()) {
                    error = "Gemini API key dalo (CDS photo ke liye)"; return@Button
                }
                loading = true; error = null; info = null
                scope.launch {
                    app.secureStorage.saveGeminiKey(geminiKey.trim())
                    val r = app.prahariApi.login(username.trim(), password)
                    loading = false
                    if (r.isSuccess) {
                        info = "Connected! ✅"
                        onConnected()
                    } else {
                        error = "Prahari login failed: ${r.exceptionOrNull()?.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(), enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Connect Karo")
        }
    }
}
