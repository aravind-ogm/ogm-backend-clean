package com.ogm.market.live;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiveTourService {

    private final AgentAvailabilityRepository agentRepo;
    private final LiveQueueRepository queueRepo;
    private final LiveSessionRepository sessionRepo;
    private final LiveLeadRepository leadRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledCallRepository scheduledCallRepo;

    // 🔔 Agent Availability
    public AvailabilityResponse getAvailability(Long propertyId) {

        AgentAvailability availability =
                agentRepo.findByPropertyId(propertyId)
                        .orElse(null);

        Long queueCount = queueRepo.countByPropertyId(propertyId);

        if (availability == null) {
            return AvailabilityResponse.builder()
                    .online(false)
                    .busy(false)
                    .queueCount(queueCount)
                    .build();
        }

        return AvailabilityResponse.builder()
                .online(availability.isOnline())
                .busy(availability.isBusy())
                .queueCount(queueCount)
                .build();
    }

    // ⏳ Join Queue
    public Integer joinQueue(JoinQueueRequest request) {

        Long count = queueRepo.countByPropertyId(request.getPropertyId());

        LiveQueue queue = LiveQueue.builder()
                .propertyId(request.getPropertyId())
                .name(request.getName())
                .mobile(request.getMobile())
                .position(count.intValue() + 1)
                .joinedAt(LocalDateTime.now())
                .build();

        queueRepo.save(queue);

        // 🔥 BROADCAST queue count update to customer-facing widget
        messagingTemplate.convertAndSend(
                "/topic/queue/" + request.getPropertyId(),
                queueRepo.countByPropertyId(request.getPropertyId())
        );

        // 📲 NOTIFY AGENT — incoming call alert on agent dashboard
        AgentAvailability agentAvail = agentRepo
                .findByPropertyId(request.getPropertyId())
                .orElse(null);

        if (agentAvail != null) {
            Map<String, Object> incomingCallPayload = new HashMap<>();
            incomingCallPayload.put("callerName",   request.getName());
            incomingCallPayload.put("callerMobile",  request.getMobile());
            incomingCallPayload.put("propertyId",    request.getPropertyId());
            incomingCallPayload.put("queuePosition", queue.getPosition());

            messagingTemplate.convertAndSend(
                    "/topic/agent/" + agentAvail.getAgentId() + "/incoming-call",
                    incomingCallPayload
            );

            // 📅 AUTO-CREATE upcoming call (15 min from now)
//            scheduledCallService.autoBookFromQueue(request, agentAvail.getAgentId());
        }

        // 🎯 CRM Capture
        leadRepo.save(
                LiveLead.builder()
                        .propertyId(request.getPropertyId())
                        .name(request.getName())
                        .mobile(request.getMobile())
                        .source("LIVE_TOUR")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 📅 AUTO-CREATE SCHEDULED CALL — 15 minutes from now
        Long agentId = agentAvail != null ? agentAvail.getAgentId() : null;
        scheduledCallRepo.save(
                ScheduledCall.builder()
                        .agentId(agentId)
                        .propertyId(request.getPropertyId())
                        .customerName(request.getName())
                        .customerMobile(request.getMobile())
                        .note("Auto-created when customer joined live tour queue")
                        .scheduledAt(LocalDateTime.now().plusMinutes(15))
                        .status("UPCOMING")
                        .source("AUTO_QUEUE")
                        .build()
        );

        return queue.getPosition();
    }

    // 📊 Start Session
    public LiveSession startSession(Long propertyId,
                                    Long agentId,
                                    String name,
                                    String mobile) {

        LiveSession session = LiveSession.builder()
                .propertyId(propertyId)
                .agentId(agentId)
                .customerName(name)
                .customerMobile(mobile)
                .startedAt(LocalDateTime.now())
                .build();

        return sessionRepo.save(session);
    }

    // 📊 End Session
    public void endSession(Long sessionId) {

        LiveSession session = sessionRepo.findById(sessionId).orElseThrow();

        session.setEndedAt(LocalDateTime.now());

        long duration =
                java.time.Duration.between(
                        session.getStartedAt(),
                        session.getEndedAt()
                ).getSeconds();

        session.setDurationSeconds(duration);

        sessionRepo.save(session);
    }
}