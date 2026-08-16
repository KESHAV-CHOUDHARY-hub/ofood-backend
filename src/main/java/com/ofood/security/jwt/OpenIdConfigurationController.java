package com.ofood.security.jwt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Discovery", description = "Endpoints for OpenID Configuration and JWKS discovery")
public class OpenIdConfigurationController {

    private final RsaKeyProperties properties;

    public OpenIdConfigurationController(RsaKeyProperties properties) {
        this.properties = properties;
    }

    @Operation(summary = "OpenID Configuration", description = "Returns the OpenID Provider configuration information, including the issuer and JWKS URI.")
    @GetMapping("/.well-known/openid-configuration")
    public ResponseEntity<Map<String, Object>> openidConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("issuer", properties.getIssuer());
        String jwksUri = properties.getIssuer();
        if (!jwksUri.endsWith("/")) jwksUri = jwksUri + "/";
        jwksUri = jwksUri + ".well-known/jwks.json";
        config.put("jwks_uri", jwksUri);
        return ResponseEntity.ok(config);
    }
}
