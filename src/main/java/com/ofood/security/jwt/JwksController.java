package com.ofood.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Discovery", description = "Endpoints for OpenID Configuration and JWKS discovery")
public class JwksController {

    private final RsaKeyProvider keyProvider;

    public JwksController(RsaKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Operation(summary = "JSON Web Key Set", description = "Returns the public RS256 key used to verify access tokens. The private key is strictly kept internal.")
    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        RSAKey rsa = keyProvider.getRsaKey();
        JWKSet set = new JWKSet(rsa.toPublicJWK());
        return ResponseEntity.ok(set.toJSONObject());
    }
}
