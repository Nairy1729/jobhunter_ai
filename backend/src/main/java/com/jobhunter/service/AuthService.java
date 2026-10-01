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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       CandidateProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );
        User savedUser = userRepository.save(user);

        // Initialize empty candidate profile
        CandidateProfile profile = new CandidateProfile();
        profile.setUser(savedUser);
        profile.setHeadline("Software Engineer");
        profile.setYearsOfExperience(new BigDecimal("2.0"));
        profile.setMinSalaryInr(new BigDecimal("1000000.00"));
        profile.setTargetRoles("[\"Software Engineer\", \"Backend Engineer\", \"Full Stack Engineer\", \"Java Developer\"]");
        profile.setPreferredLocations("[\"Bangalore\", \"Hyderabad\", \"Pune\", \"Remote\"]");
        profile.setWorkModes("[\"REMOTE\", \"HYBRID\"]");
        profileRepository.save(profile);

        String token = tokenProvider.generateTokenForUser(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return new AuthResponse(
                token,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        );
    }
}
