package org.campus.classroom.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JWTUtils {
    private final SecretKey KEY;
    private final long DEFAULT_EXPIRATION; // 默认过期时间1小时

    public JWTUtils(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration}") long expirationTime) {
        this.KEY = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.DEFAULT_EXPIRATION = expirationTime; // 1小时
    }


    public String generateToken(Long userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + DEFAULT_EXPIRATION);

        return Jwts.builder()
                .header().add("alg", "HS256").add("typ", "JWT").and()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(KEY)
                .compact();
    }

    /**
     * 生成 JWT
     *
     * @param userId
     * @param username
     * @param role
     * @param expirationTime 过期时间（毫秒）
     * @return token
     */
    public String generateToken(Long userId, String username, String role, long expirationTime) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expirationTime);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .header().add("alg", "HS256").add("typ", "JWT")
                .and()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(KEY)
                .compact();
    }


    /**
     * 解析 token，成功返回 subject，失败抛异常
     *
     * @param token JWT字符串
     * @return subject
     */
    public String parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * 校验 token 是否有效
     *
     * @param token JWT字符串
     * @return true=有效，false=无效
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}