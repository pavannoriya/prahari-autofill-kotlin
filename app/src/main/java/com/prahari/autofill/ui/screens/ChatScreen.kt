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
import kotlinx.coroutines.launch

data class ChatMsg(val fromUser: Boolean, val text: String)

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf(ChatMsg(false, "Namaste! 🙏 Rules change karne ke liye likho — jaise \"P4 No kar do\""))) }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }

    fun send() {
        val msg = input.trim()
        if (msg.isEmpty() || thinking) return
        input = ""
        messages = messages + ChatMsg(true, msg)
        thinking = true
        scope.launch {
            try {
                val uid = app.firebase.currentUid ?: throw Exception("Login nahi ho")
                val cmd = app.geminiApi.parseChatCommand(msg).getOrThrow()
                val reply = when (cmd.action) {
                    "update_rule" -> {
                        val point = cmd.point ?: throw Exception("Point samajh nahi aaya")
                        val field = cmd.field ?: "default"
                        app.firebase.updateRule(uid, point, field, cmd.value).getOrThrow()
                        "✅ $point ka $field update ho gaya: ${cmd.value ?: "default"}"
                    }
                    "show_rules" -> {
                        val rules = app.firebase.getRules(uid).getOrThrow()
                        buildString {
                            append("📋 Tumhare Rules:\n")
                            for (p in listOf("P1","P2","P3","P4","P5","P6","P7","P8","P9","P10")) {
                                val r = rules[p]
                                append("$p: ${r?.default}")
                                r?.customRemark?.let { append(" (remark set)") }
                                append("\n")
                            }
                        }
                    }
                    "reset_rules" -> {
                        app.firebase.resetRules(uid).getOrThrow()
                        "🔄 Saare rules reset ho gaye (default pe)"
                    }
                    else -> "🤔 Samajh nahi aaya. Try karo: \"P4 No kar do\" ya \"Rules dikhao\""
                }
                messages = messages + ChatMsg(false, reply)
            } catch (e: Exception) {
                messages = messages + ChatMsg(false, "❌ Error: ${e.message}")
            }
            thinking = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Rule Chat 💬",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            val reversed = messages.reversed()
            items(reversed) { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (m.fromUser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            m.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (m.fromUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (thinking) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card { Text("Soch raha hun...", Modifier.padding(12.dp)) }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("P4 No kar do...") },
                modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { send() }, enabled = !thinking) { Text("➤") }
        }
    }
}
