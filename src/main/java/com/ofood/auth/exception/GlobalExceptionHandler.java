package com.ofood.auth.exception;

import com.ofood.auth.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleBadCredentials(BadCredentialsException ex) {
        return new ApiErrorResponse("INVALID_CREDENTIALS", "Invalid email or password", org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleDisabled(org.springframework.security.authentication.DisabledException ex) {
        return new ApiErrorResponse(ex.getMessage().contains("suspended") ? "ACCOUNT_SUSPENDED" : "ACCOUNT_DISABLED", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleLocked(org.springframework.security.authentication.LockedException ex) {
        return new ApiErrorResponse("ACCOUNT_SUSPENDED", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ApiErrorResponse("BAD_REQUEST", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleIllegalState(IllegalStateException ex) {
        return new ApiErrorResponse("CONFLICT", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(com.ofood.auth.exception.DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicateEmailException(com.ofood.auth.exception.DuplicateEmailException ex) {
        return new ApiErrorResponse("EMAIL_ALREADY_EXISTS", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ApiErrorResponse("VALIDATION_FAILED", message, org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(ex.getStatusCode().toString(), ex.getReason(), org.slf4j.MDC.get("traceId")));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return new ApiErrorResponse("FORBIDDEN", "Access Denied", org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        return new ApiErrorResponse("UNAUTHORIZED", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(com.ofood.auth.exception.InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidRefreshTokenException(com.ofood.auth.exception.InvalidRefreshTokenException ex) {
        return new ApiErrorResponse("INVALID_REFRESH_TOKEN", ex.getMessage(), org.slf4j.MDC.get("traceId"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleException(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unhandled exception", ex);
        return new ApiErrorResponse("INTERNAL_SERVER_ERROR", "Unexpected error", org.slf4j.MDC.get("traceId"));
    }
}
