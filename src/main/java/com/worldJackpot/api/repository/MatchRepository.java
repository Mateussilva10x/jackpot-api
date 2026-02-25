package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.enums.MatchPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findByPhase(MatchPhase phase);

    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findByGroupNameAndPhase(String groupName, MatchPhase phase);

    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findByTeamHomeIsoCode(String isoCode);

    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findByTeamAwayIsoCode(String isoCode);

    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findByPhaseOrderByMatchDateAsc(MatchPhase phase);

    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findByPhaseNotOrderByMatchDateAsc(MatchPhase phase);

    @Override
    @EntityGraph(attributePaths = {"teamHome", "teamAway"})
    List<Match> findAll();
}
