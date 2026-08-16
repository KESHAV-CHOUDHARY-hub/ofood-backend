package com.ofood.security.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Configuration
public class JwtConfig {

    private final RsaKeyProvider keyProvider;
    private final RsaKeyProperties properties;

    public JwtConfig(RsaKeyProvider keyProvider, RsaKeyProperties properties) {
        this.keyProvider = keyProvider;
        this.properties = properties;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            RSAKey rsaKey = keyProvider.getRsaKey();
            RSAPublicKey pub = rsaKey.toRSAPublicKey();
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(pub).build();
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
            OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(properties.getAudience()));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
            return decoder;
        } catch (com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException("Failed to build JwtDecoder from RSA key", ex);
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix(""); // roles already include ROLE_ prefix
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
