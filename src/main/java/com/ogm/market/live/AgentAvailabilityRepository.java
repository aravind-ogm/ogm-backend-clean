package com.ogm.market.live;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentAvailabilityRepository extends JpaRepository<AgentAvailability, Long> {

    Optional<AgentAvailability> findByPropertyId(Long propertyId);

    Optional<AgentAvailability> findByAgentIdAndPropertyId(Long agentId, Long propertyId);

    List<AgentAvailability> findByAgentId(Long agentId);
}