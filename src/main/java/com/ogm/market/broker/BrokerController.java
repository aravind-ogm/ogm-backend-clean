package com.ogm.market.broker;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Broker REST controller — NO DATABASE VERSION
 *
 * PUBLIC:
 *   POST /api/broker/verify-phone
 *   POST /api/broker/verify-email
 *   POST /api/broker/register
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brokerService.register(req));
    }
}