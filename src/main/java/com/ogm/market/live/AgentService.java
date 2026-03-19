package com.ogm.market.live;

import com.ogm.market.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepo;
    private final AgentAvailabilityRepository availabilityRepo;
    private final LiveSessionRepository sessionRepo;
    private final AgentScheduleRepository scheduleRepo;
    private final PasswordEncoder passwordEncoder;
    private final PropertyRepository propertyRepo;

    // ─── TEMP: Hash generator — delete after use ─────────────────────────────
    public String hashPassword(String raw) {
        return passwordEncoder.encode(raw);
    }

    // ─── 1. LOGIN ────────────────────────────────────────────────────────────

    public LoginResponse login(LoginRequest request) {

        Agent agent = agentRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), agent.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return LoginResponse.builder()
                .agentId(agent.getId())
                .name(agent.getName())
                .email(agent.getEmail())
                .phone(agent.getPhone())
                .photoUrl(agent.getPhotoUrl())
                .designation(agent.getDesignation())
                .token(UUID.randomUUID().toString())
                .build();
    }

    // ─── 2. GET PROFILE ──────────────────────────────────────────────────────

    public LoginResponse getProfile(Long agentId) {

        Agent agent = agentRepo.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        return LoginResponse.builder()
                .agentId(agent.getId())
                .name(agent.getName())
                .email(agent.getEmail())
                .phone(agent.getPhone())
                .photoUrl(agent.getPhotoUrl())
                .designation(agent.getDesignation())
                .build();
    }

    // ─── 3. UPDATE PROFILE ───────────────────────────────────────────────────

    public LoginResponse updateProfile(UpdateProfileRequest request) {

        Agent agent = agentRepo.findById(request.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (request.getName()  != null && !request.getName().isBlank())
            agent.setName(request.getName());
        if (request.getPhone() != null && !request.getPhone().isBlank())
            agent.setPhone(request.getPhone());

        agentRepo.save(agent);
        return getProfile(agent.getId());
    }

    // ─── 4. CALL HISTORY ─────────────────────────────────────────────────────

    public List<CallHistoryDto> getCallHistory(Long agentId) {

        return sessionRepo
                .findByAgentIdOrderByStartedAtDesc(agentId)
                .stream()
                .map(this::toCallHistoryDto)
                .collect(Collectors.toList());
    }

    private CallHistoryDto toCallHistoryDto(LiveSession s) {

        String status = s.getEndedAt() != null ? "COMPLETED" : "ACTIVE";

        String durationFormatted = "";
        if (s.getDurationSeconds() != null) {
            long min = s.getDurationSeconds() / 60;
            long sec = s.getDurationSeconds() % 60;
            durationFormatted = min + " min " + sec + " sec";
        }

        String propertyTitle = propertyRepo.findById(s.getPropertyId())
                .map(p -> p.getTitle())
                .orElse("Property #" + s.getPropertyId());

        return CallHistoryDto.builder()
                .sessionId(s.getId())
                .customerName(s.getCustomerName())
                .customerMobile(s.getCustomerMobile())
                .propertyId(s.getPropertyId())
                .propertyTitle(propertyTitle)
                .status(status)
                .durationSeconds(s.getDurationSeconds())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .durationFormatted(durationFormatted)
                .build();
    }

    // ─── 5. AVAILABILITY TOGGLE ──────────────────────────────────────────────

    public void toggleAvailability(AvailabilityToggleRequest request) {

        AgentAvailability availability = availabilityRepo
                .findByAgentIdAndPropertyId(request.getAgentId(), request.getPropertyId())
                .orElse(AgentAvailability.builder()
                        .agentId(request.getAgentId())
                        .propertyId(request.getPropertyId())
                        .build());

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
            AgentSchedule slot = scheduleRepo
                    .findByAgentIdAndDayOfWeek(agentId, dto.getDayOfWeek())
                    .orElse(AgentSchedule.builder()
                            .agentId(agentId)
                            .dayOfWeek(dto.getDayOfWeek())
                            .build());

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

        return Map.of(
                "total",     total,
                "completed", completed,
                "active",    active
        );
    }
}