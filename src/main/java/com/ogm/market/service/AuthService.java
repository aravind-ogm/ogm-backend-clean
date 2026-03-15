package com.ogm.market.service;

import com.ogm.market.config.JwtUtil;
import com.ogm.market.dto.AuthResponse;
import com.ogm.market.dto.LoginRequest;
import com.ogm.market.dto.SignupRequest;
import com.ogm.market.exception.AuthException;
import com.ogm.market.exception.ConflictException;
import com.ogm.market.model.User;
import com.ogm.market.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user.
     * Throws ConflictException if the email is already taken.
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerified(true);

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return AuthResponse.success(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }

    /**
     * Authenticate with email + password.
     * Throws AuthException for any credential mismatch (intentionally vague to
     * prevent user-enumeration attacks).
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.success(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }
}