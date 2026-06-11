package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.entity.RefreshToken;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.repository.RefreshTokenRepository;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${app.jwt.refreshExpirationMs}")
    private Long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    // 🔐 CREATE REFRESH TOKEN
    public RefreshToken createRefreshToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // delete old token (1 user = 1 refresh token)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    // 🔍 VALIDATE TOKEN
    public RefreshToken verifyToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    @Transactional
    // ❌ DELETE TOKEN (LOGOUT)
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
