package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.enums.MatchPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);
    List<Match> findByPhase(MatchPhase phase);
    List<Match> findByGroupNameAndPhase(String groupName, MatchPhase phase);
    List<Match> findByTeamHomeIsoCode(String isoCode);
    List<Match> findByTeamAwayIsoCode(String isoCode);
    List<Match> findByPhaseOrderByMatchDateAsc(MatchPhase phase);
    List<Match> findByPhaseNotOrderByMatchDateAsc(MatchPhase phase);
}
