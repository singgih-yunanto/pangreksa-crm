package com.pangreksa.crm.security

import com.pangreksa.crm.user.domain.AppUser
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService(
    @Value("\${pangreksa.jwt.secret}") secret: String,
    @Value("\${pangreksa.jwt.expiry-hours:12}") private val expiryHours: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun issue(user: AppUser): String {
        val now = System.currentTimeMillis()
        val perms = user.role?.permissions ?: emptySet()
        return Jwts.builder()
            .subject(user.username)
            .claim("uid", user.id)
            .claim("fullName", user.fullName)
            .claim("roleId", user.role?.id)
            .claim("roleName", user.role?.name)
            .claim("perms", perms.toList())
            .issuedAt(Date(now))
            .expiration(Date(now + expiryHours * 3_600_000))
            .signWith(key)
            .compact()
    }

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    @Suppress("UNCHECKED_CAST")
    fun toPrincipal(claims: Claims): AuthPrincipal = AuthPrincipal(
        userId = (claims["uid"] as Number).toLong(),
        username = claims.subject,
        fullName = claims["fullName"] as? String ?: claims.subject,
        roleId = (claims["roleId"] as? Number)?.toLong(),
        roleName = claims["roleName"] as? String,
        permissions = ((claims["perms"] as? List<String>) ?: emptyList()).toSet(),
    )
}
