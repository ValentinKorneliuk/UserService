package com.example.userservice.filters;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.userservice.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Component
@Profile("!test")
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (hasBearerToken(authHeader)) {
            String token = extractToken(authHeader);

            try {
                Jwt jwt = jwtDecoder.decode(token);

                validateTokenType(jwt);

                Authentication auth = buildAuthenticationFromJwt(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (BadJwtException ex){
                sendUnauthorizedResponse(response, ex.getMessage(), request.getRequestURI());
                return;
            }
            catch (JwtException ex) {
                log.error("JWT processing error: {}", ex.getMessage());
                sendUnauthorizedResponse(response, "Invalid token", request.getRequestURI());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void validateTokenType(Jwt jwt) {
        String tokenType = (String) jwt.getClaims().get("token_type");
        if (!"access".equals(tokenType)) {
            throw new BadJwtException("Invalid token type: only access tokens are allowed for authorization");
        }
    }

    private Authentication buildAuthenticationFromJwt(Jwt jwt) {
        String username = jwt.getSubject();

        List<String> roles = (List<String>) jwt.getClaims().get("roles");

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    private boolean hasBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private String extractToken(String authHeader) {
        return authHeader.substring(7);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message, String path) throws IOException {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("Authorization error")
                .message(message)
                .path(path)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = objectMapper.writeValueAsString(error);
        response.getWriter().write(json);
    }

}
