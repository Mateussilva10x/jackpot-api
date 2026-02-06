package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);
}
