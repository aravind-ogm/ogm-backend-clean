package com.ogm.market.live;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
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
    public AvailabilityResponse availability(@PathVariable Long propertyId) {
        return service.getAvailability(propertyId);
    }

    // ⏳ Join Queue
    @PostMapping("/join-queue")
    public Integer joinQueue(@RequestBody JoinQueueRequest request) {
        return service.joinQueue(request);
    }

    // ✅ FIX: Start Session — accepts JSON body (not @RequestParam)
    // Called by agent when they accept an incoming call
    @PostMapping("/start-session")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> body) {
        try {
            Long propertyId = body.get("propertyId") != null
                    ? Long.valueOf(body.get("propertyId").toString()) : null;
            Long agentId = body.get("agentId") != null
                    ? Long.valueOf(body.get("agentId").toString()) : null;
            String name   = body.getOrDefault("callerName", "Unknown").toString();
            String mobile = body.getOrDefault("callerMobile", "").toString();

            LiveSession session = service.startSession(propertyId, agentId, name, mobile);

            return ResponseEntity.ok(Map.of(
                    "sessionId", session.getId(),
                    "status",    "ONGOING"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to start session: " + e.getMessage())
            );
        }
    }

    // ✅ FIX: End Session — marks status COMPLETED, calculates duration in seconds
    @PostMapping("/end-session/{sessionId}")
    public ResponseEntity<?> endSession(@PathVariable Long sessionId) {
        try {
            service.endSession(sessionId);
            return ResponseEntity.ok(Map.of("status", "COMPLETED"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to end session: " + e.getMessage())
            );
        }
    }

    // 🎥 JaaS Token
    @PostMapping("/jaas-token")
    public ResponseEntity<?> getJaasToken(@RequestBody Map<String, String> body) {
        try {
            String  userName    = body.getOrDefault("userName", "Guest");
            String  roomName    = body.getOrDefault("roomName", "ogmlive");
            boolean isModerator = "true".equalsIgnoreCase(
                    body.getOrDefault("moderator", "false")
            );

            String token = jaasTokenService.generateToken(userName, roomName, isModerator);

            // Sanity check logs
            try {
                String[] parts   = token.split("\\.");
                String   payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                System.out.println("=== JaaS JWT PAYLOAD ===");
                System.out.println(payload);
                System.out.println("========================");
                if (!payload.contains("\"room\":\"*\""))
                    System.err.println("❌ WARNING: room claim is NOT '*'");
                else
                    System.out.println("✅ room='*' correct");
                if (isModerator && payload.contains("\"moderator\":true"))
                    System.out.println("✅ moderator=true correct");
            } catch (Exception ex) {
                System.err.println("JWT decode error: " + ex.getMessage());
            }

            return ResponseEntity.ok(Map.of("token", token, "moderator", isModerator));

        } catch (Exception e) {
            System.err.println("JaaS token generation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    Map.of("error", "Token generation failed: " + e.getMessage())
            );
        }
    }
}