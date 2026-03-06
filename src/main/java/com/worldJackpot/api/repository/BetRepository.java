package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {
    Optional<Bet> findByUserIdAndMatchId(Long userId, Long matchId);
    List<Bet> findByUserId(Long userId);
    List<Bet> findByMatchId(Long matchId);
    List<Bet> findByUserIdAndMatchIdIn(Long userId, List<Long> matchIds);
}
