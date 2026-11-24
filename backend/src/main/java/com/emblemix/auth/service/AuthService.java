package com.emblemix.auth.service;

import com.emblemix.auth.dto.AuthResponse;
import com.emblemix.auth.dto.ForgotPasswordRequest;
import com.emblemix.auth.dto.LoginRequest;
import com.emblemix.auth.dto.RegisterRequest;
import com.emblemix.auth.dto.ResetPasswordRequest;
import com.emblemix.auth.dto.TokenRefreshRequest;

/**
 * Service interface for authentication, registration, session management, and password recovery.
 */
public interface AuthService {

    /**
     * Authenticates a user with email and password, issuing access and refresh tokens.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registers a new organization and provisions the initial ORG_ADMIN user.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Validates and rotates an existing refresh token, issuing new tokens.
     */
    AuthResponse refreshToken(TokenRefreshRequest request);

    /**
     * Revokes a refresh token to terminate the session.
     */
    void logout(String refreshTokenStr);

    /**
     * Initiates password reset by issuing a time-limited reset token.
     */
    String forgotPassword(ForgotPasswordRequest request);

    /**
     * Resets user password using a valid reset token and revokes active sessions.
     */
    void resetPassword(ResetPasswordRequest request);
}
