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



    @PostMapping("/jaas-token")
    public ResponseEntity<?> getJaasToken(@RequestBody Map<String, String> body) {
        try {
            String  userName    = body.getOrDefault("userName", "Guest");
            String  roomName    = body.getOrDefault("roomName", "ogmlive");
            // ✅ Parse boolean correctly from "true"/"false" string
            boolean isModerator = "true".equalsIgnoreCase(
                    body.getOrDefault("moderator", "false")
            );

            String token = jaasTokenService.generateToken(userName, roomName, isModerator);

            // ── Quick sanity check — decode payload and log it ──────────────
            try {
                String[] parts   = token.split("\\.");
                String   payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                System.out.println("=== JaaS JWT PAYLOAD ===");
                System.out.println(payload);
                System.out.println("========================");
                // Verify room is "*"
                if (!payload.contains("\"room\":\"*\"")) {
                    System.err.println("❌ WARNING: room claim is NOT '*' — audio will fail!");
                } else {
                    System.out.println("✅ room claim is '*' — correct");
                }
                // Verify moderator
                if (isModerator && !payload.contains("\"moderator\":true")) {
                    System.err.println("❌ WARNING: moderator claim is not true for agent!");
                } else if (isModerator) {
                    System.out.println("✅ moderator=true for agent — correct");
                }
            } catch (Exception ex) {
                System.err.println("Could not decode JWT for debug: " + ex.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "token",     token,
                    "moderator", isModerator
            ));

        } catch (Exception e) {
            System.err.println("JaaS token generation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    Map.of("error", "Token generation failed: " + e.getMessage())
            );
        }
    }
}