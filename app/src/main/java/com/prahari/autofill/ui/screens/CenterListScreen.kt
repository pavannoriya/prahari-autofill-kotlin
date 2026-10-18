package com.prahari.autofill.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prahari.autofill.PrahariApp
import com.prahari.autofill.data.models.Center
import com.prahari.autofill.ui.navigation.FillSession
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CenterListScreen(
    auditId: String,
    branchId: String,
    onCenterSelected: (centerId: String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var centers by remember { mutableStateOf<List<Center>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(auditId) {
        scope.launch {
            // Aaj ki date
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            // Centers list — audit list se nikalte hain (risk-details per center hota hai,
            // isliye pehle audit detail se centers nikalenge)
            // NOTE: Prahari me center list ka direct endpoint audit detail me hota hai
            loading = false
            // Fallback: user manually center code dalega (neeche search)
        }
    }

    val filtered = centers.filter {
        search.isBlank() ||
                it.centerName.contains(search, true) ||
                it.centerCode.contains(search, true)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Centers — ${FillSession.auditName}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = search, onValueChange = { search = it },
            label = { Text("Center search karo (naam/code)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (centers.isEmpty() && !loading) {
            Text(
                "Center list load karne ke liye CDS photo wala flow use karo —\n" +
                        "ya center code neeche dalke aage badho.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            var manualCode by remember { mutableStateOf("") }
            OutlinedTextField(value = manualCode, onValueChange = { manualCode = it },
                label = { Text("Center Code (e.g. NER:RRL:03:349)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (manualCode.isNotBlank()) {
                        FillSession.centerCode = manualCode.trim()
                        FillSession.centerId = manualCode.trim()
                        FillSession.centerName = manualCode.trim()
                        onCenterSelected(manualCode.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Is Center pe Fill Karo →") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { center ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        FillSession.centerId = center.centerId
                        FillSession.centerCode = center.centerCode
                        FillSession.centerName = center.centerName
                        onCenterSelected(center.centerId)
                    }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(center.centerName, style = MaterialTheme.typography.titleMedium)
                        Text(center.centerCode, style = MaterialTheme.typography.bodySmall)
                        Text("${center.meetingDay} ${center.meetingTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
