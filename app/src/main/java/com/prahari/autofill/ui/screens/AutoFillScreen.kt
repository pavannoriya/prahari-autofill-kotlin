package com.prahari.autofill.ui.screens

import androidx.compose.foundation.layout.*
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

/**
 * AutoFill — 10 points calculate → Prahari me save → verify
 */
@Composable
fun AutoFillScreen(onDone: (success: Boolean, message: String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PrahariApp
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf("Shuru ho raha hai...") }
    var progress by remember { mutableStateOf(0f) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val uid = app.firebase.currentUid ?: throw Exception("Login nahi ho")
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                // Step 1: Member count (API se!)
                step = "Member count le rahe hain..."; progress = 0.15f
                val countR = app.prahariApi.getMemberCount(FillSession.centerId, today)
                val memberCount = countR.getOrElse { FillSession.cdsData?.members?.size ?: 0 }
                FillSession.memberCount = memberCount

                // Step 2: Rules lao
                step = "Rules load ho rahe hain..."; progress = 0.3f
                val rulesR = app.firebase.getRules(uid)
                val rules = rulesR.getOrElse { Rulebook.defaultRules() }

                // Step 3: Answers calculate
                step = "10 points calculate ho rahe hain..."; progress = 0.5f
                val cardMissing = FillSession.cdsData?.cardMissingMembers ?: emptyList()
                val answers = Rulebook.calculateAnswers(
                    memberCount = memberCount,
                    cardMissingMembers = cardMissing,
                    customRules = rules,
                    cdsPhotoBase64 = FillSession.cdsPhotoBase64
                )
                FillSession.answers = answers

                // Step 4: Prahari me save
                step = "Prahari me save ho raha hai..."; progress = 0.7f
                val saveR = app.prahariApi.saveCenterFeedback(
                    centerId = FillSession.centerId,
                    auditId = FillSession.auditId,
                    branchId = FillSession.branchId,
                    answers = answers
                )
                if (saveR.isFailure) throw saveR.exceptionOrNull() ?: Exception("Save failed")

                // Step 5: Verify (re-read)
                step = "Verify ho raha hai..."; progress = 0.85f
                app.prahariApi.verifyCenterFill(FillSession.centerId, today)

                // Step 6: History save
                step = "History save ho rahi hai..."; progress = 0.95f
                app.firebase.saveFillHistory(uid, mapOf(
                    "centerId" to FillSession.centerId,
                    "centerName" to FillSession.centerName,
                    "auditId" to FillSession.auditId,
                    "memberCount" to memberCount,
                    "answers" to answers.map { "${it.point}=${it.answer}" },
                    "cardMissing" to cardMissing.size
                ))

                progress = 1f; done = true
                step = "Ho gaya! ✅"
                onDone(true, "Center fill ho gaya!")
            } catch (e: Exception) {
                done = true
                step = "Error: ${e.message}"
                onDone(false, e.message ?: "Error")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Auto Filling...", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(FillSession.centerName, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        if (!done) CircularProgressIndicator(modifier = Modifier.size(64.dp))
        else if (progress >= 1f) Text("✅", style = MaterialTheme.typography.displayLarge)
        else Text("❌", style = MaterialTheme.typography.displayLarge)

        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(step, style = MaterialTheme.typography.bodyMedium)
    }
}
