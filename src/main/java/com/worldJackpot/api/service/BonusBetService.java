package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.bet.BonusBetDto;
import com.worldJackpot.api.model.BonusBet;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.BonusBetRepository;
import com.worldJackpot.api.repository.TeamRepository;
import com.worldJackpot.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BonusBetService {

    private final BonusBetRepository bonusBetRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Transactional
    public void placeBonusBet(Long userId, BonusBetDto.BonusBetRequest request) {
        // Validation: Ensure betting enabled? (Configurable date check could be added here)
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team champion = null;
        if (request.getChampionTeamId() != null) {
            champion = teamRepository.findById(request.getChampionTeamId())
                    .orElseThrow(() -> new RuntimeException("Champion team not found"));
        }

        Team runnerUp = null;
        if (request.getRunnerUpTeamId() != null) {
            runnerUp = teamRepository.findById(request.getRunnerUpTeamId())
                    .orElseThrow(() -> new RuntimeException("Runner-up team not found"));
        }

        BonusBet bonusBet = bonusBetRepository.findByUserId(userId)
                .orElse(BonusBet.builder().user(user).build());

        bonusBet.setChampionTeam(champion);
        bonusBet.setRunnerUpTeam(runnerUp);
        bonusBet.setTopScorer(request.getTopScorer());

        bonusBetRepository.save(bonusBet);
    }

    @Transactional(readOnly = true)
    public BonusBetDto.BonusBetResponse getBonusBet(Long userId) {
        return bonusBetRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElse(null);
    }

    private BonusBetDto.BonusBetResponse mapToDto(BonusBet bet) {
        return BonusBetDto.BonusBetResponse.builder()
                .id(bet.getId())
                .championTeamId(bet.getChampionTeam() != null ? bet.getChampionTeam().getId() : null)
                .championTeamName(bet.getChampionTeam() != null ? bet.getChampionTeam().getName() : null)
                .runnerUpTeamId(bet.getRunnerUpTeam() != null ? bet.getRunnerUpTeam().getId() : null)
                .runnerUpTeamName(bet.getRunnerUpTeam() != null ? bet.getRunnerUpTeam().getName() : null)
                .topScorer(bet.getTopScorer())
                .build();
    }

    @Transactional
    public void resolveBonusBets(BonusBetDto.BonusBetResolutionRequest request) {
        List<BonusBet> allBets = bonusBetRepository.findAll();

        for (BonusBet bet : allBets) {
            User user = bet.getUser();
            int pointsEarned = 0;

            // Check Champion
            if (bet.getChampionTeam() != null && request.getChampionTeamId() != null) {
                if (bet.getChampionTeam().getId().equals(request.getChampionTeamId())) {
                    pointsEarned += 20;
                }
            }

            // Check Runner-up
            if (bet.getRunnerUpTeam() != null && request.getRunnerUpTeamId() != null) {
                if (bet.getRunnerUpTeam().getId().equals(request.getRunnerUpTeamId())) {
                    pointsEarned += 15;
                }
            }

            // Check Top Scorer
            if (bet.getTopScorer() != null && request.getTopScorer() != null) {
                if (bet.getTopScorer().trim().equalsIgnoreCase(request.getTopScorer().trim())) {
                    pointsEarned += 10;
                }
            }

            if (pointsEarned > 0) {
                user.setTotalPoints((user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + pointsEarned);
                userRepository.save(user);
            }
        }
    }
}
