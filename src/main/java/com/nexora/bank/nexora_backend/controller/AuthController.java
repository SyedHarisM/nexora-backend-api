package com.nexora.bank.nexora_backend.controller;

import com.nexora.bank.nexora_backend.dto.AuthResponseDTO;
import com.nexora.bank.nexora_backend.dto.RefreshTokenRequestDTO;
import com.nexora.bank.nexora_backend.entity.RefreshToken;
import com.nexora.bank.nexora_backend.entity.Role;
import com.nexora.bank.nexora_backend.service.RefreshTokenService;
import com.nexora.bank.nexora_backend.security.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public AuthController(RefreshTokenService refreshTokenService,
                          JwtUtil jwtUtil) {
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    // 🔁 REFRESH ACCESS TOKEN
    @PostMapping("/refresh")
    public AuthResponseDTO refreshToken(@RequestBody RefreshTokenRequestDTO request) {

        RefreshToken refreshToken = refreshTokenService.verifyToken(request.getRefreshToken());

        String email = refreshToken.getUser().getEmail();
        Role role = refreshToken.getUser().getRole();

        String newAccessToken = jwtUtil.generateToken(email, role);

        return new AuthResponseDTO(
                newAccessToken,
                refreshToken.getToken(),
                refreshToken.getUser().getName()
        );
    }
}
