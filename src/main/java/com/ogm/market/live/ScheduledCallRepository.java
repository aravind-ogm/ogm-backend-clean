package com.ogm.market.live;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduledCallRepository extends JpaRepository<ScheduledCall, Long> {

    // Agent dashboard — upcoming calls ordered by time
    List<ScheduledCall> findByAgentIdAndStatusOrderByScheduledAtAsc(Long agentId, String status);

    // All calls for an agent
    List<ScheduledCall> findByAgentIdOrderByScheduledAtDesc(Long agentId);

    // Customer-facing — check bookings by mobile
    List<ScheduledCall> findByCustomerMobileOrderByScheduledAtDesc(String mobile);
}