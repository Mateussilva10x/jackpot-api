package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.BonusBet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BonusBetRepository extends JpaRepository<BonusBet, Long> {
    Optional<BonusBet> findByUserId(Long userId);
}
