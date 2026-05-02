package org.campus.classroom.service;

import org.springframework.data.util.Pair;

public interface TokenService {
    String generateRefreshToken(Long userId);

    Long validateRefreshToken(String refreshToken);

    Pair<Long, String> rotateRefreshToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);

    void revokeAllRefreshTokens(Long userId);
}
