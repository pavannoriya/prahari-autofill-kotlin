package com.prahari.autofill.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Audit(
    val id: String = "",
    val name: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val startDate: String = "",
    val endDate: String = ""
)

@Serializable
data class Center(
    val centerId: String = "",
    val centerCode: String = "",
    val centerName: String = "",
    val meetingDay: String = "",
    val meetingTime: String = "",
    val auditId: String = "",
    val branchId: String = ""
)

@Serializable
data class Member(
    val serial: Int = 0,
    val name: String = "",
    val customerId: String = "",
    val cardMissing: Boolean = false,
    val cardMissingMark: String = ""
)

@Serializable
data class CdsData(
    val centerCode: String = "",
    val centerName: String = "",
    val meetingDay: String = "",
    val meetingTime: String = "",
    val members: List<Member> = emptyList(),
    val totalMembersVisible: Int = 0,
    val notes: String = ""
) {
    val cardMissingMembers: List<Member>
        get() = members.filter { it.cardMissing }
}

/** 10-point ka ek answer */
data class FillAnswer(
    val point: String,          // P1..P10
    val answer: String,         // Yes / No
    val remark: String? = null, // Hindi remark (sirf Yes pe)
    val evidenceType: String? = null, // CDS_PHOTO / EXCEL / null
    val evidenceData: String? = null,  // CDS photo base64 (data:image/... prefix ke saath)
    val cardMissingMembers: List<Member> = emptyList()
)

data class ChatCommand(
    val action: String, // update_rule | show_rules | reset_rules | unknown
    val point: String? = null,
    val field: String? = null, // default | customRemark
    val value: String? = null
)

data class RuleEntry(
    val default: String = "No",
    val customRemark: String? = null
)
