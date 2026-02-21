package com.worldJackpot.api.service;

import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.model.enums.NextMatchSlot;
import com.worldJackpot.api.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchProgressionServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchProgressionService matchProgressionService;

    private Team teamA;
    private Team teamB;
    private Match knockoutMatch;
    private Match nextKnockoutMatch;

    @BeforeEach
    void setup() {
        teamA = Team.builder().id(1L).name("Brazil").isoCode("BRA").build();
        teamB = Team.builder().id(2L).name("Argentina").isoCode("ARG").build();

        nextKnockoutMatch = Match.builder()
                .id(100L)
                .phase(MatchPhase.QUARTER)
                .status(MatchStatus.SCHEDULED)
                .build();

        knockoutMatch = Match.builder()
                .id(50L)
                .phase(MatchPhase.ROUND_16)
                .status(MatchStatus.FINISHED)
                .teamHome(teamA)
                .teamAway(teamB)
                .homeScore(2)
                .awayScore(1)
                .nextMatchId(100L)
                .nextMatchSlot(NextMatchSlot.HOME)
                .build();
    }

    @Test
    void shouldAdvanceWinnerToNextKnockoutMatch() {
        when(matchRepository.findById(100L)).thenReturn(Optional.of(nextKnockoutMatch));

        matchProgressionService.processKnockoutProgression(knockoutMatch);

        verify(matchRepository, times(1)).save(nextKnockoutMatch);
        assert(nextKnockoutMatch.getTeamHome().equals(teamA)); // Brazil won 2-1
    }

    @Test
    void shouldNotAdvanceIfMatchNotFinished() {
        knockoutMatch.setStatus(MatchStatus.SCHEDULED);
        matchProgressionService.processKnockoutProgression(knockoutMatch);
        verify(matchRepository, never()).save(any());
    }
}
