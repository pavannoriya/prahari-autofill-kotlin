package com.prahari.autofill.ui.navigation

/** Saare screens ke routes */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PRAHARI_CONNECT = "prahari_connect"
    const val MAIN = "main"

    // Fill flow
    const val AUDIT_LIST = "audit_list"
    const val CENTER_LIST = "center_list/{auditId}/{branchId}"
    const val CDS_PHOTO = "cds_photo/{auditId}/{branchId}/{centerId}"
    const val CDS_RESULTS = "cds_results/{auditId}/{branchId}/{centerId}"
    const val AUTO_FILL = "auto_fill/{auditId}/{branchId}/{centerId}"
    const val CONFIRMATION = "confirmation"

    // Tabs
    const val TAB_FILL = "tab_fill"
    const val TAB_CHAT = "tab_chat"
    const val TAB_RULES = "tab_rules"

    fun centerList(auditId: String, branchId: String) = "center_list/$auditId/$branchId"
    fun cdsPhoto(auditId: String, branchId: String, centerId: String) = "cds_photo/$auditId/$branchId/$centerId"
    fun cdsResults(auditId: String, branchId: String, centerId: String) = "cds_results/$auditId/$branchId/$centerId"
    fun autoFill(auditId: String, branchId: String, centerId: String) = "auto_fill/$auditId/$branchId/$centerId"
}
