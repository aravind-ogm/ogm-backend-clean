package com.ogm.market.live;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentScheduleRepository extends JpaRepository<AgentSchedule, Long> {

    List<AgentSchedule> findByAgentIdOrderByDayOfWeek(Long agentId);

    Optional<AgentSchedule> findByAgentIdAndDayOfWeek(Long agentId, String dayOfWeek);
}