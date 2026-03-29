package com.ogm.market.broker;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Broker REST controller — phone.email version.
 *
 * PUBLIC (no JWT):
 *   POST /api/broker/verify-phone   — fetch verified phone from phone.email
 *   POST /api/broker/verify-email   — fetch verified email from phone.email
 *   POST /api/broker/register       — register broker
 *   POST /api/broker/login          — login via phone.email, returns JWT
 *
 * AUTHENTICATED (JWT required):
 *   GET  /api/broker/profile
 *   PUT  /api/broker/profile
 */
@RestController
@RequestMapping("/api/broker")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

    @PostMapping("/verify-phone")
    public ResponseEntity<BrokerDto.VerifyPhoneResponse> verifyPhone(
            @Valid @RequestBody BrokerDto.VerifyPhoneRequest req) {
        return ResponseEntity.ok(brokerService.verifyPhone(req.getUserJsonUrl()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<BrokerDto.VerifyEmailResponse> verifyEmail(
            @Valid @RequestBody BrokerDto.VerifyEmailRequest req) {
        return ResponseEntity.ok(brokerService.verifyEmail(req.getUserJsonUrl()));
    }

    @PostMapping("/register")
    public ResponseEntity<BrokerDto.RegisterResponse> register(
            @Valid @RequestBody BrokerDto.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brokerService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<BrokerDto.LoginResponse> login(
            @Valid @RequestBody BrokerDto.LoginRequest req) {
        return ResponseEntity.ok(brokerService.login(req));
    }

    @GetMapping("/profile")
    public ResponseEntity<BrokerDto.BrokerProfileResponse> getProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(brokerService.getProfile(email));
    }

    @PutMapping("/profile")
    public ResponseEntity<BrokerDto.BrokerProfileResponse> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody BrokerDto.UpdateProfileRequest req) {
        return ResponseEntity.ok(brokerService.updateProfile(email, req));
    }
}