package com.ogm.market.live;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * All endpoints consumed by the Agent Admin Dashboard frontend.
 *
 * Base URL: /api/agent
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@CrossOrigin
public class AgentController {

    private final AgentService agentService;

    // ─── AUTH ────────────────────────────────────────────────────────────────

    /**
     * POST /api/agent/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(agentService.login(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * TEMPORARY — delete after use
     * GET /api/agent/hash?password=Test@1234
     * Returns BCrypt hash of the given password
     */
//    @GetMapping("/hash")
//    public String hash(@RequestParam String password) {
//        return agentService.hashPassword(password);
//    }

    // ─── PROFILE ─────────────────────────────────────────────────────────────

    /**
     * GET /api/agent/profile?agentId=1
     * Returns agent name, email, phone, photo, designation
     */
    @GetMapping("/profile")
    public ResponseEntity<LoginResponse> getProfile(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getProfile(agentId));
    }

    // ─── CALL HISTORY ────────────────────────────────────────────────────────

    /**
     * GET /api/agent/calls?agentId=1
     * Returns list of all past sessions (call history)
     */
    @GetMapping("/calls")
    public ResponseEntity<List<CallHistoryDto>> getCallHistory(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getCallHistory(agentId));
    }

    // ─── STATS ───────────────────────────────────────────────────────────────

    /**
     * GET /api/agent/stats?agentId=1
     * Returns { total, completed, active }
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getStats(agentId));
    }

    // ─── AVAILABILITY ────────────────────────────────────────────────────────

    /**
     * PUT /api/agent/availability
     * Body: { "agentId": 1, "propertyId": 101, "online": true, "busy": false }
     * Toggles agent online/busy state for a specific property
     */
    @PutMapping("/availability")
    public ResponseEntity<Void> toggleAvailability(
            @RequestBody AvailabilityToggleRequest request) {
        agentService.toggleAvailability(request);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/agent/profile
     * Body: { "agentId": 1, "name": "New Name", "phone": "9999999999" }
     */
    @PutMapping("/profile")
    public ResponseEntity<LoginResponse> updateProfile(
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(agentService.updateProfile(request));
    }

    // ─── SCHEDULE ────────────────────────────────────────────────────────────

    /**
     * GET /api/agent/schedule?agentId=1
     * Returns the agent's weekly availability schedule
     */
    @GetMapping("/schedule")
    public ResponseEntity<List<AgentSchedule>> getSchedule(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getSchedule(agentId));
    }

    /**
     * PUT /api/agent/schedule?agentId=1
     * Body: [ { "dayOfWeek": "MON", "startTime": "09:00", "endTime": "18:00", "active": true }, ... ]
     * Saves/updates the full weekly schedule
     */
    @PutMapping("/schedule")
    public ResponseEntity<Void> saveSchedule(
            @RequestParam Long agentId,
            @RequestBody List<ScheduleSlotDto> slots) {
        agentService.saveSchedule(agentId, slots);
        return ResponseEntity.ok().build();
    }

//    @GetMapping("/upcoming")
//    public ResponseEntity<List<ScheduledCall>> getUpcoming(@RequestParam Long agentId) {
//        return ResponseEntity.ok(scheduledCallRepo
//                .findByAgentIdAndStatusOrderByScheduledAtAsc(agentId, "UPCOMING"));
//    }
}