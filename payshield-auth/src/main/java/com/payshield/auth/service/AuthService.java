package com.payshield.auth.service;

import com.payshield.auth.config.JwtUtil;
import com.payshield.auth.dto.AuthDTOs.*;
import com.payshield.auth.entity.RefreshToken;
import com.payshield.auth.entity.User;
import com.payshield.auth.exception.AuthException;
import com.payshield.auth.repository.RefreshTokenRepository;
import com.payshield.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new AuthException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User not found"));

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("User {} logged in", user.getEmail());
        return new TokenResponse(accessToken, refreshToken,
                jwtUtil.getExpiration(), UserResponse.from(user));
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .build();

        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("New user registered: {}", user.getEmail());
        return new TokenResponse(accessToken, refreshToken,
                jwtUtil.getExpiration(), UserResponse.from(user));
    }

    @Transactional
    public TokenResponse refreshToken(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new AuthException("Refresh token expired. Please login again.");
        }

        User user = token.getUser();

        // Delete old token FIRST, then create new one — prevents duplicate key on re-use
        refreshTokenRepository.delete(token);
        refreshTokenRepository.flush();

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = createRefreshToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken,
                jwtUtil.getExpiration(), UserResponse.from(user));
    }

    // Scheduled cleanup: delete expired refresh tokens every 6 hours
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = 0;
        try {
            refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.debug("Cleaned up expired refresh tokens");
        } catch (Exception e) {
            log.error("Failed to clean up expired tokens: {}", e.getMessage());
        }
    }

    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        return refreshTokenRepository.save(token).getToken();
    }
}
