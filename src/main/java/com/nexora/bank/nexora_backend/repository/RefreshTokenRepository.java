package com.nexora.bank.nexora_backend.repository;

import com.nexora.bank.nexora_backend.entity.RefreshToken;
import com.nexora.bank.nexora_backend.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByUser(User user);
}
