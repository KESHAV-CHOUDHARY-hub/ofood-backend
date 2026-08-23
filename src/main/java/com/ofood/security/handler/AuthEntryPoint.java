package com.ofood.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String code = "AUTHENTICATION_REQUIRED";
        String message = "Authentication is required";
        
        if (authException instanceof org.springframework.security.oauth2.server.resource.InvalidBearerTokenException invalidTokenEx) {
            code = "ACCESS_TOKEN_INVALID";
            message = "Access token is invalid";
            
            if (invalidTokenEx.getCause() instanceof org.springframework.security.oauth2.jwt.JwtValidationException jwtEx) {
                boolean isExpired = jwtEx.getErrors().stream()
                        .anyMatch(err -> "expired_token".equals(err.getErrorCode()));
                if (isExpired) {
                    code = "ACCESS_TOKEN_EXPIRED";
                    message = "Access token has expired";
                }
            }
        }
        
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId == null) {
            traceId = java.util.UUID.randomUUID().toString();
        }
        
        com.ofood.auth.dto.ApiErrorResponse error = new com.ofood.auth.dto.ApiErrorResponse(code, message, traceId);
        mapper.writeValue(response.getOutputStream(), error);
    }
}
