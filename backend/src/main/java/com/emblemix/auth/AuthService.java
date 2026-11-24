package com.emblemix.auth;

import com.emblemix.auth.dto.*;
import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.exception.UnauthorizedException;
import com.emblemix.common.security.JwtTokenProvider;
import com.emblemix.common.security.UserPrincipal;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.user.Role;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import com.emblemix.user.dto.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationMs;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${emblemix.security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase().trim(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String accessToken = tokenProvider.generateToken(principal);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in successfully: {}", user.getEmail());
        return AuthResponse.of(accessToken, refreshToken.getToken(), 900, UserProfileResponse.from(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with email " + email + " already exists");
        }

        // Generate organization slug
        String baseSlug = request.organizationName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        if (baseSlug.isEmpty()) {
            baseSlug = "org";
        }
        String slug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);

        Organization org = new Organization(UUID.randomUUID(), request.organizationName().trim(), slug);
        Organization savedOrg = organizationRepository.save(org);

        User user = new User(
                UUID.randomUUID(),
                savedOrg,
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                Role.ORG_ADMIN
        );
        User savedUser = userRepository.save(user);

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String accessToken = tokenProvider.generateToken(principal);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        log.info("New organization and admin user registered: org={}, user={}", savedOrg.getName(), savedUser.getEmail());
        return AuthResponse.of(accessToken, refreshToken.getToken(), 900, UserProfileResponse.from(savedUser));
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken currentToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (currentToken.isRevoked() || currentToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        User user = currentToken.getUser();
        // Revoke the old token (rotation)
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // Issue new refresh token & access token
        RefreshToken newRefreshToken = createRefreshToken(user);
        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateToken(principal);

        return AuthResponse.of(newAccessToken, newRefreshToken.getToken(), 900, UserProfileResponse.from(user));
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for email: " + email));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(user, token, Instant.now().plus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset token generated for user: {}", email);
        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token has expired or has already been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke existing user sessions
        refreshTokenRepository.revokeAllUserTokens(user.getId());
        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    private RefreshToken createRefreshToken(User user) {
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
        RefreshToken refreshToken = new RefreshToken(user, token, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }
}
