package com.ogm.market.broker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrokerRepository extends JpaRepository<Broker, UUID> {

    Optional<Broker> findByEmail(String email);

    Optional<Broker> findByMobile(String mobile);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);
}