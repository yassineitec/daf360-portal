package com.daf360.portal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RsaKeyConfig {

    private final AppProperties props;

    @Bean
    @ConditionalOnMissingBean(PrivateKey.class)
    public PrivateKey jwtPrivateKey() throws Exception {
        String pem = Files.readString(Paths.get(props.getJwt().getPrivateKeyPath()));
        String encoded = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(encoded);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    @Bean
    @ConditionalOnMissingBean(PublicKey.class)
    public PublicKey jwtPublicKey() throws Exception {
        String pem = Files.readString(Paths.get(props.getJwt().getPublicKeyPath()));
        String encoded = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(encoded);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
