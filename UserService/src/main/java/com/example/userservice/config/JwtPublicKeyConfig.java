package com.example.userservice.config;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Configuration
@Profile("!test")
public class JwtPublicKeyConfig {
    private static final String REDIS_KEY = "auth:public-key";
    private static final Duration TTL = Duration.ofHours(24);

    private String publicKeyUrl;
    private RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    public JwtPublicKeyConfig(RedisTemplate redisTemplate,
                              RestTemplate restTemplate,
                              @Value("${auth.public-key-url}") String publicKeyUrl){
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.publicKeyUrl = publicKeyUrl;
    }

    @Bean
    public PublicKey jwtPublicKey() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null) {
                return convertPemToPublicKey(cached);
            }

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    publicKeyUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Auth server responded with error status: " + response.getStatusCode());
            }

            Map<String, String> body = response.getBody();
            if (body == null || !body.containsKey("value")) {
                throw new IllegalStateException("Auth server response does not contain the required 'value' field or the body is empty");
            }

            String pem = body.get("value");

            redisTemplate.opsForValue().set(REDIS_KEY, pem, TTL);

            return convertPemToPublicKey(pem);

        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to connect to auth server or fetch the public key from URL: " + publicKeyUrl, e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse the public key from PEM string", e);
        }
    }


    private PublicKey convertPemToPublicKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(base64);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }
}