package com.prahari.autofill.ui.navigation

import com.prahari.autofill.data.models.CdsData
import com.prahari.autofill.data.models.FillAnswer

/**
 * Fill flow ka shared state — screens ke beech data pass karne ke liye
 * (React Native me navigation params the, yahan simple holder hai)
 */
object FillSession {
    var auditId: String = ""
    var auditName: String = ""
    var branchId: String = ""
    var branchName: String = ""
    var centerId: String = ""
    var centerCode: String = ""
    var centerName: String = ""

    var cdsPhotoBase64: String? = null  // data:image/jpeg;base64,... prefix ke saath
    var cdsPhotoRawBase64: String? = null // bina prefix (Gemini ke liye)
    var cdsData: CdsData? = null
    var memberCount: Int = 0
    var answers: List<FillAnswer> = emptyList()

    fun reset() {
        cdsPhotoBase64 = null
        cdsPhotoRawBase64 = null
        cdsData = null
        memberCount = 0
        answers = emptyList()
    }

    fun resetAll() {
        auditId = ""; auditName = ""; branchId = ""; branchName = ""
        centerId = ""; centerCode = ""; centerName = ""
        reset()
    }
}
