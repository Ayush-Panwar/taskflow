package com.taskflow.common.error

import org.springframework.http.HttpStatus

/** Base class for failures the API reports on purpose. [GlobalExceptionHandler] turns these into responses. */
abstract class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
    val fieldErrors: List<FieldViolation> = emptyList(),
) : RuntimeException(message)

class NotFoundException(message: String) :
    ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message)

class EmailAlreadyRegisteredException :
    ApiException(
        HttpStatus.CONFLICT,
        "EMAIL_ALREADY_REGISTERED",
        "This email is already registered",
        listOf(FieldViolation("email", "This email is already registered")),
    )

class InvalidCredentialsException :
    ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password")

/** The token is valid but its user no longer exists, for example after the database was reset. */
class UnknownUserException :
    ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please sign in again")

/** A validation failure that needs the database or other logic, so it cannot be a Bean Validation annotation. */
class FieldValidationException(field: String, message: String) :
    ApiException(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Request validation failed",
        listOf(FieldViolation(field, message)),
    )
