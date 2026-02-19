package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.dto.match.MatchScoreUpdateDto;
import com.worldJackpot.api.model.Bet;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.service.RecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final BetRepository betRepository;

    private final RecalculationService recalculationService; // Injected service

    @Transactional(readOnly = true)
    public List<BetDto.MatchGroupResponse> getMatchesGroupedByGroup(Long userId) {
        List<Match> matches = matchRepository.findAll();
        Map<Long, Bet> userBetsMap;

        if (userId != null) {
            userBetsMap = betRepository.findByUserId(userId).stream()
                    .collect(Collectors.toMap(bet -> bet.getMatch().getId(), Function.identity()));
        } else {
            userBetsMap = Map.of();
        }

        Map<String, List<BetDto.MatchBetResponse>> groupedMatches = matches.stream()
                .map(match -> mapToMatchBetResponse(match, userBetsMap.get(match.getId())))
                .collect(Collectors.groupingBy(BetDto.MatchBetResponse::getGroup));

        List<BetDto.MatchGroupResponse> response = new ArrayList<>();
        // Ensure groups A-L order if needed, but for now simple iteration
        groupedMatches.forEach((group, matchList) -> {
            matchList.sort((m1, m2) -> m1.getDateTime().compareTo(m2.getDateTime()));
            response.add(new BetDto.MatchGroupResponse(group, matchList));
        });
        
        // precise sorting of groups could be added here if 'group' keys are consistently sortable

        return response;
    }

    @Transactional
    public void finalizeMatch(Long matchId, MatchScoreUpdateDto dto) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new com.worldJackpot.api.exception.ResourceNotFoundException("Match not found: " + matchId));

        match.setHomeScore(dto.getHomeScore());
        match.setAwayScore(dto.getAwayScore());
        match.setStatus(MatchStatus.FINISHED);
        
        matchRepository.save(match);
        
        recalculationService.recalculatePoints(match.getId());
    }

    private BetDto.MatchBetResponse mapToMatchBetResponse(Match match, Bet bet) {
        BetDto.BetResponse betResponse = null;
        if (bet != null) {
            betResponse = BetDto.BetResponse.builder()
                    .id(bet.getId())
                    .matchId(match.getId())
                    .homeScore(bet.getHomeScore())
                    .awayScore(bet.getAwayScore())
                    .updatedAt(bet.getUpdatedAt())
                    .build();
        }

        return BetDto.MatchBetResponse.builder()
                .id(match.getId())
                .homeTeam(match.getTeamHome().getName())
                .awayTeam(match.getTeamAway().getName())
                .homeTeamFlag(match.getTeamHome().getFlagUrl())
                .awayTeamFlag(match.getTeamAway().getFlagUrl())
                .dateTime(match.getMatchDate())
                .status(match.getStatus().name())
                .group(match.getGroupName())
                .userBet(betResponse)
                .build();
    }
}
