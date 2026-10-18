package com.prahari.autofill.ui.screens

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
import com.prahari.autofill.data.models.RuleEntry
import kotlinx.coroutines.launch

@Composable
fun MyRulesScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var rules by remember { mutableStateOf<Map<String, RuleEntry>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            val uid = app.firebase.currentUid
            if (uid != null) {
                rules = app.firebase.getRules(uid).getOrElse { emptyMap() }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mere Rules 📋", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = {
                scope.launch {
                    val uid = app.firebase.currentUid ?: return@launch
                    app.firebase.resetRules(uid)
                    load()
                }
            }) { Text("Reset") }
        }
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("P1","P2","P3","P4","P5","P6","P7","P8","P9","P10")) { p ->
                    val r = rules[p]
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p, style = MaterialTheme.typography.titleMedium)
                                r?.customRemark?.let {
                                    Text(it.take(60), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            // Toggle Yes/No
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = r?.default == "Yes",
                                    onClick = {
                                        scope.launch {
                                            val uid = app.firebase.currentUid ?: return@launch
                                            app.firebase.updateRule(uid, p, "default", "Yes")
                                            load()
                                        }
                                    },
                                    label = { Text("Yes") }
                                )
                                Spacer(Modifier.width(8.dp))
                                FilterChip(
                                    selected = r?.default != "Yes",
                                    onClick = {
                                        scope.launch {
                                            val uid = app.firebase.currentUid ?: return@launch
                                            app.firebase.updateRule(uid, p, "default", "No")
                                            load()
                                        }
                                    },
                                    label = { Text("No") }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "💡 Chat me likhke bhi change kar sakte ho: \"P4 No kar do\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
