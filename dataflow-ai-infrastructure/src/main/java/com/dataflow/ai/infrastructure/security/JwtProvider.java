package com.dataflow.ai.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token提供者（访问令牌 + 刷新令牌）
 */
@Component
public class JwtProvider {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret:defaultSecretKeyForDataflowAI2024PleaseChangeMe}")
    private String secret;

    @Value("${app.jwt.expiration:86400000}")
    private long expiration;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String userId, String username, String role) {
        Map<String, Object> claims = baseClaims(userId, username, role);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return buildToken(claims, username, expiration);
    }

    public String generateRefreshToken(String userId, String username, String role) {
        Map<String, Object> claims = baseClaims(userId, username, role);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        return buildToken(claims, username, refreshExpiration);
    }

    /** @deprecated 使用 {@link #generateAccessToken} */
    public String generateToken(String userId, String username, String role) {
        return generateAccessToken(userId, username, role);
    }

    public String refreshAccessToken(String refreshToken) {
        Claims claims = getClaimsFromToken(refreshToken);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }
        return generateAccessToken(
                claims.get("userId", String.class),
                claims.getSubject(),
                claims.get("role", String.class));
    }

    private Map<String, Object> baseClaims(String userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return claims;
    }

    private String buildToken(Map<String, Object> claims, String subject, long ttlMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", String.class);
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        return TOKEN_TYPE_ACCESS.equals(getClaimsFromToken(token).get(CLAIM_TOKEN_TYPE, String.class));
    }
}
