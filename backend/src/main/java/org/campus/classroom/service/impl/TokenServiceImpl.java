package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private static final String REFRESH_TOKEN_PREFIX = "crs:login:refresh:token:";
    private static final String USER_REFRESH_SET_PREFIX = "crs:login:refresh:user:";
    private static final DefaultRedisScript<String> ROTATE_REFRESH_TOKEN_SCRIPT = createRotateRefreshTokenScript();

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static DefaultRedisScript<String> createRotateRefreshTokenScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText("""
                local userId = redis.call('GET', KEYS[1])
                if not userId then
                    return ''
                end
                local userSetKey = ARGV[4] .. userId
                redis.call('DEL', KEYS[1])
                redis.call('SREM', userSetKey, ARGV[1])
                redis.call('SET', KEYS[2], userId, 'PX', ARGV[3])
                redis.call('SADD', userSetKey, ARGV[2])
                redis.call('PEXPIRE', userSetKey, ARGV[3])
                return userId
                """);
        return script;
    }

    private String buildTokenKey(String refreshToken) {
        return REFRESH_TOKEN_PREFIX + hashRefreshToken(refreshToken);
    }

    private String buildTokenHash(String refreshToken) {
        return hashRefreshToken(refreshToken);
    }

    private String buildUserRefreshSetKey(Long userId) {
        return USER_REFRESH_SET_PREFIX + userId;
    }

    private String requireRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        return refreshToken;
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public String generateRefreshToken(Long userId) {
        String refreshToken = UUID.randomUUID().toString();
        String key = buildTokenKey(refreshToken);
        String userRefreshSetKey = buildUserRefreshSetKey(userId);

        stringRedisTemplate.opsForValue().set(
                key,
                userId.toString(),
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );
        stringRedisTemplate.opsForSet().add(userRefreshSetKey, buildTokenHash(refreshToken));
        stringRedisTemplate.expire(userRefreshSetKey, refreshExpiration, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    @Override
    public Long validateRefreshToken(String refreshToken) {
        String key = buildTokenKey(requireRefreshToken(refreshToken));
        String userId = stringRedisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        return Long.valueOf(userId);
    }

    @Override
    public Pair<Long, String> rotateRefreshToken(String refreshToken) {
        String validRefreshToken = requireRefreshToken(refreshToken);
        String newRefreshToken = UUID.randomUUID().toString();
        String userId = stringRedisTemplate.execute(
                ROTATE_REFRESH_TOKEN_SCRIPT,
                List.of(buildTokenKey(validRefreshToken), buildTokenKey(newRefreshToken)),
                buildTokenHash(validRefreshToken),
                buildTokenHash(newRefreshToken),
                String.valueOf(refreshExpiration),
                USER_REFRESH_SET_PREFIX
        );
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        return Pair.of(Long.valueOf(userId), newRefreshToken);
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String key = buildTokenKey(refreshToken);
        String userId = stringRedisTemplate.opsForValue().get(key);
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(key);
        stringRedisTemplate.opsForSet().remove(buildUserRefreshSetKey(Long.valueOf(userId)), buildTokenHash(refreshToken));
    }

    @Override
    public void revokeAllRefreshTokens(Long userId) {
        String userRefreshSetKey = buildUserRefreshSetKey(userId);
        Set<String> tokenHashes = stringRedisTemplate.opsForSet().members(userRefreshSetKey);
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(userRefreshSetKey);
        if (tokenHashes != null) {
            for (String tokenHash : tokenHashes) {
                keysToDelete.add(REFRESH_TOKEN_PREFIX + tokenHash);
            }
        }
        stringRedisTemplate.delete(keysToDelete);
    }
}
