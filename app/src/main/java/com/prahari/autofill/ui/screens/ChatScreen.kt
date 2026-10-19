package com.prahari.autofill.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.prahari.autofill.data.rulebook.Rulebook
import com.prahari.autofill.ui.navigation.FillSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMsg(val fromUser: Boolean, val text: String, val isPhoto: Boolean = false)

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMsg(false, "Namaste! 🙏\n\n📸 **CDS photo bhejo** — main padhke center dhoondunga aur 10 points auto-fill kar dunga!\n\nYa rules change karne ke liye likho — jaise \"P4 No kar do\"")
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var pendingFill by remember { mutableStateOf<PendingFill?>(null) }

    fun addMsg(fromUser: Boolean, text: String) {
        messages = messages + ChatMsg(fromUser, text)
    }

    // ---------- CDS photo processing ----------
    fun processCDS(photoBase64: String) {
        thinking = true
        scope.launch {
            try {
                addMsg(false, "📸 CDS mil gaya! Padh raha hun...")
                
                // Step 1: Vision se CDS padho
                val cds = app.geminiApi.readCDS(photoBase64).getOrThrow()
                addMsg(false, "📋 Center mila: **${cds.centerName}** (${cds.centerCode})")
                
                // Step 2: Audit select karo (pehla available)
                if (FillSession.auditId.isEmpty()) {
                    addMsg(false, "🔍 Assigned audits le raha hun...")
                    val audits = app.prahariApi.getAudits().getOrThrow()
                    // Sirf valid audits (jisme id ho)
                    val validAudits = audits.filter { it.id.isNotEmpty() }
                    if (validAudits.isEmpty()) throw Exception("Koi audit nahi mila. Pehle Prahari me audit check karo — kya tumhare naam pe koi audit assigned hai?")
                    val audit = validAudits.first()
                    FillSession.auditId = audit.id
                    FillSession.auditName = audit.name.ifEmpty { "Audit ${audit.id}" }
                    FillSession.branchId = audit.branchId
                    FillSession.branchName = audit.branchName
                    addMsg(false, "📁 Audit: ${FillSession.auditName} (Branch: ${audit.branchName.ifEmpty { audit.branchId }})")
                }
                
                // Step 3: Center dhoondho
                addMsg(false, "🔍 Center dhoond raha hun...")
                val centers = app.prahariApi.getSelectedCenters(FillSession.auditId).getOrThrow()
                val center = centers.find {
                    it.centerCode.equals(cds.centerCode, true) ||
                    it.centerName.equals(cds.centerName, true)
                } ?: throw Exception("Center '${cds.centerCode}' audit me nahi mila. Pehle Prahari me center select karo.")
                
                FillSession.centerId = center.centerId
                FillSession.centerCode = center.centerCode
                FillSession.centerName = center.centerName
                FillSession.cdsPhotoBase64 = photoBase64
                FillSession.cdsData = cds
                
                addMsg(false, "✅ Center confirm: **${center.centerName}** (ID: ${center.centerId})")
                
                // Step 4: Member count (API se!)
                addMsg(false, "👥 Member count le raha hun...")
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val memberCount = app.prahariApi.getMemberCount(center.centerId, today)
                    .getOrElse { cds.members.size }
                FillSession.memberCount = memberCount
                addMsg(false, "👥 Members: $memberCount")
                
                // Step 5: Answers calculate
                addMsg(false, "🧮 10 points calculate ho rahe hain...")
                val uid = app.firebase.currentUid ?: throw Exception("Login nahi ho")
                val rules = app.firebase.getRules(uid).getOrElse { Rulebook.defaultRules() }
                val cardMissing = cds.cardMissingMembers
                val answers = Rulebook.calculateAnswers(
                    memberCount = memberCount,
                    cardMissingMembers = cardMissing,
                    customRules = rules,
                    cdsPhotoBase64 = photoBase64
                )
                FillSession.answers = answers
                
                // Step 6: Summary dikhao
                val summary = buildString {
                    append("📝 **Fill Summary:**\n\n")
                    for (a in answers) {
                        append("${a.point}: ${a.answer}")
                        if (!a.remark.isNullOrEmpty()) append(" (remark)")
                        if (a.evidenceType != null) append(" 📸")
                        append("\n")
                    }
                    if (cardMissing.isNotEmpty()) {
                        append("\n⚠️ Card missing: ${cardMissing.size} members\n")
                        for (m in cardMissing) append("  • ${m.name}\n")
                    }
                    append("\nSave karun? **'ha'** likho ya **'na'** likho!")
                }
                addMsg(false, summary)
                pendingFill = PendingFill(answers, photoBase64, cardMissing.size)
                
            } catch (e: Exception) {
                addMsg(false, "❌ Error: ${e.message}")
            }
            thinking = false
        }
    }

    // ---------- Confirmation handling ----------
    fun handleConfirmation(msg: String): Boolean {
        val pf = pendingFill ?: return false
        val lower = msg.lowercase().trim()
        return when {
            lower in listOf("ha", "haan", "yes", "y", "kar do", "save karo", "ok") -> {
                thinking = true
                scope.launch {
                    try {
                        addMsg(false, "💾 Prahari me save ho raha hai...")
                        val uid = app.firebase.currentUid ?: throw Exception("Login nahi ho")
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        
                        val saveR = app.prahariApi.saveCenterFeedback(
                            centerId = FillSession.centerId,
                            auditId = FillSession.auditId,
                            branchId = FillSession.branchId,
                            answers = pf.answers
                        )
                        if (saveR.isFailure) throw saveR.exceptionOrNull() ?: Exception("Save failed")
                        
                        // Verify
                        app.prahariApi.verifyCenterFill(FillSession.centerId, today)
                        
                        // History
                        app.firebase.saveFillHistory(uid, mapOf(
                            "centerId" to FillSession.centerId,
                            "centerName" to FillSession.centerName,
                            "auditId" to FillSession.auditId,
                            "memberCount" to FillSession.memberCount,
                            "answers" to pf.answers.map { "${it.point}=${it.answer}" },
                            "cardMissing" to pf.cardMissingCount
                        ))
                        
                        addMsg(false, "✅ **Ho gaya!** Center fill ho gaya aur verify bhi!\n\n📸 Agla CDS bhejo!")
                        pendingFill = null
                    } catch (e: Exception) {
                        addMsg(false, "❌ Save me error: ${e.message}")
                    }
                    thinking = false
                }
                true
            }
            lower in listOf("na", "nahi", "no", "n", "cancel") -> {
                addMsg(false, "🚫 Cancel kar diya. Kuch change karna hai to batao!")
                pendingFill = null
                true
            }
            else -> false
        }
    }

    // ---------- Text message handling ----------
    fun send() {
        val msg = input.trim()
        if (msg.isEmpty() || thinking) return
        input = ""
        addMsg(true, msg)
        
        // Pehle confirmation check karo
        if (handleConfirmation(msg)) return
        
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
                    else -> "🤔 Samajh nahi aaya.\n\n📸 CDS photo bhejo auto-fill ke liye\n📋 \"Rules dikhao\" — rules dekhne ke liye\n✏️ \"P4 No kar do\" — rule change karne ke liye"
                }
                addMsg(false, reply)
            } catch (e: Exception) {
                addMsg(false, "❌ Error: ${e.message}")
            }
            thinking = false
        }
    }

    // ---------- Photo picker ----------
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            thinking = true
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                        ?: throw Exception("Photo read nahi ho payi")
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    addMsg(true, "📸 CDS photo bheji")
                    processCDS(base64)
                } catch (e: Exception) {
                    addMsg(false, "❌ Photo error: ${e.message}")
                    thinking = false
                }
            }
        }
    }

    // ---------- UI ----------
    Column(Modifier.fillMaxSize()) {
        Text(
            "AutoFill Chat 💬",
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
            // Photo attach button
            IconButton(onClick = { photoPicker.launch("image/*") }, enabled = !thinking) {
                Text("📎", style = MaterialTheme.typography.headlineSmall)
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("CDS bhejo ya likho...") },
                modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { send() }, enabled = !thinking) { Text("➤") }
        }
    }
}

private data class PendingFill(
    val answers: List<com.prahari.autofill.data.models.FillAnswer>,
    val photoBase64: String,
    val cardMissingCount: Int
)
