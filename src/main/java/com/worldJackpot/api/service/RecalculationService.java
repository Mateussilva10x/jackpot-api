package com.worldJackpot.api.service;

import com.worldJackpot.api.model.Bet;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecalculationService {

    private final MatchRepository matchRepository;
    private final BetRepository betRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void recalculatePoints(Long matchId) {
        log.info("Starting background points recalculation for match ID: {}", matchId);

        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != com.worldJackpot.api.model.enums.MatchStatus.FINISHED) {
            log.warn("Match {} not found or not FINISHED. Skipping recalculation.", matchId);
            return;
        }

        List<Bet> bets = betRepository.findByMatchId(matchId);
        if (bets.isEmpty()) {
            log.info("No bets found for match {}.", matchId);
            return;
        }

        // Group bets by userId to optimize user updates
        Map<Long, List<Bet>> betsByUser = bets.stream()
                .collect(Collectors.groupingBy(bet -> bet.getUser().getId()));

        for (Map.Entry<Long, List<Bet>> entry : betsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Bet> userBets = entry.getValue();

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            int totalPointsGained = 0;

            for (Bet bet : userBets) {
                if (bet.getPointsEarned() != null) {
                    log.debug("Bet {} already processed. Skipping.", bet.getId());
                    continue; // Skip if already calculated
                }

                int points = calculatePointsForBet(match, bet);
                bet.setPointsEarned(points);
                totalPointsGained += points;
            }

            if (totalPointsGained > 0) {
                user.setTotalPoints((user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + totalPointsGained);
                userRepository.save(user); // Save updated user
            }
        }
        
        // Save all updated bets in batch (if supported by JPA config)
        betRepository.saveAll(bets);

        log.info("Finished recalculation for match ID: {}. Processed {} bets.", matchId, bets.size());
    }

    private int calculatePointsForBet(Match match, Bet bet) {
        int matchHome = match.getHomeScore();
        int matchAway = match.getAwayScore();
        int betHome = bet.getHomeScore();
        int betAway = bet.getAwayScore();

        // Rule 1: Exact Score (10 pts)
        if (matchHome == betHome && matchAway == betAway) {
            return 10;
        }

        // Determine match result
        int matchResult = Integer.compare(matchHome, matchAway); // 1 (Home Win), -1 (Away Win), 0 (Draw)
        int betResult = Integer.compare(betHome, betAway);

        // Rule 2 & 3: Winner or Draw match
        if (matchResult == betResult) {
            // Rule 3 exception for draws: if it's a draw and not exact score (checked above), it's 5 pts. Goal diff is always 0.
            if (matchResult == 0) {
                return 5;
            }

            int matchDiff = matchHome - matchAway;
            int betDiff = betHome - betAway;

            // Rule 2: Goal Difference matches (7 pts)
            if (matchDiff == betDiff) {
                return 7;
            }
            // Rule 3: Only Winner (5 pts)
            return 5;
        }

        // Rule 4: Total miss (0 pts)
        return 0;
    }
}
