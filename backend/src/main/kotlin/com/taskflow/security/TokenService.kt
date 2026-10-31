package com.taskflow.security

import com.taskflow.user.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

data class IssuedToken(val value: String, val expiresAt: Instant)

@Service
class TokenService(
    private val encoder: JwtEncoder,
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    fun issueFor(user: User): IssuedToken {
        val issuedAt = clock.instant()
        val expiresAt = issuedAt.plus(properties.expiry)
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(user.id.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims))
        return IssuedToken(token.tokenValue, expiresAt)
    }
}
