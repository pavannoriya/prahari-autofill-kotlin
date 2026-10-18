package com.prahari.autofill.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prahari.autofill.data.models.RuleEntry
import kotlinx.coroutines.tasks.await

/**
 * Firebase Manager — Auth + Firestore
 * Collections: users/{uid}, rules/{uid}, fill_history/{uid}/fills
 */
class FirebaseManager {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ---------- Register ----------
    suspend fun register(
        email: String,
        password: String,
        name: String,
        prahariUsername: String,
        empId: String,
        phone: String
    ): Result<String> = runCatching {
        val cred = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = cred.user!!.uid
        db.collection("users").document(uid).set(mapOf(
            "name" to name,
            "prahariUsername" to prahariUsername,
            "empId" to empId,
            "phone" to phone,
            "role" to "auditor",
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastLogin" to FieldValue.serverTimestamp()
        )).await()
        // Default rules
        val defaults = mutableMapOf<String, Any>()
        for (p in POINTS) {
            defaults[p] = mapOf(
                "default" to if (p == "P8" || p == "P9") "Yes" else "No",
                "customRemark" to null
            )
        }
        defaults["updatedAt"] = FieldValue.serverTimestamp()
        db.collection("rules").document(uid).set(defaults).await()
        uid
    }

    // ---------- Login / Logout ----------
    suspend fun login(email: String, password: String): Result<String> = runCatching {
        val cred = auth.signInWithEmailAndPassword(email, password).await()
        cred.user!!.uid
    }

    fun logout() = auth.signOut()

    // ---------- Profile ----------
    suspend fun getProfile(uid: String): Result<Map<String, Any?>> = runCatching {
        val snap = db.collection("users").document(uid).get().await()
        snap.data ?: emptyMap()
    }

    // ---------- Rules ----------
    suspend fun getRules(uid: String): Result<Map<String, RuleEntry>> = runCatching {
        val snap = db.collection("rules").document(uid).get().await()
        val data = snap.data ?: return@runCatching defaultRules()
        val out = mutableMapOf<String, RuleEntry>()
        for (p in POINTS) {
            @Suppress("UNCHECKED_CAST")
            val entry = data[p] as? Map<String, Any?>
            out[p] = RuleEntry(
                default = entry?.get("default") as? String ?: if (p == "P8" || p == "P9") "Yes" else "No",
                customRemark = entry?.get("customRemark") as? String
            )
        }
        out
    }

    suspend fun updateRule(uid: String, point: String, field: String, value: String?): Result<Unit> = runCatching {
        val ref = db.collection("rules").document(uid)
        val snap = ref.get().await()
        val rules = (snap.data ?: mutableMapOf<String, Any>()).toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val entry = (rules[point] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf()
        if (value == null) entry.remove(field) else entry[field] = value
        rules[point] = entry
        rules["updatedAt"] = FieldValue.serverTimestamp()
        rules["updatedVia"] = "chat"
        ref.set(rules).await()
    }

    suspend fun resetRules(uid: String): Result<Unit> = runCatching {
        val defaults = mutableMapOf<String, Any>()
        for (p in POINTS) {
            defaults[p] = mapOf(
                "default" to if (p == "P8" || p == "P9") "Yes" else "No",
                "customRemark" to null
            )
        }
        defaults["updatedAt"] = FieldValue.serverTimestamp()
        db.collection("rules").document(uid).set(defaults).await()
    }

    // ---------- Fill history ----------
    suspend fun saveFillHistory(uid: String, fillData: Map<String, Any?>): Result<Unit> = runCatching {
        val data = fillData.toMutableMap()
        data["filledAt"] = FieldValue.serverTimestamp()
        db.collection("fill_history").document(uid).collection("fills").add(data).await()
    }

    companion object {
        val POINTS = listOf("P1","P2","P3","P4","P5","P6","P7","P8","P9","P10")

        fun defaultRules(): Map<String, RuleEntry> =
            POINTS.associateWith { p ->
                RuleEntry(
                    default = if (p == "P8" || p == "P9") "Yes" else "No",
                    customRemark = null
                )
            }
    }
}
