package com.taskflow.security

import com.taskflow.support.ApiTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit

class TokenSecurityApiTest : ApiTest() {

    @Autowired
    private lateinit var jwtEncoder: JwtEncoder

    @Autowired
    private lateinit var jwtProperties: JwtProperties

    @ParameterizedTest
    @ValueSource(strings = ["/api/users", "/api/anything-else", "/api/auth/unknown"])
    fun `requests without a token get a 401 in the standard error shape`(path: String) {
        mockMvc.get(path).expectUnauthorized(path)
    }

    @Test
    fun `a garbage token is rejected`() {
        mockMvc.get("/api/users") { header("Authorization", bearer("garbage")) }.expectUnauthorized("/api/users")
    }

    @Test
    fun `a token whose signature was tampered with is rejected`() {
        val token = newUser().token
        val tampered = token.dropLast(4) + if (token.endsWith("AAAA")) "BBBB" else "AAAA"

        mockMvc.get("/api/users") { header("Authorization", bearer(tampered)) }.expectUnauthorized("/api/users")
    }

    @Test
    fun `an expired token is rejected`() {
        val twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS)

        mockMvc.get("/api/users") {
            header("Authorization", bearer(signedToken(subject = "1", issuer = jwtProperties.issuer, expiresAt = twoHoursAgo)))
        }.expectUnauthorized("/api/users")
    }

    @Test
    fun `a token from a different issuer is rejected`() {
        val inOneHour = Instant.now().plus(1, ChronoUnit.HOURS)

        mockMvc.get("/api/users") {
            header("Authorization", bearer(signedToken(subject = "1", issuer = "someone-else", expiresAt = inOneHour)))
        }.expectUnauthorized("/api/users")
    }

    @Test
    fun `a valid token is accepted`() {
        mockMvc.get("/api/users") { header("Authorization", bearer(newUser().token)) }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `the health endpoint is public and reveals no details`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
            jsonPath("$.components") { doesNotExist() }
        }
    }

    @Test
    fun `unknown routes and unsupported methods use the standard error shape`() {
        val token = newUser().token

        mockMvc.get("/api/does-not-exist") { header("Authorization", bearer(token)) }.andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.path") { value("/api/does-not-exist") }
        }
        mockMvc.post("/api/users") {
            header("Authorization", bearer(token))
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.status") { value(405) }
        }
    }

    @Test
    fun `a URL refused by the security firewall still uses the standard error shape`() {
        mockMvc.get(URI("/api/projects;x=1")).andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.code") { value("REQUEST_REJECTED") }
        }
    }

    private fun ResultActionsDsl.expectUnauthorized(path: String) = andExpect {
        status { isUnauthorized() }
        header { string("WWW-Authenticate", "Bearer") }
        jsonPath("$.status") { value(401) }
        jsonPath("$.code") { value("UNAUTHORIZED") }
        jsonPath("$.path") { value(path) }
        jsonPath("$.timestamp") { isNotEmpty() }
    }

    private fun signedToken(subject: String, issuer: String, expiresAt: Instant): String {
        val claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(subject)
            .issuedAt(expiresAt.minus(1, ChronoUnit.HOURS))
            .expiresAt(expiresAt)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
