package com.ofood.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RsaKeyProviderTest {

    private static final String TEST_PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDVjlTMs3KeYUgN\n" +
            "9+Z3kkBPZ6u7v59jvLs2LJ6Ku91a8LtvUEtShErz430CmFwlYYCQkKWSTdXGw7LB\n" +
            "YvF/XwvI7QPm183vLtF+H/1fbEm7HPAmnyxbb4uhL2Eexld4+XFSmXUuaOgw0JCv\n" +
            "42zlQvAi54SUM3OrbwFFTs4xUzA1z7ugtkGtfsDDXewFX4fFQG1l1X9O2EposIWz\n" +
            "W/4PbWWmNkyyHq4vEQGakNhycb/TnvoBu8pC0KQ3lUH9iYl5fiGgJ+3J34+Osjbt\n" +
            "rle1v4pCzefuxONKME/zUJ04gouYHOgXY1zld87AHMKXYX4mhHdZAuw2OJ921tJs\n" +
            "UG+OwonTAgMBAAECggEAST1C/+EbEg4y8/ds5t0ViSX4067oLwBvBClRRisfwyyE\n" +
            "W6yh6G7Odc4y9UzjbT5/JRU2c3KWOlCrYX9Za2FhXUtL50NjsP/RgPvfnXY54x4N\n" +
            "jnFQeRtMO79QA4nqPebuYnGWqo/38aXVuTy5//Zw/MeBxIHQzZ6IRQi91lS9V/aB\n" +
            "4sr9FY5ftH+rOCwSYKmfKfAjJEIeVZNeC3kw5+Ww5kH1+irqLlj9BYKPHumrs34D\n" +
            "WAXBSsfIFEtmHUGuhZLWZO5IAtA7BMdf8Kc/zfabwn3qOY/7//AA5TemT9AeOCPg\n" +
            "4nM9b5KMiL0p3VlITC/wqaHb7NNpttmyh7wbJkbawQKBgQDxDIyhTFTBY2N7dKGZ\n" +
            "ZDC84VviNDI338EytmjyKSK4oEGzUJ+yfcgb60q62m7GAOVPTBt8XKtinrv9gmTO\n" +
            "Tcf+xqUAMCdEoberfwmRjvHDVm9tVQl54++yE4sRrC0sbCumXRT0Rz4siyxfxErs\n" +
            "3XNOO4YoFEla0zatEgST4Ad5aQKBgQDizT0sSYZVLTtdkZ2ZrN9l8PXvD77fd0ir\n" +
            "zBKRyYOSJMRNnRP1nmMOP/nuvdzbsecaZhEBCJ87iWjvdYq96a4SirsjWTRgX3Fm\n" +
            "2PGVHZLB8dzads7kuv5RXY5JWyWaH3sQLYZ+dGbINjICBvHfik5KxBIMLWxYnTVW\n" +
            "liG6ZZWl2wKBgGbEx+w44+WzXwWSexcvvQu6NPdi84oYzoC1flbILPJz+K7fj8aE\n" +
            "k907WpgBarrmRN2jABDsXXFlZZa2kg3W4Y6A2HYVEZjULb9jIQw62l5Cqqz9VVXv\n" +
            "VREh/eeh9Xx7/bwm8TkYaqyJBXkq8hq8a12OSSsrQv8DD8uH5AW7vNQJAoGAUScT\n" +
            "Dne4g13N8isac1RyEy3nMgU2TQuHi0FYG6Y5V9+kBgmAjNCBrSWjLpPtJQdBJCcF\n" +
            "SpNlNo5yZ8xtOosU6DmPwJQ4s4szLpPNzYdpbdA3MEx2t01Zlo+dTA47JCzDggRo\n" +
            "LXNFG7qYpjUJ8uywGvyRYJ4YOJT38uWBaArVQyUCgYBCy/Omk/AowgLOzeNoNzoN\n" +
            "LpL/U4LRsTHNuBz2eNZE9C7VUp/KgNxqW1jHc4oCQHAjDHZfvW3IdZPBkLG9D0S6\n" +
            "RhRrSXRUJgygg6RMVOwno/3BYA3S1yNXnIJsvyriL6ncQTGxHd86U1BObGbd9nnY\n" +
            "joeQtynG9GSWSRrXIy9GIA==\n" +
            "-----END PRIVATE KEY-----";

    private RsaKeyProperties properties;
    private ResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        properties = new RsaKeyProperties();
        properties.setKeyId("test-key-id");
        resourceLoader = mock(ResourceLoader.class);
    }

    @Test
    void loadsPrivateKeyFromPemDirectly() throws Exception {
        properties.setPrivateKeyPem(TEST_PRIVATE_KEY_PEM);

        RsaKeyProvider provider = new RsaKeyProvider(properties, resourceLoader);
        
        assertNotNull(provider.getRsaKey());
        verify(resourceLoader, never()).getResource(anyString());
    }

    @Test
    void loadsPrivateKeyFromPathWhenPemAbsent() throws Exception {
        properties.setPrivateKeyPath("classpath:dummy.pem");
        
        when(resourceLoader.getResource(anyString()))
                .thenReturn(new ByteArrayResource(TEST_PRIVATE_KEY_PEM.getBytes(StandardCharsets.UTF_8)));

        RsaKeyProvider provider = new RsaKeyProvider(properties, resourceLoader);

        assertNotNull(provider.getRsaKey());
        verify(resourceLoader, times(1)).getResource("classpath:dummy.pem");
    }

    @Test
    void pemTakesPrecedenceOverPath() throws Exception {
        properties.setPrivateKeyPem(TEST_PRIVATE_KEY_PEM);
        properties.setPrivateKeyPath("classpath:dummy.pem");

        RsaKeyProvider provider = new RsaKeyProvider(properties, resourceLoader);

        assertNotNull(provider.getRsaKey());
        verify(resourceLoader, never()).getResource(anyString());
    }

    @Test
    void throwsExceptionWhenNeitherIsConfigured() {
        properties.setPrivateKeyPem(null);
        properties.setPrivateKeyPath(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new RsaKeyProvider(properties, resourceLoader);
        });
        
        assertTrue(exception.getMessage().contains("must be configured"));
    }

    @Test
    void loadsPrivateKeyFromPemWithLiteralNewlines() throws Exception {
        String literalNewlines = TEST_PRIVATE_KEY_PEM.replace("\n", "\\n");
        properties.setPrivateKeyPem(literalNewlines);
        RsaKeyProvider provider = new RsaKeyProvider(properties, resourceLoader);
        assertNotNull(provider.getRsaKey());
    }

    @Test
    void loadsPrivateKeyFromPemWithCRLF() throws Exception {
        String crlf = TEST_PRIVATE_KEY_PEM.replace("\n", "\r\n");
        properties.setPrivateKeyPem(crlf);
        RsaKeyProvider provider = new RsaKeyProvider(properties, resourceLoader);
        assertNotNull(provider.getRsaKey());
    }

    @Test
    void loadsPrivateKeyFromPemWithExtraWhitespace() throws Exception {
        String whitespace = "   \n\n  " + TEST_PRIVATE_KEY_PEM + "  \n\t  ";
        properties.setPrivateKeyPem(whitespace);
        RsaKeyProvider provider = new RsaKeyProvider(properties, resourceLoader);
        assertNotNull(provider.getRsaKey());
    }

    @Test
    void failsOnBlankPem() {
        properties.setPrivateKeyPem("   \n   ");
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new RsaKeyProvider(properties, resourceLoader);
        });
        assertTrue(exception.getMessage().contains("must be configured"));
    }

    @Test
    void failsOnMissingBeginMarker() {
        String missingBegin = TEST_PRIVATE_KEY_PEM.replace("-----BEGIN PRIVATE KEY-----", "");
        properties.setPrivateKeyPem(missingBegin);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new RsaKeyProvider(properties, resourceLoader);
        });
        assertTrue(exception.getMessage().contains("check the key format"));
    }

    @Test
    void failsOnMissingEndMarker() {
        String missingEnd = TEST_PRIVATE_KEY_PEM.replace("-----END PRIVATE KEY-----", "");
        properties.setPrivateKeyPem(missingEnd);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new RsaKeyProvider(properties, resourceLoader);
        });
        assertTrue(exception.getMessage().contains("check the key format"));
    }

    @Test
    void failsOnInvalidBase64() {
        String invalidBase64 = "-----BEGIN PRIVATE KEY-----\n" +
                "!!!!INVALID_BASE64!!!!\n" +
                "-----END PRIVATE KEY-----";
        properties.setPrivateKeyPem(invalidBase64);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new RsaKeyProvider(properties, resourceLoader);
        });
        assertTrue(exception.getMessage().contains("check the key format"));
    }

    @Test
    void failsOnInvalidPkcs8Bytes() {
        String invalidPkcs8 = "-----BEGIN PRIVATE KEY-----\n" +
                "dGVzdA==\n" +
                "-----END PRIVATE KEY-----";
        properties.setPrivateKeyPem(invalidPkcs8);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new RsaKeyProvider(properties, resourceLoader);
        });
        assertTrue(exception.getMessage().contains("check the key format"));
    }
}
