package com.example.userservice.filters;

import java.io.IOException;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.userservice.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("!test")
public class JwtAuthFilter extends OncePerRequestFilter {

    private final PublicKey jwtPublicKey;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(PublicKey jwtPublicKey, ObjectMapper objectMapper) {
        this.jwtPublicKey = jwtPublicKey;
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
                Claims claims = parseAndValidateToken(token);
                validateTokenType(claims);

                Authentication auth = buildAuthenticationFromClaims(claims);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException ex) {
                sendUnauthorizedResponse(response, ex.getMessage(), request.getRequestURI());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private String extractToken(String authHeader) {
        return authHeader.substring(7);
    }

    private Claims parseAndValidateToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateTokenType(Claims claims) {
        String tokenType = claims.get("token_type", String.class);
        if (!"access".equals(tokenType)) {
            throw new JwtException("Invalid token type: only access tokens are allowed for authorization");
        }
    }

    private Authentication buildAuthenticationFromClaims(Claims claims) {
        String username = claims.getSubject();
        List<String> roles = claims.get("roles", List.class);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
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
