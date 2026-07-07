package com.pangreksa.crm.security

/** Permission keys granted to roles and enforced via @PreAuthorize / row-level visibility. */
object Permissions {
    const val VIEW_ALL = "VIEW_ALL"
    const val ADMIN_USERS = "ADMIN_USERS"
    const val ADMIN_ROLES = "ADMIN_ROLES"
    const val REPORT_VIEW = "REPORT_VIEW"

    fun crud(module: String) = listOf("${module}_VIEW", "${module}_CREATE", "${module}_EDIT", "${module}_DELETE")

    val MODULES = listOf("LEAD", "ACCOUNT", "CONTACT", "DEAL", "TASK", "MEETING", "CALL")
    val ALL: List<String> = MODULES.flatMap { crud(it) } + listOf(VIEW_ALL, ADMIN_USERS, ADMIN_ROLES, REPORT_VIEW)
}
