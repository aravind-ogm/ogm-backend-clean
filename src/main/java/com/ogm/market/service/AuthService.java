package com.ogm.market.service;

import com.ogm.market.config.JwtUtil;
import com.ogm.market.dto.AuthResponse;
import com.ogm.market.dto.LoginRequest;
import com.ogm.market.dto.SignupRequest;
import com.ogm.market.model.User;
import com.ogm.market.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse signup(SignupRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerified(true);

        userRepository.save(user);

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }
}
