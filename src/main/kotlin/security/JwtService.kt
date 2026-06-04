package org.example.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${koim.jwt.secret}") private val secret: String,
    @Value("\${koim.jwt.expire-minutes}") private val expireMinutes: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    fun generate(uid: Long): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(uid.toString())
            .issuedAt(Date(now))
            .expiration(Date(now + expireMinutes * 60_000))
            .signWith(key)
            .compact()
    }

    fun parseUid(token: String): Long? = runCatching {
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject.toLong()
    }.getOrNull()
}
