package com.prahari.autofill.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prahari.autofill.PrahariApp
import com.prahari.autofill.ui.navigation.FillSession
import kotlinx.coroutines.launch
import java.io.InputStream

@Composable
fun CDSPhotoScreen(onPhotoReady: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> photoUri = uri }

    // Camera launcher (simple: gallery hi use karenge, camera optional)
    fun analyzePhoto() {
        val uri = photoUri ?: return
        analyzing = true; error = null
        scope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes == null) {
                    error = "Photo read nahi hui"; analyzing = false; return@launch
                }
                val rawBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                FillSession.cdsPhotoRawBase64 = rawBase64
                FillSession.cdsPhotoBase64 = "data:image/jpeg;base64,$rawBase64"

                // Gemini se CDS padho
                val r = app.geminiApi.readCDS(rawBase64)
                analyzing = false
                if (r.isSuccess) {
                    FillSession.cdsData = r.getOrNull()
                    onPhotoReady()
                } else {
                    error = "CDS padhne me error: ${r.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                analyzing = false
                error = "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CDS Photo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Center: ${FillSession.centerName}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        photoUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "CDS Photo",
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (photoUri == null) "📷 CDS Photo Chuno" else "🔄 Dusri Photo Chuno") }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { analyzePhoto() },
            modifier = Modifier.fillMaxWidth(),
            enabled = photoUri != null && !analyzing
        ) {
            if (analyzing) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Gemini padh raha hai...")
            } else Text("✅ Photo Analyze Karo")
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
