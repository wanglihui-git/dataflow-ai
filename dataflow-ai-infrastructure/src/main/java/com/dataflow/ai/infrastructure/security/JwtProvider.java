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
 * JWT 令牌提供者：签发与解析访问令牌、刷新令牌。
 * <p>配置项：{@code app.jwt.secret}、{@code app.jwt.expiration}、{@code app.jwt.refresh-expiration}。
 */
@Component
public class JwtProvider {

    /** Claim 中标识令牌类型的字段名 */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    /** 访问令牌类型值 */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** 刷新令牌类型值 */
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

    /**
     * 签发访问令牌（短期，用于 API 鉴权）。
     *
     * @param userId   用户 ID
     * @param username 用户名（写入 subject）
     * @param role     角色
     * @return JWT 字符串
     */
    public String generateAccessToken(String userId, String username, String role) {
        Map<String, Object> claims = baseClaims(userId, username, role);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return buildToken(claims, username, expiration);
    }

    /**
     * 签发刷新令牌（长期，仅用于换取新访问令牌）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色
     * @return JWT 字符串
     */
    public String generateRefreshToken(String userId, String username, String role) {
        Map<String, Object> claims = baseClaims(userId, username, role);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        return buildToken(claims, username, refreshExpiration);
    }

    /**
     * 签发访问令牌（兼容旧调用，等价于 {@link #generateAccessToken}）。
     *
     * @deprecated 请使用 {@link #generateAccessToken}
     */
    public String generateToken(String userId, String username, String role) {
        return generateAccessToken(userId, username, role);
    }

    /**
     * 使用有效的刷新令牌换取新的访问令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     * @throws IllegalArgumentException 令牌类型不是 refresh 时
     */
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

    /**
     * 解析 JWT 并返回全部 Claims。
     *
     * @param token JWT 字符串
     * @return 声明集合
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中读取用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", String.class);
    }

    /**
     * 从令牌中读取用户名（subject）。
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从令牌中读取角色。
     *
     * @param token JWT 字符串
     * @return 角色
     */
    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    /**
     * 校验令牌签名有效且未过期（不区分 access/refresh）。
     *
     * @param token JWT 字符串
     * @return 有效返回 {@code true}
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验是否为有效的访问令牌（未过期且 tokenType=access）。
     *
     * @param token JWT 字符串
     * @return 有效返回 {@code true}
     */
    public boolean validateAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        return TOKEN_TYPE_ACCESS.equals(getClaimsFromToken(token).get(CLAIM_TOKEN_TYPE, String.class));
    }
}
