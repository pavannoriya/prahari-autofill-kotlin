package com.prahari.autofill.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prahari.autofill.data.models.Member
import com.prahari.autofill.ui.navigation.FillSession

/**
 * CDS Results — Gemini ka output verify karo, tap karke card-missing flag karo
 */
@Composable
fun CDSResultsScreen(onConfirm: () -> Unit) {
    val cds = FillSession.cdsData
    var members by remember { mutableStateOf(cds?.members ?: emptyList()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("CDS Verify Karo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Center: ${cds?.centerName ?: FillSession.centerName}", style = MaterialTheme.typography.titleMedium)
                Text("Code: ${cds?.centerCode}", style = MaterialTheme.typography.bodySmall)
                Text("Meeting: ${cds?.meetingDay} ${cds?.meetingTime}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Members (${members.size}) — card missing wale pe tap karo:",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(members) { idx, m ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        members = members.toMutableList().also {
                            it[idx] = m.copy(cardMissing = !m.cardMissing)
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (m.cardMissing)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = m.cardMissing,
                            onCheckedChange = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${m.serial}. ${m.name}", style = MaterialTheme.typography.bodyLarge)
                            if (m.customerId.isNotBlank())
                                Text(m.customerId, style = MaterialTheme.typography.bodySmall)
                        }
                        if (m.cardMissing) Text("❌ Card nahi", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        val missing = members.count { it.cardMissing }
        Text("Card missing: $missing members", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                FillSession.cdsData = cds?.copy(members = members)
                onConfirm()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("✅ Confirm Karke Auto-Fill Karo →") }
    }
}
