package com.emblemix.auth;

import com.emblemix.auth.dto.*;
import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.UnauthorizedException;
import com.emblemix.common.security.JwtTokenProvider;
import com.emblemix.common.security.UserPrincipal;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.user.Role;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userRepository,
                organizationRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                tokenProvider,
                604800000L
        );
    }

    @Test
    @DisplayName("Login should return AuthResponse with tokens when credentials are valid")
    void testLoginSuccess() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization(orgId, "Acme", "acme");
        User user = new User(userId, org, "test@acme.com", "hash", "John", "Doe", Role.ORG_ADMIN);
        UserPrincipal principal = UserPrincipal.create(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(any())).thenReturn("jwt.access.token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        LoginRequest request = new LoginRequest("test@acme.com", "Password123!");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.access.token", response.accessToken());
        assertEquals("test@acme.com", response.user().email());
    }

    @Test
    @DisplayName("Login should throw exception when credentials are invalid")
    void testLoginFailure() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("test@acme.com", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Register should create organization, user, and return tokens")
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("admin@newco.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedHash");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenProvider.generateToken(any())).thenReturn("new.jwt.token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RegisterRequest request = new RegisterRequest(
                "NewCo Global", "Alice", "Smith", "admin@newco.com", "Password123!"
        );
        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("new.jwt.token", response.accessToken());
        assertEquals("admin@newco.com", response.user().email());
        assertEquals("Alice", response.user().firstName());
    }

    @Test
    @DisplayName("Register should reject duplicate email addresses")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmail("exists@acme.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "Acme", "Bob", "Smith", "exists@acme.com", "Password123!"
        );
        assertThrows(BadRequestException.class, () -> authService.register(request));
    }
}
