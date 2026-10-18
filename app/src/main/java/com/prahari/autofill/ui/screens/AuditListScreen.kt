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
import com.prahari.autofill.data.models.Audit
import com.prahari.autofill.ui.navigation.FillSession
import kotlinx.coroutines.launch

@Composable
fun AuditListScreen(onAuditSelected: (auditId: String, branchId: String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var audits by remember { mutableStateOf<List<Audit>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val r = app.prahariApi.getAudits()
            loading = false
            if (r.isSuccess) audits = r.getOrNull() ?: emptyList()
            else error = r.exceptionOrNull()?.message
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mere Audits", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
            audits.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Koi audit assign nahi hai")
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(audits) { audit ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            FillSession.auditId = audit.id
                            FillSession.auditName = audit.name
                            FillSession.branchId = audit.branchId
                            FillSession.branchName = audit.branchName
                            onAuditSelected(audit.id, audit.branchId)
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(audit.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Branch: ${audit.branchName}", style = MaterialTheme.typography.bodyMedium)
                            Text("${audit.startDate} → ${audit.endDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
