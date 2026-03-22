package com.ogm.market.live;

import com.ogm.market.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import com.ogm.market.config.JwtUtil;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository              agentRepo;
    private final AgentAvailabilityRepository  availabilityRepo;
    private final LiveSessionRepository        sessionRepo;
    private final AgentScheduleRepository      scheduleRepo;
    private final ScheduledCallRepository      scheduledCallRepo;
    private final PropertyRepository           propertyRepo;
    private final PasswordEncoder              passwordEncoder;
    private final JwtUtil                      jwtUtil;

    // ─── 1. LOGIN ────────────────────────────────────────────────────────────
    public LoginResponse login(LoginRequest request) {
        Agent agent = agentRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), agent.getPassword()))
            throw new RuntimeException("Invalid email or password");
        return LoginResponse.builder()
                .agentId(agent.getId()).name(agent.getName()).email(agent.getEmail())
                .phone(agent.getPhone()).photoUrl(agent.getPhotoUrl())
                .designation(agent.getDesignation())
                .token(jwtUtil.generateToken(agent.getEmail())).build();
    }

    // ─── 2. GET PROFILE ──────────────────────────────────────────────────────
    public LoginResponse getProfile(Long agentId) {
        Agent agent = agentRepo.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        return LoginResponse.builder()
                .agentId(agent.getId()).name(agent.getName()).email(agent.getEmail())
                .phone(agent.getPhone()).photoUrl(agent.getPhotoUrl())
                .designation(agent.getDesignation()).build();
    }

    // ─── 3. UPDATE PROFILE ───────────────────────────────────────────────────
    public LoginResponse updateProfile(UpdateProfileRequest request) {
        Agent agent = agentRepo.findById(request.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        if (request.getName()  != null && !request.getName().isBlank())  agent.setName(request.getName());
        if (request.getPhone() != null && !request.getPhone().isBlank()) agent.setPhone(request.getPhone());
        agentRepo.save(agent);
        return getProfile(agent.getId());
    }

    // ─── 4. CALL HISTORY ─────────────────────────────────────────────────────
    public List<CallHistoryDto> getCallHistory(Long agentId) {
        return sessionRepo.findByAgentIdOrderByStartedAtDesc(agentId)
                .stream().map(this::toCallHistoryDto).collect(Collectors.toList());
    }

    private CallHistoryDto toCallHistoryDto(LiveSession s) {
        String status = s.getEndedAt() != null ? "COMPLETED" : "ACTIVE";
        String durationFormatted = "";
        if (s.getDurationSeconds() != null) {
            long min = s.getDurationSeconds() / 60, sec = s.getDurationSeconds() % 60;
            durationFormatted = min + " min " + sec + " sec";
        }
        String propertyTitle = propertyRepo.findById(s.getPropertyId())
                .map(p -> p.getTitle()).orElse("Property #" + s.getPropertyId());
        return CallHistoryDto.builder()
                .sessionId(s.getId()).customerName(s.getCustomerName())
                .customerMobile(s.getCustomerMobile()).propertyId(s.getPropertyId())
                .propertyTitle(propertyTitle).status(status)
                .durationSeconds(s.getDurationSeconds()).startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt()).durationFormatted(durationFormatted).build();
    }

    // ─── 5. AVAILABILITY ─────────────────────────────────────────────────────
    public void toggleAvailability(AvailabilityToggleRequest request) {
        AgentAvailability availability = availabilityRepo
                .findByAgentIdAndPropertyId(request.getAgentId(), request.getPropertyId())
                .orElse(AgentAvailability.builder()
                        .agentId(request.getAgentId()).propertyId(request.getPropertyId()).build());
        availability.setOnline(request.isOnline());
        availability.setBusy(request.isBusy());
        availabilityRepo.save(availability);
    }

    // ─── 6. SCHEDULE ─────────────────────────────────────────────────────────
    public List<AgentSchedule> getSchedule(Long agentId) {
        return scheduleRepo.findByAgentIdOrderByDayOfWeek(agentId);
    }

    public void saveSchedule(Long agentId, List<ScheduleSlotDto> slots) {
        for (ScheduleSlotDto dto : slots) {
            AgentSchedule slot = scheduleRepo.findByAgentIdAndDayOfWeek(agentId, dto.getDayOfWeek())
                    .orElse(AgentSchedule.builder().agentId(agentId).dayOfWeek(dto.getDayOfWeek()).build());
            slot.setStartTime(dto.getStartTime());
            slot.setEndTime(dto.getEndTime());
            slot.setActive(dto.isActive());
            scheduleRepo.save(slot);
        }
    }

    // ─── 7. STATS ────────────────────────────────────────────────────────────
    public Map<String, Long> getStats(Long agentId) {
        long total     = sessionRepo.countByAgentId(agentId);
        long completed = sessionRepo.countByAgentIdAndEndedAtIsNotNull(agentId);
        long active    = sessionRepo.findByAgentIdAndEndedAtIsNull(agentId).size();
        return Map.of("total", total, "completed", completed, "active", active);
    }

    // ─── 8. UPCOMING CALLS ───────────────────────────────────────────────────

    /** Get all UPCOMING scheduled calls for agent */
    public List<ScheduledCallDto> getUpcomingCalls(Long agentId) {
        return scheduledCallRepo
                .findByAgentIdAndStatusOrderByScheduledAtAsc(agentId, "UPCOMING")
                .stream().map(this::toScheduledCallDto).collect(Collectors.toList());
    }

    /** Book a call — used by customer (CUSTOMER_BOOKING) or agent (AGENT_SCHEDULED) */
    public ScheduledCallDto bookCall(BookCallRequest req) {
        LocalDateTime scheduledAt = LocalDateTime.parse(req.getScheduledAt());

        // Find agent — if not specified, use the one assigned to this property
        Long agentId = req.getAgentId();
        if (agentId == null && req.getPropertyId() != null) {
            agentId = availabilityRepo.findFirstByPropertyId(req.getPropertyId())
                    .map(a -> a.getAgentId()).orElse(null);
        }

        ScheduledCall call = ScheduledCall.builder()
                .agentId(agentId)
                .propertyId(req.getPropertyId())
                .customerName(req.getCustomerName())
                .customerMobile(req.getCustomerMobile())
                .note(req.getNote())
                .scheduledAt(scheduledAt)
                .status("UPCOMING")
                .source(req.getSource() != null ? req.getSource() : "CUSTOMER_BOOKING")
                .build();

        return toScheduledCallDto(scheduledCallRepo.save(call));
    }

    /** Auto-create upcoming call when customer joins queue */
    public ScheduledCallDto autoBookFromQueue(JoinQueueRequest req, LocalDateTime scheduledAt) {
        Long agentId = availabilityRepo.findFirstByPropertyId(req.getPropertyId())
                .map(a -> a.getAgentId()).orElse(null);

        ScheduledCall call = ScheduledCall.builder()
                .agentId(agentId)
                .propertyId(req.getPropertyId())
                .customerName(req.getName())
                .customerMobile(req.getMobile())
                .note("Auto-created from live tour queue")
                .scheduledAt(scheduledAt)
                .status("UPCOMING")
                .source("AUTO_QUEUE")
                .build();

        return toScheduledCallDto(scheduledCallRepo.save(call));
    }

    /** Cancel a scheduled call */
    public void cancelCall(Long callId) {
        ScheduledCall call = scheduledCallRepo.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call not found"));
        call.setStatus("CANCELLED");
        scheduledCallRepo.save(call);
    }

    /** Complete a scheduled call */
    public void completeCall(Long callId) {
        ScheduledCall call = scheduledCallRepo.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call not found"));
        call.setStatus("COMPLETED");
        scheduledCallRepo.save(call);
    }

    private ScheduledCallDto toScheduledCallDto(ScheduledCall s) {
        String propertyTitle = s.getPropertyId() != null
                ? propertyRepo.findById(s.getPropertyId())
                .map(p -> p.getTitle()).orElse("Property #" + s.getPropertyId())
                : "—";

        String formatted = s.getScheduledAt() != null
                ? s.getScheduledAt().format(DateTimeFormatter.ofPattern("MMM d • hh:mm a"))
                : "—";

        return ScheduledCallDto.builder()
                .id(s.getId()).agentId(s.getAgentId()).propertyId(s.getPropertyId())
                .propertyTitle(propertyTitle).customerName(s.getCustomerName())
                .customerMobile(s.getCustomerMobile()).note(s.getNote())
                .scheduledAt(s.getScheduledAt()).scheduledAtFormatted(formatted)
                .status(s.getStatus()).source(s.getSource()).build();
    }
}