package com.pangreksa.crm.security

import com.pangreksa.crm.base.ValidationException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwt: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            try {
                val principal = jwt.toPrincipal(jwt.parse(header.substring(7)))
                val authorities = principal.permissions.map { SimpleGrantedAuthority(it) }
                val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: Exception) {
                SecurityContextHolder.clearContext()
            }
        }
        chain.doFilter(request, response)
    }
}

object CurrentUser {
    fun currentOrNull(): AuthPrincipal? =
        SecurityContextHolder.getContext().authentication?.principal as? AuthPrincipal

    fun require(): AuthPrincipal =
        currentOrNull() ?: throw ValidationException(null, "Not authenticated")
}
