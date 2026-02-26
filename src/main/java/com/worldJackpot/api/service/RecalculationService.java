package com.worldJackpot.api.service;

import com.worldJackpot.api.model.Bet;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // Group bets by userId
        Map<Long, List<Bet>> betsByUser = bets.stream()
                .collect(Collectors.groupingBy(bet -> bet.getUser().getId()));

        // Step 1: Update pointsEarned on every bet for this match
        for (List<Bet> userBets : betsByUser.values()) {
            for (Bet bet : userBets) {
                int newPoints = calculatePointsForBet(match, bet);
                bet.setPointsEarned(newPoints);
            }
        }

        // Save all updated bets first so the sum below sees fresh values
        betRepository.saveAll(bets);

        // Step 2: Recompute each user's totalPoints FROM SCRATCH by summing ALL their bets.
        // This makes the recalculation idempotent — no matter how many times the admin
        // changes the score, the ranking will always reflect the correct, current state.
        for (Long userId : betsByUser.keySet()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            int totalPoints = betRepository.findByUserId(userId).stream()
                    .filter(b -> b.getPointsEarned() != null)
                    .mapToInt(Bet::getPointsEarned)
                    .sum();

            user.setTotalPoints(totalPoints);
            userRepository.save(user);
        }

        log.info("Finished recalculation for match ID: {}. Processed {} bets.", matchId, bets.size());
    }

    private int calculatePointsForBet(Match match, Bet bet) {
        int matchHome = match.getHomeScore();
        int matchAway = match.getAwayScore();
        int betHome = bet.getHomeScore();
        int betAway = bet.getAwayScore();

        int basePoints = 0;

        int matchResult = Integer.compare(matchHome, matchAway);
        int betResult = Integer.compare(betHome, betAway);

        boolean hitHomeScore = matchHome == betHome;
        boolean hitAwayScore = matchAway == betAway;
        boolean hitWinnerOrDraw = matchResult == betResult;

        if (hitHomeScore && hitAwayScore) {
            basePoints = 10;
        } else if (hitWinnerOrDraw && (hitHomeScore || hitAwayScore)) {
            basePoints = 7;
        } else if (hitWinnerOrDraw) {
            basePoints = 5;
        } else if (hitHomeScore || hitAwayScore) {
            basePoints = 3;
        }

        // Extra logic for knockout stage draw winners
        if (match.getPhase() != com.worldJackpot.api.model.enums.MatchPhase.GROUP 
                && betResult == 0 
                && matchResult == 0) {
            if (bet.getSelectedWinner() != null && match.getPenaltyWinner() != null) {
                if (bet.getSelectedWinner().getId().equals(match.getPenaltyWinner().getId())) {
                    basePoints += 5; // 5 extra points for guessing the advancing team correctly
                }
            }
        }

        return basePoints;
    }
}
