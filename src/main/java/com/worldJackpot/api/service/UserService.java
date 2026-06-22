package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.dto.user.UserProfileDto;
import com.worldJackpot.api.dto.user.UserEvolutionDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MatchService matchService;
    private final BonusBetService bonusBetService;
    private final BetRepository betRepository;

    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.worldJackpot.api.exception.ResourceNotFoundException("User not found: " + userId));

        List<BetDto.MatchGroupResponse> userBets = matchService.getMatchesGroupedByGroup(userId, null);

        com.worldJackpot.api.dto.bet.BonusBetDto.BonusBetResponse bonusBet = bonusBetService.getBonusBet(userId);

        // Calculate approximate ranking position for display, if desired.
        // For simplicity and reusing existing entity fields, we just use the user fields.

        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .totalPoints(user.getTotalPoints() == null ? 0 : user.getTotalPoints())
                .rankingPosition(user.getRankingPosition())
                .avatarId(user.getAvatarId())
                .exactScores((int) betRepository.countExactScoresByUserId(userId))
                .partialScores((int) betRepository.countPartialScoresByUserId(userId))
                .justGoals((int) betRepository.countJustGoalsByUserId(userId))
                .bets(userBets)
                .bonusBet(bonusBet)
                .build();
    }

    /**
     * Reconstructs a user's ranking position over time, one entry per day that had FINISHED matches,
     * from the start of the tournament up to the most recent finished match.
     * Points are accumulated chronologically; position = number of users with strictly more points + 1.
     */
    public List<UserEvolutionDto> getUserEvolution(Long userId) {
        // Validate user exists (throws 404 otherwise)
        userRepository.findById(userId)
                .orElseThrow(() -> new com.worldJackpot.api.exception.ResourceNotFoundException("User not found: " + userId));

        List<Object[]> rows = betRepository.findFinishedBetPointsWithDate();

        // Sum points per (date, user); dates ordered ascending via TreeMap.
        TreeMap<LocalDate, Map<Long, Integer>> pointsByDate = new TreeMap<>();
        for (Object[] row : rows) {
            Long uid = (Long) row[0];
            LocalDate date = ((Instant) row[1]).atZone(ZoneOffset.UTC).toLocalDate();
            int pts = ((Number) row[2]).intValue();
            pointsByDate.computeIfAbsent(date, k -> new HashMap<>())
                    .merge(uid, pts, Integer::sum);
        }

        // Walk dates chronologically, accumulating each user's running total.
        Map<Long, Integer> running = new HashMap<>();
        List<UserEvolutionDto> evolution = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<Long, Integer>> dayEntry : pointsByDate.entrySet()) {
            for (Map.Entry<Long, Integer> userPts : dayEntry.getValue().entrySet()) {
                running.merge(userPts.getKey(), userPts.getValue(), Integer::sum);
            }

            int myTotal = running.getOrDefault(userId, 0);
            int higher = 0;
            for (int total : running.values()) {
                if (total > myTotal) higher++;
            }

            evolution.add(UserEvolutionDto.builder()
                    .date(dayEntry.getKey())
                    .position(higher + 1)
                    .totalPoints(myTotal)
                    .build());
        }

        return evolution;
    }
}
