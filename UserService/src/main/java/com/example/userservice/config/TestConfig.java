package com.example.userservice.config;

import dev.kornliuk.jwtauthstarter.security.UserDetailsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.UUID;

@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public UserDetailsProvider testUserProvider() {
        return new UserDetailsProvider() {
            @Override
            public UUID getUserId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000000");
            }

            @Override
            public String getEmail() {
                return "test@gmail.com";
            }

            @Override
            public List<GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public boolean hasRole(String role) {
                return false;
            }

        };
    }

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .build();
    }
}