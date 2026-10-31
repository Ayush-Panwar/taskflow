package com.taskflow.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/** The logged-in user as controllers see it. Inject with @AuthenticationPrincipal. */
data class AuthenticatedUser(val id: Long)

/** Runs after a token has been verified, and turns its subject claim into an [AuthenticatedUser]. */
@Component
class JwtToAuthenticatedUserConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val user = AuthenticatedUser(id = jwt.subject.toLong())
        return UsernamePasswordAuthenticationToken.authenticated(user, jwt, emptyList())
    }
}
