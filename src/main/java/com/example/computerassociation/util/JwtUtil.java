// src/main/java/com/example/computerassociation/util/JwtUtil.java

/**
 * JWT 工具类
 * 
 * 负责生成、解析、验证 JWT Token。
 * 密钥和过期时间从 application.yml 中注入，Token 采用 HMAC-SHA 算法签名。
 */

package com.example.computerassociation.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;                          /// JWT 签名密钥，从配置文件注入

    @Value("${jwt.expiration:86400}")
    private Long expiration;                        /// Token 有效期（秒），默认 86400（24h）

    /** 根据密钥获取签名 Key 对象 */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** 生成 JWT Token */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("created", new Date());

        return Jwts.builder()
                .setClaims(claims)                                             // 设置载荷
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))  // 过期时间
                .signWith(getSigningKey())                                     // 签名
                .compact();
    }

    /** 从 Token 中提取用户名 */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /** 解析 Token，返回 Claims */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 详细验证 Token（区分过期与其他无效原因） */
    public JwtValidationResult validateTokenWithResult(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            if (tokenUsername == null) {
                return JwtValidationResult.invalid("无效的令牌");
            }
            if (!tokenUsername.equals(username)) {
                return JwtValidationResult.invalid("用户名不匹配");
            }
            if (isTokenExpired(token)) {
                return JwtValidationResult.expired();
            }
            return JwtValidationResult.valid();
        } catch (ExpiredJwtException e) {
            return JwtValidationResult.expired();
        }
    }

    /** 简单验证 Token（返回布尔值） */
    public Boolean validateToken(String token, String username) {
        JwtValidationResult result = validateTokenWithResult(token, username);
        return result.isValid();
    }

    /** 判断 Token 是否过期 */
    private Boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate != null && expirationDate.before(new Date());
    }

    /** 获取 Token 的过期时间 */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (JwtException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  内部类：JWT 验证结果                                              */
    /* ------------------------------------------------------------------ */
    public static class JwtValidationResult {
        private final boolean valid;           /// 是否有效
        private final boolean expired;         /// 是否过期
        private final String message;          /// 附加消息

        private JwtValidationResult(boolean valid, boolean expired, String message) {
            this.valid = valid;
            this.expired = expired;
            this.message = message;
        }

        public static JwtValidationResult valid() {
            return new JwtValidationResult(true, false, null);
        }

        public static JwtValidationResult expired() {
            return new JwtValidationResult(false, true, "令牌已过期");
        }

        public static JwtValidationResult invalid(String message) {
            return new JwtValidationResult(false, false, message);
        }

        public boolean isValid() { return valid; }
        public boolean isExpired() { return expired; }
        public String getMessage() { return message; }
    }
}