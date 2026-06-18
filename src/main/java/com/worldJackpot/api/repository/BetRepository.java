package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {
    Optional<Bet> findByUserIdAndMatchId(Long userId, Long matchId);
    List<Bet> findByUserId(Long userId);
    List<Bet> findByMatchId(Long matchId);
    List<Bet> findByUserIdAndMatchIdIn(Long userId, List<Long> matchIds);

    /**
     * Counts exact-score hits (bet score == final match score) per user, for FINISHED matches only.
     * Returns rows of [userId (Long), exactScores (Long)] for the given user ids.
     */
    @Query("SELECT b.user.id, COUNT(b) FROM Bet b " +
           "WHERE b.match.status = com.worldJackpot.api.model.enums.MatchStatus.FINISHED " +
           "AND b.homeScore = b.match.homeScore " +
           "AND b.awayScore = b.match.awayScore " +
           "AND b.user.id IN :userIds " +
           "GROUP BY b.user.id")
    List<Object[]> countExactScoresByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * Counts exact-score hits for a single user, FINISHED matches only.
     */
    @Query("SELECT COUNT(b) FROM Bet b " +
           "WHERE b.match.status = com.worldJackpot.api.model.enums.MatchStatus.FINISHED " +
           "AND b.homeScore = b.match.homeScore " +
           "AND b.awayScore = b.match.awayScore " +
           "AND b.user.id = :userId")
    long countExactScoresByUserId(@Param("userId") Long userId);
}
