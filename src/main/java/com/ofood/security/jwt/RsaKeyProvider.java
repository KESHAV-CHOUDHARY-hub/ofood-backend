package com.ofood.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaKeyProvider {

    private final RsaKeyProperties properties;
    private final ResourceLoader resourceLoader;
    private RSAKey rsaKey;

    public RsaKeyProvider(RsaKeyProperties properties, ResourceLoader resourceLoader) throws Exception {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        init();
    }

    private void init() throws Exception {
        String privatePem = properties.getPrivateKeyPem();
        String privatePath = properties.getPrivateKeyPath();
        String publicPath = properties.getPublicKeyPath();
        String keyId = properties.getKeyId();

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("JWT key ID is not configured (ofood.security.jwt.key-id)");
        }

        PrivateKey privateKey;
        if (privatePem != null && !privatePem.isBlank()) {
            privateKey = loadPrivateKeyFromPem(privatePem);
        } else if (privatePath != null && !privatePath.isBlank()) {
            privateKey = loadPrivateKeyFromPem(readResourceAsString(privatePath));
        } else {
            throw new IllegalStateException("Either ofood.security.jwt.private-key-pem or ofood.security.jwt.private-key-path must be configured");
        }
        PublicKey publicKey = (publicPath != null && !publicPath.isBlank())
                ? loadPublicKeyFromPem(readResourceAsString(publicPath))
                : derivePublicKeyFromPrivateKey(privateKey);

        rsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) publicKey)
                .privateKey(privateKey)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyId)
                .build();
    }

    private String readResourceAsString(String path) throws Exception {
        if (path.startsWith("/") && !path.startsWith("file:")) {
            path = "file:" + path;
        }
        Resource res = resourceLoader.getResource(path);
        try (InputStream in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private PrivateKey loadPrivateKeyFromPem(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("Private key PEM cannot be null or blank");
        }
        try {
            String normalized = pem.replace("\\n", "\n").replace("\\r", "\r").trim();
            if (!normalized.contains("-----BEGIN") || !normalized.contains("-----END") || !normalized.contains("PRIVATE KEY-----")) {
                throw new IllegalArgumentException("PEM does not contain required BEGIN/END PRIVATE KEY markers");
            }
            String base64Body = normalized
                    .replaceAll("-----BEGIN (.*)PRIVATE KEY-----", "")
                    .replaceAll("-----END (.*)PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64Body);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA private key from PEM. Please check the key format and encoding.", e);
        }
    }

    private PublicKey loadPublicKeyFromPem(String pem) throws Exception {
        String pub = pem.replaceAll("-----BEGIN (.*)PUBLIC KEY-----", "")
                .replaceAll("-----END (.*)PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "");
        byte[] keyBytes = Base64.getDecoder().decode(pub);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private PublicKey derivePublicKeyFromPrivateKey(PrivateKey privateKey) throws Exception {
        if (privateKey instanceof RSAPrivateCrtKey rsaPrivateKey) {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent()));
        }
        throw new IllegalStateException("Only RSA private keys with CRT parameters are supported for public key derivation");
    }

    public RSAKey getRsaKey() {
        return rsaKey;
    }
}
