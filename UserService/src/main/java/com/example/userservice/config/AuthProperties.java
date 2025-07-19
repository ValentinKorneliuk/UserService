package com.example.userservice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Profile("!test")
@ConfigurationProperties(prefix = "auth")
@Data
@Validated
public class AuthProperties {

    @NotBlank(message = "property 'auth.public-key-url' must be set")
    private String publicKeyUrl;
}
