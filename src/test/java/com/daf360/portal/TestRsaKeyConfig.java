package com.daf360.portal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

// Overrides RsaKeyConfig beans for Spring context tests — no PEM files needed
@TestConfiguration
public class TestRsaKeyConfig {

    private static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KEY_PAIR = gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test RSA key pair", e);
        }
    }

    @Bean
    @Primary
    public PrivateKey jwtPrivateKey() {
        return KEY_PAIR.getPrivate();
    }

    @Bean
    @Primary
    public PublicKey jwtPublicKey() {
        return KEY_PAIR.getPublic();
    }
}
