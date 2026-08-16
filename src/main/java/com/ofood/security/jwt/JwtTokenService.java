package com.ofood.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final RsaKeyProperties properties;

    @Autowired
    public JwtTokenService(RsaKeyProvider keyProvider, RsaKeyProperties properties) {
        RSAKey rsaKey = keyProvider.getRsaKey();
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        this.properties = properties;
    }

    // Test-friendly constructor allowing direct RSAKey injection to avoid mocking resource loaders
    public JwtTokenService(RSAKey rsaKey, RsaKeyProperties properties) {
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        this.properties = properties;
    }

    public String generateAccessToken(UUID userId, UUID sid, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("roles", roles)
                .claim("sid", sid.toString())
                .audience(Collections.singletonList(properties.getAudience()));

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.getKeyId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
