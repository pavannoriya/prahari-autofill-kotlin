package com.prahari.autofill.data.api

import com.prahari.autofill.data.models.CdsData
import com.prahari.autofill.data.models.ChatCommand
import com.prahari.autofill.data.models.Member
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
 * Gemini API — direct REST calls (generativelanguage.googleapis.com)
 * 1. CDS photo reading (Vision)
 * 2. Chat command parsing
 */
class GeminiApi(private val secureStorage: SecureStorage) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private fun apiKey(): String =
        secureStorage.getGeminiKey() ?: throw IllegalStateException("Gemini API key set nahi hai")

    private suspend fun generateContent(parts: JsonArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("contents", buildJsonArray {
                    addJsonObject { put("parts", parts) }
                })
                put("generationConfig", buildJsonObject {
                    put("temperature", 0.2)
                })
            }.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${apiKey()}")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini error ${resp.code}: ${text.take(200)}"))
                }
                val obj = json.parseToJsonElement(text).jsonObject
                val content = obj["candidates"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: return@withContext Result.failure(Exception("Gemini se jawab nahi mila"))
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- CDS photo padho ----------
    suspend fun readCDS(photoBase64: String): Result<CdsData> {
        val parts = buildJsonArray {
            addJsonObject { put("text", CDS_PROMPT) }
            addJsonObject {
                put("inline_data", buildJsonObject {
                    put("mime_type", "image/jpeg")
                    put("data", photoBase64)
                })
            }
        }
        val r = generateContent(parts)
        return r.mapCatching { text ->
            val jsonStr = extractJson(text)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            CdsData(
                centerCode = obj["center_code"]?.jsonPrimitive?.contentOrNull ?: "",
                centerName = obj["center_name"]?.jsonPrimitive?.contentOrNull ?: "",
                meetingDay = obj["meeting_day"]?.jsonPrimitive?.contentOrNull ?: "",
                meetingTime = obj["meeting_time"]?.jsonPrimitive?.contentOrNull ?: "",
                members = obj["members"]?.jsonArray?.mapNotNull { el ->
                    try {
                        val m = el.jsonObject
                        Member(
                            serial = m["serial"]?.jsonPrimitive?.intOrNull ?: 0,
                            name = m["name"]?.jsonPrimitive?.contentOrNull ?: "",
                            customerId = m["customer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                            cardMissing = m["card_missing"]?.jsonPrimitive?.booleanOrNull ?: false,
                            cardMissingMark = m["card_missing_mark"]?.jsonPrimitive?.contentOrNull ?: ""
                        )
                    } catch (_: Exception) { null }
                } ?: emptyList(),
                totalMembersVisible = obj["total_members_visible"]?.jsonPrimitive?.intOrNull ?: 0,
                notes = obj["notes"]?.jsonPrimitive?.contentOrNull ?: ""
            )
        }
    }

    // ---------- Chat command parse karo ----------
    suspend fun parseChatCommand(message: String): Result<ChatCommand> {
        val parts = buildJsonArray {
            addJsonObject { put("text", "$CHAT_PROMPT\n\nUser message: \"$message\"") }
        }
        val r = generateContent(parts)
        return r.mapCatching { text ->
            val jsonStr = extractJson(text)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            ChatCommand(
                action = obj["action"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                point = obj["point"]?.jsonPrimitive?.contentOrNull,
                field = obj["field"]?.jsonPrimitive?.contentOrNull,
                value = obj["value"]?.jsonPrimitive?.contentOrNull
            )
        }
    }

    private fun extractJson(text: String): String {
        // Markdown code blocks hatao
        val cleaned = text.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end < 0 || end <= start) throw IllegalStateException("CDS data parse nahi hua")
        return cleaned.substring(start, end + 1)
    }

    companion object {
        const val MODEL = "gemini-1.5-flash"

        const val CDS_PROMPT = """You are reading a Collection Demand Sheet (CDS) from Sonata Microfinance.
This is a field audit document with handwritten Hindi text.

Extract the following in JSON format:
{
  "center_code": "e.g. NER:RRL:03:349",
  "center_name": "e.g. Devnagar2nd",
  "meeting_day": "e.g. Monday",
  "meeting_time": "e.g. 10:00 AM",
  "members": [
    {
      "serial": 1,
      "name": "member name",
      "customer_id": "if visible",
      "card_missing": true/false,
      "card_missing_mark": "describe any tick/cross/handwritten mark indicating card issue"
    }
  ],
  "total_members_visible": 0,
  "notes": "any other relevant observations"
}

Look carefully for:
- Handwritten "कार्ड" (card) notations
- Ticks, crosses, or circles next to member names
- Any member marked as not having received loan card

Return ONLY valid JSON, no other text."""

        const val CHAT_PROMPT = """You are a command parser for a field audit app.
The user sends messages in Hindi/Hinglish to change audit fill rules.

Parse the message and return JSON:
{
  "action": "update_rule" | "show_rules" | "reset_rules" | "unknown",
  "point": "P1" to "P10" (if applicable),
  "field": "default" | "customRemark" (if applicable),
  "value": "Yes" | "No" | "remark text" (if applicable)
}

Examples:
- "P4 No kar do" → {"action":"update_rule","point":"P4","field":"default","value":"No"}
- "P7 hamesha Yes" → {"action":"update_rule","point":"P7","field":"default","value":"Yes"}
- "P8 ka remark change karo: बैठक नहीं हो रही" → {"action":"update_rule","point":"P8","field":"customRemark","value":"बैठक नहीं हो रही"}
- "P9 default remark" → {"action":"update_rule","point":"P9","field":"customRemark","value":null}
- "Rules dikhao" → {"action":"show_rules"}
- "Rules reset karo" → {"action":"reset_rules"}

Return ONLY valid JSON."""
    }
}
