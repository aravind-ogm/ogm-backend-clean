package com.ogm.market.live;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/live-tour")
@RequiredArgsConstructor
@CrossOrigin
public class LiveTourController {

    private final LiveTourService service;

    @Autowired
    private JaasTokenService jaasTokenService;

    // 🔔 Availability
    @GetMapping("/availability/{propertyId}")
    public AvailabilityResponse availability(
            @PathVariable Long propertyId) {
        return service.getAvailability(propertyId);
    }

    // ⏳ Join Queue
    @PostMapping("/join-queue")
    public Integer joinQueue(
            @RequestBody JoinQueueRequest request) {
        return service.joinQueue(request);
    }

    // 📊 Start Session
    @PostMapping("/start-session")
    public void startSession(
            @RequestParam Long propertyId,
            @RequestParam Long agentId,
            @RequestParam String name,
            @RequestParam String mobile) {

        service.startSession(propertyId, agentId, name, mobile);
    }

    // 📊 End Session
    @PostMapping("/end-session/{sessionId}")
    public void endSession(@PathVariable Long sessionId) {
        service.endSession(sessionId);
    }

    @PostMapping("/token")
    public String generateToken(@RequestParam String userName) {

        // Call 100ms server API here
        // Return generated token

        return "generated-token-from-100ms";
    }



    // Customer calls this to get a token before joining
    @PostMapping("/jaas-token")
    public ResponseEntity<?> getJaasToken(@RequestBody Map<String, String> body) {
        try {
            String userName  = body.getOrDefault("userName", "Guest");
            String roomName  = body.get("roomName");
            boolean isMod    = Boolean.parseBoolean(body.getOrDefault("moderator", "false"));
            String token     = jaasTokenService.generateToken(userName, roomName, isMod);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Token generation failed"));
        }
    }
}