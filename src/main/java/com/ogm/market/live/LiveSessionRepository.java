package com.ogm.market.live;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {

    // All sessions for an agent, newest first
    List<LiveSession> findByAgentIdOrderByStartedAtDesc(Long agentId);

    // Completed sessions only (endedAt is not null)
    List<LiveSession> findByAgentIdAndEndedAtIsNotNullOrderByStartedAtDesc(Long agentId);

    // Active / in-progress sessions (endedAt is null)
    List<LiveSession> findByAgentIdAndEndedAtIsNull(Long agentId);

    // Count completed sessions for an agent
    long countByAgentIdAndEndedAtIsNotNull(Long agentId);

    // Count all sessions for an agent
    long countByAgentId(Long agentId);
}