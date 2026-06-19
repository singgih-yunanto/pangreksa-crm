package com.pangreksa.crm.security

/** The authenticated principal placed in the SecurityContext by [JwtAuthFilter]. */
data class AuthPrincipal(
    val userId: Long,
    val username: String,
    val fullName: String,
    val roleId: Long?,
    val roleName: String?,
    val permissions: Set<String>,
)
