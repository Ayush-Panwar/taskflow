package com.taskflow.security

import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtConverter: JwtToAuthenticatedUserConverter,
    private val errorHandler: SecurityErrorHandler,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Tokens travel in a header, which browsers never attach on their own, so CSRF does not apply.
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtConverter) }
                it.authenticationEntryPoint(errorHandler)
                it.accessDeniedHandler(errorHandler)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(errorHandler)
                it.accessDeniedHandler(errorHandler)
            }
        return http.build()
    }

    /** An old token sent along with a login request must not stop the user from logging in. */
    @Bean
    fun bearerTokenResolver(): BearerTokenResolver {
        val defaultResolver = DefaultBearerTokenResolver()
        return BearerTokenResolver { request ->
            if (request.requestURI.startsWith("/api/auth/")) null else defaultResolver.resolve(request)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
