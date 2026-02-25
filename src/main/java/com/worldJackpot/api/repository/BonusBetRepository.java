package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.BonusBet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface BonusBetRepository extends JpaRepository<BonusBet, Long> {
    @EntityGraph(attributePaths = {"championTeam", "runnerUpTeam"})
    Optional<BonusBet> findByUserId(Long userId);
}
