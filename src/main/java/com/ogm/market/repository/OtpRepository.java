package com.ogm.market.repository;

import com.ogm.market.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByIdentifier(String identifier);

    void deleteByIdentifier(String identifier);
}
