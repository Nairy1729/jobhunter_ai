package com.jobhunter.service;

import com.jobhunter.model.dto.AuthRequest;
import com.jobhunter.model.dto.AuthResponse;
import com.jobhunter.model.dto.RegisterRequest;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.User;
import com.jobhunter.repository.CandidateProfileRepository;
import com.jobhunter.repository.UserRepository;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                profileRepository,
                passwordEncoder,
                authenticationManager,
                tokenProvider
        );
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "newcandidate@jobhunter.ai",
                "securePass123",
                "Jane",
                "Doe"
        );

        when(userRepository.existsByEmail("newcandidate@jobhunter.ai")).thenReturn(false);
        when(passwordEncoder.encode("securePass123")).thenReturn("encodedPassword");

        User savedUser = new User("newcandidate@jobhunter.ai", "encodedPassword", "Jane", "Doe");
        savedUser.setId(UUID.randomUUID());
        savedUser.setRole("ROLE_CANDIDATE");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(profileRepository.save(any(CandidateProfile.class))).thenReturn(new CandidateProfile());
        when(tokenProvider.generateTokenForUser(any(), any(), any())).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("newcandidate@jobhunter.ai", response.getEmail());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("ROLE_CANDIDATE", response.getRole());

        verify(userRepository, times(1)).save(any(User.class));
        verify(profileRepository, times(1)).save(any(CandidateProfile.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "existing@jobhunter.ai",
                "securePass123",
                "Jane",
                "Doe"
        );

        when(userRepository.existsByEmail("existing@jobhunter.ai")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });

        assertTrue(ex.getMessage().contains("Email is already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        AuthRequest request = new AuthRequest("candidate@jobhunter.ai", "password123");

        User user = new User("candidate@jobhunter.ai", "encodedPassword", "Alex", "Chen");
        user.setId(UUID.randomUUID());
        user.setRole("ROLE_CANDIDATE");
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("mock.login.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.login.token", response.getToken());
        assertEquals("candidate@jobhunter.ai", response.getEmail());
        assertEquals("Alex", response.getFirstName());
        assertEquals("Chen", response.getLastName());
        assertEquals("ROLE_CANDIDATE", response.getRole());
    }

    @Test
    void testLoginInvalidCredentialsThrowsException() {
        AuthRequest request = new AuthRequest("candidate@jobhunter.ai", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
    }
}
