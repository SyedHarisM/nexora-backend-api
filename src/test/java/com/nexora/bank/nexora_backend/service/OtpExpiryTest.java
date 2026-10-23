package com.nexora.bank.nexora_backend.service;

import com.nexora.bank.nexora_backend.entity.Role;
import com.nexora.bank.nexora_backend.entity.User;
import com.nexora.bank.nexora_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * One-time codes previously had no lifetime: a code issued at any point stayed
 * valid until something replaced it. These tests pin the new behaviour.
 */
@SpringBootTest
@ActiveProfiles("test")
class OtpExpiryTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        when(emailService.generateOtp()).thenReturn("123456");

        user = new User();
        user.setName("Code Holder");
        user.setEmail("codeholder@gmail.com");
        user.setPassword("hashed");
        user.setPhoneNumber("03005555555");
        user.setRole(Role.USER);
        user.setIsVerified(false);
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("a lapsed code is refused even when the digits match")
    void expiredCodeIsRefused() {
        user.setOtp("123456");
        user.setOtpExpiresAt(Instant.now().minusSeconds(60));
        userRepository.save(user);

        ResponseEntity<?> response =
                userService.verifyAndResetPassword(user.getEmail(), "123456", "newPassword1");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(userRepository.findByEmail(user.getEmail()).orElseThrow().getIsVerified())
                .isFalse();
    }

    @Test
    @DisplayName("a code with no recorded expiry is treated as lapsed")
    void codeWithoutExpiryIsRefused() {
        user.setOtp("123456");
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        ResponseEntity<?> response =
                userService.verifyAndResetPassword(user.getEmail(), "123456", "newPassword1");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("a live code is accepted and then cleared")
    void liveCodeIsAcceptedAndCleared() {
        user.setOtp("123456");
        user.setOtpExpiresAt(Instant.now().plusSeconds(300));
        userRepository.save(user);

        ResponseEntity<?> response =
                userService.verifyAndResetPassword(user.getEmail(), "123456", "newPassword1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(reloaded.getIsVerified()).isTrue();
        // Cleared, so the same code cannot be replayed.
        assertThat(reloaded.getOtp()).isNull();
        assertThat(reloaded.getOtpExpiresAt()).isNull();
    }

    @Test
    @DisplayName("requesting a reset records a lifetime alongside the code")
    void requestingAResetRecordsAnExpiry() {
        userService.processForgotPassword(user.getEmail());

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(reloaded.getOtp()).isNotNull();
        assertThat(reloaded.getOtpExpiresAt()).isNotNull();
        assertThat(reloaded.isOtpExpired()).isFalse();
    }
}
