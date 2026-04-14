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

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(agentService.login(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<LoginResponse> getProfile(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getProfile(agentId));
    }

    @PutMapping("/profile")
    public ResponseEntity<LoginResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(agentService.updateProfile(request));
    }


    @GetMapping("/calls")
    public ResponseEntity<List<CallHistoryDto>> getCallHistory(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getCallHistory(agentId));
    }


    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getStats(agentId));
    }

    @PutMapping("/availability")
    public ResponseEntity<Void> toggleAvailability(@RequestBody AvailabilityToggleRequest request) {
        agentService.toggleAvailability(request);
        return ResponseEntity.ok().build();
    }

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

    @GetMapping("/upcoming")
    public ResponseEntity<List<ScheduledCallDto>> getUpcoming(@RequestParam Long agentId) {
        return ResponseEntity.ok(agentService.getUpcomingCalls(agentId));
    }

    @PostMapping("/book-call")
    public ResponseEntity<ScheduledCallDto> bookCall(@RequestBody BookCallRequest request) {
        return ResponseEntity.ok(agentService.bookCall(request));
    }

    @PutMapping("/upcoming/{id}/cancel")
    public ResponseEntity<Void> cancelCall(@PathVariable Long id) {
        agentService.cancelCall(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/upcoming/{id}/complete")
    public ResponseEntity<Void> completeCall(@PathVariable Long id) {
        agentService.completeCall(id);
        return ResponseEntity.ok().build();
    }
}