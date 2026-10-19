package com.prahari.autofill.data.api

import com.prahari.autofill.data.models.Audit
import com.prahari.autofill.data.models.Center
import com.prahari.autofill.data.models.FillAnswer
import com.prahari.autofill.data.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Prahari API Client — saari requests phone se direct (VPN on hona chahiye)
 * Base: https://prahari.sonataindia.com
 */
class PrahariApi(private val secureStorage: SecureStorage) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // ---------- Login ----------
    suspend fun login(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("username", username)
                put("password", password)
            }.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL/backend/auth/login/")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Login failed: ${resp.code}"))
                val obj = json.parseToJsonElement(text).jsonObject
                val token = obj["access_token"]?.jsonPrimitive?.contentOrNull
                    ?: obj["token"]?.jsonPrimitive?.contentOrNull
                    ?: obj["access"]?.jsonPrimitive?.contentOrNull
                    ?: obj["jwt"]?.jsonPrimitive?.contentOrNull
                    ?: return@withContext Result.failure(Exception(obj["message"]?.jsonPrimitive?.contentOrNull ?: "Login failed"))
                secureStorage.savePrahariToken(token)
                secureStorage.savePrahariUsername(username)
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Generic POST (token ke saath) ----------
    private suspend fun post(endpoint: String, payload: Map<String, Any?> = emptyMap()): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            try {
                val token = secureStorage.getPrahariToken()
                    ?: return@withContext Result.failure(Exception("Prahari me login nahi ho"))
                val bodyJson = buildJsonObject {
                    put("token", token)
                    for ((k, v) in payload) {
                        when (v) {
                            null -> put(k, JsonNull)
                            is String -> put(k, v)
                            is Number -> put(k, v.toDouble())
                            is Boolean -> put(k, v)
                            else -> put(k, v.toString())
                        }
                    }
                }.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL$endpoint")
                    .post(bodyJson)
                    .build()

                client.newCall(request).execute().use { resp ->
                    val text = resp.body?.string() ?: "{}"
                    if (!resp.isSuccessful) return@withContext Result.failure(Exception("API Error: ${resp.code}"))
                    Result.success(json.parseToJsonElement(text).jsonObject)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---------- Assigned audits ----------
    suspend fun getAudits(): Result<List<Audit>> {
        val r = post("/backend/audit/list/")
        return r.map { obj ->
            val arr = obj["audits"]?.jsonArray
                ?: obj["data"]?.jsonArray
                ?: obj["results"]?.jsonArray
                ?: emptyList()
            arr.mapNotNull { el ->
                try {
                    val o = el.jsonObject
                    Audit(
                        id = o["id"]?.jsonPrimitive?.contentOrNull ?: o["audit_id"]?.jsonPrimitive?.contentOrNull ?: "",
                        name = o["name"]?.jsonPrimitive?.contentOrNull ?: o["audit_name"]?.jsonPrimitive?.contentOrNull ?: "",
                        branchId = o["branch_id"]?.jsonPrimitive?.contentOrNull ?: "",
                        branchName = o["branch_name"]?.jsonPrimitive?.contentOrNull ?: "",
                        startDate = o["start_date"]?.jsonPrimitive?.contentOrNull ?: "",
                        endDate = o["end_date"]?.jsonPrimitive?.contentOrNull ?: ""
                    )
                } catch (_: Exception) { null }
            }
        }
    }

    // ---------- Selected centers for an audit ----------
    suspend fun getSelectedCenters(auditId: String): Result<List<Center>> {
        val r = post("/backend/audit/selected-centers/", mapOf("audit_id" to auditId))
        return r.map { obj ->
            val arr = obj["centers"]?.jsonArray
                ?: obj["data"]?.jsonArray
                ?: obj["results"]?.jsonArray
                ?: emptyList()
            arr.mapNotNull { el ->
                try {
                    val o = el.jsonObject
                    Center(
                        centerId = o["center_id"]?.jsonPrimitive?.contentOrNull
                            ?: o["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        centerCode = o["center_code"]?.jsonPrimitive?.contentOrNull
                            ?: o["centercode"]?.jsonPrimitive?.contentOrNull ?: "",
                        centerName = o["center_name"]?.jsonPrimitive?.contentOrNull
                            ?: o["centername"]?.jsonPrimitive?.contentOrNull ?: "",
                        meetingDay = o["meeting_day"]?.jsonPrimitive?.contentOrNull ?: "",
                        meetingTime = o["meeting_time"]?.jsonPrimitive?.contentOrNull ?: "",
                        auditId = auditId,
                        branchId = o["branch_id"]?.jsonPrimitive?.contentOrNull ?: ""
                    )
                } catch (_: Exception) { null }
            }
        }
    }

    // ---------- Center risk details ----------
    suspend fun getCenterDetails(centerId: String, asOnDate: String): Result<JsonObject> =
        post("/backend/audit/centers/risk-details/", mapOf("center_id" to centerId, "as_on_date" to asOnDate))

    // ---------- Member count (hamesha API se!) ----------
    suspend fun getMemberCount(centerId: String, asOnDate: String): Result<Int> {
        val r = post("/backend/audit/centers/risk-details/", mapOf("center_id" to centerId, "as_on_date" to asOnDate))
        return r.map { obj ->
            obj["customer_risks"]?.jsonArray?.size
                ?: obj["customers"]?.jsonArray?.size
                ?: 0
        }
    }

    // ---------- Disbursements (P4 Excel ke liye) ----------
    suspend fun getDisbursements(centerId: String, asOnDate: String): Result<JsonObject> =
        post("/backend/audit/centers/disbursements/", mapOf("center_id" to centerId, "as_on_date" to asOnDate))

    // ---------- Center fill save ----------
    suspend fun saveCenterFeedback(
        centerId: String,
        auditId: String,
        branchId: String,
        answers: List<FillAnswer>,
        checklistIdMap: Map<String, String> = emptyMap()
    ): Result<JsonObject> {
        val items = buildJsonArray {
            for (a in answers) {
                addJsonObject {
                    put("checklist_id", checklistIdMap[a.point] ?: a.point)
                    put("parameter_code", CHECKLIST_CODES[a.point] ?: a.point)
                    put("answer", a.answer)
                    // Remark sirf Yes pe
                    put("normal_remark", if (a.answer == "Yes") (a.remark ?: "") else "")
                    when (a.evidenceType) {
                        "CDS_PHOTO" -> put("evidence_image", buildJsonObject {
                            put("base64", a.evidenceData ?: "")
                            put("filename", "cds.jpg")
                            put("latitude", JsonNull)
                            put("longitude", JsonNull)
                            put("image_text", JsonNull)
                        })
                        else -> put("evidence_image", JsonNull)
                    }
                }
            }
        }
        // Raw post with JsonArray payload
        return withContext(Dispatchers.IO) {
            try {
                val token = secureStorage.getPrahariToken()
                    ?: return@withContext Result.failure(Exception("Prahari me login nahi ho"))
                val bodyJson = buildJsonObject {
                    put("token", token)
                    put("center_id", centerId)
                    put("audit_id", auditId)
                    put("branch_id", branchId)
                    put("action", "DRAFT_SAVED")
                    put("feedback_items", items)
                }.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL/backend/audit/center-feedback/save/")
                    .post(bodyJson)
                    .build()

                client.newCall(request).execute().use { resp ->
                    val text = resp.body?.string() ?: "{}"
                    if (!resp.isSuccessful) return@withContext Result.failure(Exception("Save failed: ${resp.code}"))
                    Result.success(json.parseToJsonElement(text).jsonObject)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ---------- Verify: save ke baad re-read ----------
    suspend fun verifyCenterFill(centerId: String, asOnDate: String): Result<JsonObject> =
        getCenterDetails(centerId, asOnDate)

    companion object {
        const val BASE_URL = "https://prahari.sonataindia.com"
        val CHECKLIST_CODES = mapOf(
            "P1" to "CCP_001", "P2" to "CCP_002", "P3" to "CCP_003",
            "P4" to "CCP_004", "P5" to "CCP_005", "P6" to "CCP_006",
            "P7" to "CCP_007", "P8" to "CCP_008", "P9" to "CCP_009",
            "P10" to "CCP_010"
        )
    }
}
