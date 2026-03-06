package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.model.Bet;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import com.worldJackpot.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BetService {

    private final BetRepository betRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final com.worldJackpot.api.util.MatchDeadlineValidator deadlineValidator;

    @Transactional
    public void placeBets(Long userId, List<BetDto.BetRequest> betRequests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        for (BetDto.BetRequest request : betRequests) {
            Match match = matchRepository.findById(request.getMatchId())
                    .orElseThrow(() -> new RuntimeException("Match not found: " + request.getMatchId()));

            deadlineValidator.validate(match);

            Bet bet = betRepository.findByUserIdAndMatchId(userId, match.getId())
                    .orElse(Bet.builder()
                            .user(user)
                            .match(match)
                            .build());

            bet.setHomeScore(request.getHomeScore());
            bet.setAwayScore(request.getAwayScore());

            if (request.getSelectedWinnerId() != null) {
                Team selectedWinner = teamRepository.findById(request.getSelectedWinnerId())
                        .orElseThrow(() -> new RuntimeException("Selected winner Team not found: " + request.getSelectedWinnerId()));
                bet.setSelectedWinner(selectedWinner);
            } else {
                bet.setSelectedWinner(null);
            }

            betRepository.save(bet);
        }
    }
}
