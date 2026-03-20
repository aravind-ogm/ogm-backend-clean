package com.ogm.market.live;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@CrossOrigin
public class AgentController {

    private final AgentService agentService;

    // ─── AUTH ────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try { return ResponseEntity.ok(agentService.login(request)); }
        catch (RuntimeException e) { return ResponseEntity.status(401).build(); }
    }

    // ─── PROFILE ─────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<LoginResponse> getProfile(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getProfile(agentId));
    }

    @PutMapping("/profile")
    public ResponseEntity<LoginResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(agentService.updateProfile(request));
    }

    // ─── CALL HISTORY ────────────────────────────────────────────────────────

    @GetMapping("/calls")
    public ResponseEntity<List<CallHistoryDto>> getCallHistory(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getCallHistory(agentId));
    }

    // ─── STATS ───────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getStats(agentId));
    }

    // ─── AVAILABILITY ────────────────────────────────────────────────────────

    @PutMapping("/availability")
    public ResponseEntity<Void> toggleAvailability(@RequestBody AvailabilityToggleRequest request) {
        agentService.toggleAvailability(request);
        return ResponseEntity.ok().build();
    }

    // ─── SCHEDULE ────────────────────────────────────────────────────────────

    @GetMapping("/schedule")
    public ResponseEntity<List<AgentSchedule>> getSchedule(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getSchedule(agentId));
    }

    @PutMapping("/schedule")
    public ResponseEntity<Void> saveSchedule(@RequestParam Long agentId,
                                             @RequestBody List<ScheduleSlotDto> slots) {
        agentService.saveSchedule(agentId, slots);
        return ResponseEntity.ok().build();
    }

    // ─── UPCOMING CALLS ──────────────────────────────────────────────────────

    /**
     * GET /api/agent/upcoming?agentId=1
     * Returns all UPCOMING scheduled calls for this agent
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<ScheduledCallDto>> getUpcoming(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getUpcomingCalls(agentId));
    }

    /**
     * POST /api/agent/book-call
     * Book a call — used by customer from property page OR agent from dashboard
     * Body: { agentId?, propertyId, customerName, customerMobile, scheduledAt, note, source }
     */
    @PostMapping("/book-call")
    public ResponseEntity<ScheduledCallDto> bookCall(@RequestBody BookCallRequest request) {
        return ResponseEntity.ok(agentService.bookCall(request));
    }

    /**
     * PUT /api/agent/upcoming/{id}/cancel
     * Cancel a scheduled call
     */
    @PutMapping("/upcoming/{id}/cancel")
    public ResponseEntity<Void> cancelCall(@PathVariable Long id) {
        agentService.cancelCall(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/agent/upcoming/{id}/complete
     * Mark a scheduled call as completed
     */
    @PutMapping("/upcoming/{id}/complete")
    public ResponseEntity<Void> completeCall(@PathVariable Long id) {
        agentService.completeCall(id);
        return ResponseEntity.ok().build();
    }
}