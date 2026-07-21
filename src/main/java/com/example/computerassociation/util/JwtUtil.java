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
    private String secret;

    @Value("${jwt.expiration:86400}")
    private Long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("created", new Date());

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

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

    public Boolean validateToken(String token, String username) {
        JwtValidationResult result = validateTokenWithResult(token, username);
        return result.isValid();
    }

    private Boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate != null && expirationDate.before(new Date());
    }

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

    public static class JwtValidationResult {
        private final boolean valid;
        private final boolean expired;
        private final String message;

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
