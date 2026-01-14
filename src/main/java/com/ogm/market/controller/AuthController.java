package com.ogm.market.controller;

import com.ogm.market.dto.AuthResponse;
import com.ogm.market.dto.LoginRequest;
import com.ogm.market.dto.OtpRequest;
import com.ogm.market.dto.SignupRequest;
import com.ogm.market.service.AuthService;
import com.ogm.market.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp/send")
    public String sendOtp(@RequestParam String identifier) {
        otpService.sendOtp(identifier);
        return "OTP sent";
    }


    @PostMapping("/otp/verify")
    public String verifyOtp(@RequestBody OtpRequest request) {
        boolean valid = otpService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );
        return valid ? "OTP Verified" : "Invalid OTP";
    }

}
