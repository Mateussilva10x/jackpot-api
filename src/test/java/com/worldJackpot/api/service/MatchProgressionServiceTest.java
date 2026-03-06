package com.worldJackpot.api.service;

import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchProgressionServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchProgressionService matchProgressionService;

    private Team teamHome;
    private Team teamAway;
    private Match nextMatch;

    @BeforeEach
    void setup() {
        teamHome = Team.builder().id(1L).name("South Korea").isoCode("KOR").build();
        teamAway = Team.builder().id(2L).name("Brazil").isoCode("BRA").build();

        // nextMatch has a placeholder team "W73" as teamHome
        Team placeholderW73 = Team.builder().id(100L).name("Winner Match 73").isoCode("W73").build();
        nextMatch = Match.builder()
                .id(200L)
                .phase(MatchPhase.ROUND_16)
                .status(MatchStatus.SCHEDULED)
                .teamHome(placeholderW73)
                .build();
    }

    // -------------------------------------------------------------------------
    // Score-based winner
    // -------------------------------------------------------------------------

    @Test
    void shouldAdvanceWinnerByScore() {
        Match round32Match = finishedMatch(MatchPhase.ROUND_32, "WC2026-R32-73", 2, 1, null);

        when(matchRepository.findByTeamHomeIsoCode("W73")).thenReturn(List.of(nextMatch));
        when(matchRepository.findByTeamAwayIsoCode("W73")).thenReturn(Collections.emptyList());

        matchProgressionService.processKnockoutProgression(round32Match);

        ArgumentCaptor<Match> saved = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getTeamHome()).isEqualTo(teamHome); // won 2-1
    }

    @Test
    void shouldAdvanceAwayTeamWinnerByScore() {
        Match round32Match = finishedMatch(MatchPhase.ROUND_32, "WC2026-R32-73", 0, 3, null);

        Team placeholderW73 = Team.builder().id(100L).name("Winner Match 73").isoCode("W73").build();
        Match matchWhereW73IsAway = Match.builder()
                .id(300L)
                .phase(MatchPhase.ROUND_16)
                .status(MatchStatus.SCHEDULED)
                .teamAway(placeholderW73)
                .build();

        when(matchRepository.findByTeamHomeIsoCode("W73")).thenReturn(Collections.emptyList());
        when(matchRepository.findByTeamAwayIsoCode("W73")).thenReturn(List.of(matchWhereW73IsAway));

        matchProgressionService.processKnockoutProgression(round32Match);

        ArgumentCaptor<Match> saved = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getTeamAway()).isEqualTo(teamAway); // won 3-0
    }

    // -------------------------------------------------------------------------
    // Penalty winner
    // -------------------------------------------------------------------------

    @Test
    void shouldAdvancePenaltyWinner() {
        // Draw 1-1, but South Korea wins on penalties
        Match round32Match = finishedMatch(MatchPhase.ROUND_32, "WC2026-R32-73", 1, 1, teamHome);

        when(matchRepository.findByTeamHomeIsoCode("W73")).thenReturn(List.of(nextMatch));
        when(matchRepository.findByTeamAwayIsoCode("W73")).thenReturn(Collections.emptyList());

        matchProgressionService.processKnockoutProgression(round32Match);

        ArgumentCaptor<Match> saved = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getTeamHome()).isEqualTo(teamHome); // penalty winner
    }

    // -------------------------------------------------------------------------
    // No-op cases
    // -------------------------------------------------------------------------

    @Test
    void shouldSkipWhenMatchNotFinished() {
        Match m = Match.builder()
                .id(1L)
                .phase(MatchPhase.ROUND_32)
                .status(MatchStatus.SCHEDULED)
                .referenceCode("WC2026-R32-73")
                .build();

        matchProgressionService.processKnockoutProgression(m);

        verify(matchRepository, never()).findByTeamHomeIsoCode(any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldSkipGroupMatches() {
        Match m = finishedMatch(MatchPhase.GROUP, "WC2026-G-A-1", 2, 0, null);

        matchProgressionService.processKnockoutProgression(m);

        verify(matchRepository, never()).findByTeamHomeIsoCode(any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldSkipWhenDrawAndNoPenaltyWinner() {
        Match m = finishedMatch(MatchPhase.ROUND_32, "WC2026-R32-73", 1, 1, null);

        matchProgressionService.processKnockoutProgression(m);

        verify(matchRepository, never()).findByTeamHomeIsoCode(any());
        verify(matchRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // SEMI-final: also places the loser in 3rd-place match
    // -------------------------------------------------------------------------

    @Test
    void shouldPlaceSemiLoserInThirdPlaceSlot() {
        Match semiMatch = finishedMatch(MatchPhase.SEMI, "WC2026-SF-101", 2, 0, null);
        // teamHome (KOR) wins; teamAway (BRA) is the loser -> L101

        Team placeholderW101 = Team.builder().id(200L).name("Winner Match 101").isoCode("W101").build();
        Team placeholderL101 = Team.builder().id(201L).name("Loser Match 101").isoCode("L101").build();

        Match finalMatch = Match.builder().id(400L).phase(MatchPhase.FINAL)
                .teamHome(placeholderW101).build();
        Match thirdPlaceMatch = Match.builder().id(401L).phase(MatchPhase.THIRD_PLACE)
                .teamHome(placeholderL101).build();

        when(matchRepository.findByTeamHomeIsoCode("W101")).thenReturn(List.of(finalMatch));
        when(matchRepository.findByTeamAwayIsoCode("W101")).thenReturn(Collections.emptyList());
        when(matchRepository.findByTeamHomeIsoCode("L101")).thenReturn(List.of(thirdPlaceMatch));
        when(matchRepository.findByTeamAwayIsoCode("L101")).thenReturn(Collections.emptyList());

        matchProgressionService.processKnockoutProgression(semiMatch);

        // Two saves: finalMatch (winner) and thirdPlaceMatch (loser)
        verify(matchRepository, times(2)).save(any(Match.class));
        assertThat(finalMatch.getTeamHome()).isEqualTo(teamHome);   // KOR goes to final
        assertThat(thirdPlaceMatch.getTeamHome()).isEqualTo(teamAway); // BRA goes to 3rd place
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Match finishedMatch(MatchPhase phase, String referenceCode, int homeScore, int awayScore, Team penaltyWinner) {
        return Match.builder()
                .id(50L)
                .phase(phase)
                .status(MatchStatus.FINISHED)
                .teamHome(teamHome)
                .teamAway(teamAway)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .penaltyWinner(penaltyWinner)
                .referenceCode(referenceCode)
                .build();
    }
}
