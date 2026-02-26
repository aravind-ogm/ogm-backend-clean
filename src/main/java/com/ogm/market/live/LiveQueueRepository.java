package com.ogm.market.live;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveQueueRepository extends JpaRepository<LiveQueue, Long> {

    List<LiveQueue> findByPropertyIdOrderByPositionAsc(Long propertyId);

    Long countByPropertyId(Long propertyId);
}