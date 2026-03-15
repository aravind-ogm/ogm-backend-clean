package com.ogm.market.controller;

import com.ogm.market.config.JwtUtil;
import com.ogm.market.dto.AuthResponse;
import com.ogm.market.dto.LoginRequest;
import com.ogm.market.dto.OtpRequest;
import com.ogm.market.dto.SignupRequest;
import com.ogm.market.model.User;
import com.ogm.market.repository.UserRepository;
import com.ogm.market.service.AuthService;
import com.ogm.market.service.OtpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /* ── Signup ──────────────────────────────────────────────── */

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    /* ── Login ───────────────────────────────────────────────── */

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /* ── OTP: send ───────────────────────────────────────────── */

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestParam @NotBlank(message = "Identifier is required") String identifier) {

        otpService.sendOtp(identifier);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtp());

        String identifier = request.getEmail();
        boolean isPhone = !identifier.contains("@");

        User user = userRepository.findByEmail(identifier).orElseGet(() -> {
            if (isPhone) {
                // Auto-register phone-only users
                User newUser = new User();
                newUser.setFullName(identifier);
                newUser.setEmail(identifier);
                newUser.setPassword("");
                newUser.setVerified(true);
                return userRepository.save(newUser);
            }
            // Email OTP — auto-register if not found too
            User newUser = new User();
            newUser.setFullName("User");
            newUser.setEmail(identifier);
            newUser.setPassword("");
            newUser.setVerified(true);
            return userRepository.save(newUser);
        });

        return AuthResponse.success(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }
}