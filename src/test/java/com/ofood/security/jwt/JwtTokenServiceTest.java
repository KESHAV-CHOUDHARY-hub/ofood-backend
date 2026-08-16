package com.ofood.security.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private RSAKey generateRsaJwk() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var kp = kpg.generateKeyPair();
        RSAKey rsaJwk = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey(kp.getPrivate())
                .keyID("test-kid")
                .build();
        return rsaJwk;
    }

    private RsaKeyProperties createProperties(String issuer, String audience, String keyId, Duration ttl) {
        RsaKeyProperties props = new RsaKeyProperties();
        props.setIssuer(issuer);
        props.setAudience(audience);
        props.setKeyId(keyId);
        props.setPrivateKeyPath("file:" + System.getProperty("user.dir") + "/src/test/resources/keys/test_private.pem");
        props.setPublicKeyPath("");
        props.setAccessTokenTtl(ttl);
        return props;
    }

    @Test
    void generateAndValidateToken() throws Exception {
        RSAKey rsaJwk = generateRsaJwk();

        RsaKeyProperties props = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));

        JwtTokenService svc = new JwtTokenService(rsaJwk, props);

        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        String token = svc.generateAccessToken(userId, sid, List.of("ROLE_CUSTOMER"));
        assertNotNull(token);

        RSAPublicKey pub = rsaJwk.toRSAPublicKey();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(pub).build();
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer("http://localhost:8080"));
        var jwt = decoder.decode(token);

        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals("http://localhost:8080", jwt.getIssuer().toString());
        assertTrue(jwt.getAudience().contains("ofood-api"));
        assertEquals(sid.toString(), jwt.getClaimAsString("sid"));
        assertEquals(List.of("ROLE_CUSTOMER"), jwt.getClaimAsStringList("roles"));
    }

    @Test
    void incorrectIssuerRejected() throws Exception {
        RSAKey rsaJwk = generateRsaJwk();
        RsaKeyProperties tokenProps = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));
        RsaKeyProperties decoderProps = createProperties("http://issuer.example", "ofood-api", "test-kid", Duration.ofMinutes(10));
        JwtTokenService svc = new JwtTokenService(rsaJwk, tokenProps);
        JwtConfig jwtConfig = new JwtConfig(new RsaKeyProvider(decoderProps, new DefaultResourceLoader()), decoderProps);
        JwtDecoder decoder = jwtConfig.jwtDecoder();

        String token = svc.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), List.of("ROLE_CUSTOMER"));
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void incorrectAudienceRejected() throws Exception {
        RSAKey rsaJwk = generateRsaJwk();
        RsaKeyProperties tokenProps = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));
        RsaKeyProperties decoderProps = createProperties("http://localhost:8080", "wrong-audience", "test-kid", Duration.ofMinutes(10));
        JwtTokenService svc = new JwtTokenService(rsaJwk, tokenProps);
        JwtConfig jwtConfig = new JwtConfig(new RsaKeyProvider(decoderProps, new DefaultResourceLoader()), decoderProps);
        JwtDecoder decoder = jwtConfig.jwtDecoder();

        String token = svc.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), List.of("ROLE_CUSTOMER"));
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void expiredTokenRejected() throws Exception {
        RSAKey rsaJwk = generateRsaJwk();

        RsaKeyProperties props = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMillis(1));

        JwtTokenService svc = new JwtTokenService(rsaJwk, props);

        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        String token = svc.generateAccessToken(userId, sid, List.of("ROLE_CUSTOMER"));

        Thread.sleep(50);

        RSAPublicKey pub = rsaJwk.toRSAPublicKey();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(pub).build();
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer("http://localhost:8080"));
        assertThrows(org.springframework.security.oauth2.jwt.JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void invalidSignatureRejected() throws Exception {
        RSAKey rsaJwk = generateRsaJwk();

        RsaKeyProperties props = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));

        JwtTokenService svc = new JwtTokenService(rsaJwk, props);

        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        String token = svc.generateAccessToken(userId, sid, List.of("ROLE_CUSTOMER"));

        String tampered = token.replace('a', 'b');
        RSAPublicKey pub = rsaJwk.toRSAPublicKey();
        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(pub).build();
        assertThrows(org.springframework.security.oauth2.jwt.JwtException.class, () -> decoder.decode(tampered));
    }

    @Test
    void uniqueJtiGeneratedForEachToken() throws Exception {
        RSAKey rsaJwk = generateRsaJwk();
        RsaKeyProperties props = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));
        JwtTokenService svc = new JwtTokenService(rsaJwk, props);

        String first = svc.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), List.of("ROLE_CUSTOMER"));
        String second = svc.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), List.of("ROLE_CUSTOMER"));

        assertNotEquals(first, second);
    }

    @Test
    void rolesClaimConvertedToAuthorities() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("");

        JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(converter);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("roles", List.of("ROLE_ADMIN", "ROLE_CUSTOMER"))
                .build();

        var authentication = authConverter.convert(jwt);
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_CUSTOMER"));
    }

    @Test
    void jwksEndpointExposesPublicKeyAndMetadata() throws Exception {
        RsaKeyProperties props = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));
        RsaKeyProvider provider = new RsaKeyProvider(props, new DefaultResourceLoader());
        JwksController controller = new JwksController(provider);

        ResponseEntity<Map<String, Object>> response = controller.jwks();
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("keys"));
        List<Map<String, Object>> keys = (List<Map<String, Object>>) body.get("keys");
        assertEquals(1, keys.size());
        Map<String, Object> jwk = keys.get(0);
        assertEquals("RSA", jwk.get("kty"));
        assertEquals("RS256", jwk.get("alg"));
        assertEquals("sig", jwk.get("use"));
        assertEquals("test-kid", jwk.get("kid"));
        assertTrue(jwk.containsKey("n"));
        assertTrue(jwk.containsKey("e"));
        assertFalse(jwk.containsKey("d"));
    }

    @Test
    void openidDiscoveryReturnsConfiguredValues() {
        RsaKeyProperties props = createProperties("http://localhost:8080", "ofood-api", "test-kid", Duration.ofMinutes(10));
        OpenIdConfigurationController controller = new OpenIdConfigurationController(props);

        ResponseEntity<Map<String, Object>> response = controller.openidConfig();
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("http://localhost:8080", body.get("issuer"));
        assertEquals("http://localhost:8080/.well-known/jwks.json", body.get("jwks_uri"));
    }
}
