package org.example.security

import org.springframework.security.core.context.SecurityContextHolder

object CurrentUser {
    fun uid(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: error("not authenticated")
        return auth.principal as? Long ?: error("invalid principal")
    }
}
