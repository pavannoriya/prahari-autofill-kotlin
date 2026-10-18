package com.prahari.autofill.data.rulebook

import com.prahari.autofill.data.models.FillAnswer
import com.prahari.autofill.data.models.Member
import com.prahari.autofill.data.models.RuleEntry

/**
 * 10-Point Rulebook Logic
 * Har point ka default answer, conditions, Hindi remarks
 */
object Rulebook {

    val CHECKLIST_CODES = mapOf(
        "P1" to "CCP_001", "P2" to "CCP_002", "P3" to "CCP_003",
        "P4" to "CCP_004", "P5" to "CCP_005", "P6" to "CCP_006",
        "P7" to "CCP_007", "P8" to "CCP_008", "P9" to "CCP_009",
        "P10" to "CCP_010"
    )

    val POINTS = listOf("P1","P2","P3","P4","P5","P6","P7","P8","P9","P10")

    // Default Hindi remarks
    const val REMARK_P7 = "केंद्र विजिट के दौरान देखा गया कि स्टाफ केन्द्र मीटिंग के समय से 15 मिनिट विलम्ब से केन्द्र में गया l"
    const val REMARK_P8_FEW = "केंद्र विजिट के दौरान देखा गया की केंद्र में बेठक नहीं लगाई जा रही है सभी सदस्यों के घर जाकर किस्तों की वसूली की जा रही है l"
    const val REMARK_P8_MANY = "विजिट के दौरान देखा गया की सेंटर मीटिंग में 80% सदस्याओं की उपस्थिति नहीं पाई गई l एवं बी आर ओ के द्वारा अनुपस्थित सदस्यों का फॉलो अप नहीं किया जा रहा है l"
    const val REMARK_P9 = "केंद्र विजिट के दौरान देखा गया की केंद्र की सदस्याओ को कंपनी के बीमा ब्याज व टोल फ्री नंबर के बारे में जानकारी का अभाव पाया गया l"

    fun defaultRules(): Map<String, RuleEntry> =
        POINTS.associateWith { p ->
            RuleEntry(
                default = if (p == "P8" || p == "P9") "Yes" else "No",
                customRemark = null
            )
        }

    /**
     * 10 points ke answers calculate karo
     * @param memberCount API se member count (hamesha API se, CDS se nahi!)
     * @param cardMissingMembers Gemini se card-missing list
     * @param customRules Firebase se user ke custom rules
     * @param cdsPhotoBase64 CDS photo base64 (data:image/jpeg;base64,... prefix ke saath)
     */
    fun calculateAnswers(
        memberCount: Int,
        cardMissingMembers: List<Member>,
        customRules: Map<String, RuleEntry> = defaultRules(),
        cdsPhotoBase64: String? = null
    ): List<FillAnswer> {
        val answers = mutableListOf<FillAnswer>()
        fun rule(p: String): RuleEntry = customRules[p] ?: RuleEntry(
            default = if (p == "P8" || p == "P9") "Yes" else "No"
        )

        // P1, P2, P3, P5, P6, P10 — default No
        for (p in listOf("P1", "P2", "P3", "P5", "P6", "P10")) {
            val r = rule(p)
            answers.add(FillAnswer(
                point = p,
                answer = r.default,
                // Remark sirf Yes pe
                remark = if (r.default == "Yes") r.customRemark else null,
                evidenceType = null
            ))
        }

        // P4 — Card missing → Excel evidence
        if (cardMissingMembers.isNotEmpty()) {
            val r = rule("P4")
            answers.add(FillAnswer(
                point = "P4",
                answer = "Yes",
                remark = r.customRemark,
                evidenceType = "EXCEL",
                cardMissingMembers = cardMissingMembers
            ))
        } else {
            val r = rule("P4")
            answers.add(FillAnswer(
                point = "P4",
                answer = r.default,
                remark = if (r.default == "Yes") r.customRemark else null,
                evidenceType = null
            ))
        }

        // P7 — Late staff (members > 2)
        if (memberCount > 2) {
            val r = rule("P7")
            answers.add(FillAnswer(
                point = "P7",
                answer = "Yes",
                remark = r.customRemark ?: REMARK_P7,
                evidenceType = if (cdsPhotoBase64 != null) "CDS_PHOTO" else null,
                evidenceData = cdsPhotoBase64
            ))
        } else {
            val r = rule("P7")
            val ans = if (r.default == "Yes") "Yes" else "No"
            answers.add(FillAnswer(
                point = "P7",
                answer = ans,
                remark = if (ans == "Yes") (r.customRemark ?: REMARK_P7) else null,
                evidenceType = null
            ))
        }

        // P8 — Hamesha Yes + CDS evidence
        run {
            val r = rule("P8")
            val ans = r.default.ifEmpty { "Yes" }
            val remark = r.customRemark ?: if (memberCount <= 3) REMARK_P8_FEW else REMARK_P8_MANY
            answers.add(FillAnswer(
                point = "P8",
                answer = ans,
                remark = if (ans == "Yes") remark else null,
                evidenceType = if (ans == "Yes" && cdsPhotoBase64 != null) "CDS_PHOTO" else null,
                evidenceData = if (ans == "Yes") cdsPhotoBase64 else null
            ))
        }

        // P9 — Hamesha Yes, NO evidence
        run {
            val r = rule("P9")
            val ans = r.default.ifEmpty { "Yes" }
            answers.add(FillAnswer(
                point = "P9",
                answer = ans,
                remark = if (ans == "Yes") (r.customRemark ?: REMARK_P9) else null,
                evidenceType = null // P9 pe kabhi evidence nahi
            ))
        }

        // P1..P10 order me sort karo
        return answers.sortedBy { POINTS.indexOf(it.point) }
    }

    /**
     * P4 ke liye Loan Card Excel ka CSV content banao
     * (Excel khud CSV khol leta hai)
     */
    fun buildLoanCardCsv(
        cardMissingMembers: List<Member>,
        region: String,
        branch: String,
        staffName: String,
        empId: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("S N,Region,Branch,Staff Name,Employee Id,Date of Disbusement,Loan Amount,Loan card status,Name of Client,Client ID")
        cardMissingMembers.forEachIndexed { i, m ->
            sb.appendLine("${i + 1},$region,$branch,$staffName,$empId,,,Not Given,${m.name},${m.customerId}")
        }
        return sb.toString()
    }
}
