package com.taskflow.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(private val properties: JwtProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * No secret is ever stored in the code. Without JWT_SECRET the app still starts safely with a
     * random key; the only cost is that tokens stop working when the app restarts.
     */
    @Bean
    fun jwtSigningKey(): SecretKey {
        val keyBytes = if (properties.secret.isBlank()) {
            log.warn("JWT_SECRET is not set: using a random signing key. Tokens will stop working after a restart.")
            ByteArray(64).also { SecureRandom().nextBytes(it) }
        } else {
            properties.secret.toByteArray()
        }
        require(keyBytes.size >= MIN_KEY_BYTES) { "JWT_SECRET must be at least $MIN_KEY_BYTES bytes long" }
        return SecretKeySpec(keyBytes, "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(key: SecretKey): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))

    /** Checks the signature, the expiry time and the issuer of every incoming token. */
    @Bean
    fun jwtDecoder(key: SecretKey): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .apply { setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer)) }

    private companion object {
        const val MIN_KEY_BYTES = 32 // HS256 needs a key of at least 256 bits.
    }
}
