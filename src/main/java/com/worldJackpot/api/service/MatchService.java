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
    private final com.worldJackpot.api.repository.TeamRepository teamRepository;

    private final RecalculationService recalculationService;
    private final MatchProgressionService matchProgressionService;

    @Transactional(readOnly = true)
    public List<BetDto.MatchGroupResponse> getMatchesGroupedByGroup(Long userId, String type) {
        List<Match> matches;
        
        if ("GROUP".equalsIgnoreCase(type)) {
            matches = matchRepository.findByPhaseOrderByMatchDateAsc(com.worldJackpot.api.model.enums.MatchPhase.GROUP);
        } else if ("KNOCKOUT".equalsIgnoreCase(type)) {
            matches = matchRepository.findByPhaseNotOrderByMatchDateAsc(com.worldJackpot.api.model.enums.MatchPhase.GROUP);
        } else {
            matches = matchRepository.findAll();
        }

        Map<Long, Bet> userBetsMap;

        if (userId != null && !matches.isEmpty()) {
            List<Long> matchIds = matches.stream().map(Match::getId).collect(Collectors.toList());
            userBetsMap = betRepository.findByUserIdAndMatchIdIn(userId, matchIds).stream()
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
            response.add(new BetDto.MatchGroupResponse(group, matchList, "Vale resultado após prorrogação, exceto pênaltis."));
        });
        
        // precise sorting of groups could be added here if 'group' keys are consistently sortable

        return response;
    }

    @Transactional
    public void finalizeMatch(Long matchId, MatchScoreUpdateDto dto) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new com.worldJackpot.api.exception.ResourceNotFoundException("Match not found: " + matchId));

        boolean isKnockout = match.getPhase() != com.worldJackpot.api.model.enums.MatchPhase.GROUP;
        boolean isDraw = dto.getHomeScore().equals(dto.getAwayScore());

        if (isKnockout && isDraw && dto.getPenaltyWinnerId() == null) {
            throw new IllegalArgumentException(
                "Knockout matches ending in a draw require penaltyWinnerId (the team that advanced on penalties).");
        }

        match.setHomeScore(dto.getHomeScore());
        match.setAwayScore(dto.getAwayScore());

        if (dto.getPenaltyWinnerId() != null) {
            com.worldJackpot.api.model.Team penaltyWinner = teamRepository.findById(dto.getPenaltyWinnerId())
                .orElseThrow(() -> new com.worldJackpot.api.exception.ResourceNotFoundException("Team not found"));

            Long homeId = match.getTeamHome() != null ? match.getTeamHome().getId() : null;
            Long awayId = match.getTeamAway() != null ? match.getTeamAway().getId() : null;
            if (!penaltyWinner.getId().equals(homeId) && !penaltyWinner.getId().equals(awayId)) {
                throw new IllegalArgumentException(
                    "penaltyWinnerId must be one of the two teams playing this match.");
            }

            match.setPenaltyWinner(penaltyWinner);
        }
        
        match.setStatus(MatchStatus.FINISHED);
        
        matchRepository.save(match);
        
        recalculationService.recalculatePoints(match.getId());
        
        if (match.getPhase() == com.worldJackpot.api.model.enums.MatchPhase.GROUP) {
            matchProgressionService.processGroupStageCompletion(match.getGroupName());
        } else {
            matchProgressionService.processKnockoutProgression(match);
        }
    }

    private BetDto.MatchBetResponse mapToMatchBetResponse(Match match, Bet bet) {
        BetDto.BetResponse betResponse = null;
        if (bet != null) {
            betResponse = BetDto.BetResponse.builder()
                    .id(bet.getId())
                    .matchId(match.getId())
                    .homeScore(bet.getHomeScore())
                    .awayScore(bet.getAwayScore())
                    .selectedWinnerId(bet.getSelectedWinner() != null ? bet.getSelectedWinner().getId() : null)
                    .updatedAt(bet.getUpdatedAt())
                    .build();
        }

        String groupValue = (match.getPhase() != com.worldJackpot.api.model.enums.MatchPhase.GROUP) 
                ? match.getPhase().name() 
                : match.getGroupName();

        return BetDto.MatchBetResponse.builder()
                .id(match.getId())
                .homeTeamId(match.getTeamHome().getId())
                .awayTeamId(match.getTeamAway().getId())
                .homeTeam(match.getTeamHome().getName())
                .awayTeam(match.getTeamAway().getName())
                .homeTeamFlag(match.getTeamHome().getFlagUrl())
                .awayTeamFlag(match.getTeamAway().getFlagUrl())
                .dateTime(match.getMatchDate())
                .status(match.getStatus().name())
                .group(groupValue)
                .userBet(betResponse)
                .officialHomeScore(match.getHomeScore())
                .officialAwayScore(match.getAwayScore())
                .build();
    }
}
