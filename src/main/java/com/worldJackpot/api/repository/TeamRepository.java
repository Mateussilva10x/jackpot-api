package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByIsoCode(String isoCode);

    boolean existsByIsoCode(String isoCode);
}
