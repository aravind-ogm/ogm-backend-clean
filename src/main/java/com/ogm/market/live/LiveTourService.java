package com.ogm.market.live;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LiveTourService {

    private final AgentAvailabilityRepository agentRepo;
    private final LiveQueueRepository queueRepo;
    private final LiveSessionRepository sessionRepo;
    private final LiveLeadRepository leadRepo;
    private final SimpMessagingTemplate messagingTemplate;

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

        // 🔥 BROADCAST UPDATE
        messagingTemplate.convertAndSend(
                "/topic/queue/" + request.getPropertyId(),
                queueRepo.countByPropertyId(request.getPropertyId())
        );

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