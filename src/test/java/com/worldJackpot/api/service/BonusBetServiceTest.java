package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.bet.BonusBetDto;
import com.worldJackpot.api.model.BonusBet;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.BonusBetRepository;
import com.worldJackpot.api.repository.TeamRepository;
import com.worldJackpot.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BonusBetServiceTest {

    @Mock
    private BonusBetRepository bonusBetRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BonusBetService bonusBetService;

    private User user;
    private Team championTeam;
    private Team runnerUpTeam;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setTotalPoints(10); // Start with 10 points

        championTeam = new Team();
        championTeam.setId(100L);
        championTeam.setName("Brazil");

        runnerUpTeam = new Team();
        runnerUpTeam.setId(200L);
        runnerUpTeam.setName("France");
    }

    @Test
    void resolveBonusBets_FullMatch() {
        // Given
        BonusBet bet = new BonusBet();
        bet.setUser(user);
        bet.setChampionTeam(championTeam);
        bet.setRunnerUpTeam(runnerUpTeam);
        bet.setTopScorer("Vinicius Junior");

        when(bonusBetRepository.findAll()).thenReturn(List.of(bet));

        BonusBetDto.BonusBetResolutionRequest request = BonusBetDto.BonusBetResolutionRequest.builder()
                .championTeamId(100L)
                .runnerUpTeamId(200L)
                .topScorer("Vinicius Junior")
                .build();

        // When
        bonusBetService.resolveBonusBets(request);

        // Then
        // Expected Points: 10 (initial) + 20 (champion) + 15 (runner-up) + 10 (top scorer) = 55
        assertEquals(55, user.getTotalPoints());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void resolveBonusBets_PartialMatch() {
        // Given
        BonusBet bet = new BonusBet();
        bet.setUser(user);
        bet.setChampionTeam(championTeam); // Correct
        
        Team wrongRunnerUp = new Team();
        wrongRunnerUp.setId(300L);
        bet.setRunnerUpTeam(wrongRunnerUp); // Wrong

        bet.setTopScorer("Mbappe"); // Wrong

        when(bonusBetRepository.findAll()).thenReturn(List.of(bet));

        BonusBetDto.BonusBetResolutionRequest request = BonusBetDto.BonusBetResolutionRequest.builder()
                .championTeamId(100L)
                .runnerUpTeamId(200L)
                .topScorer("Vinicius Junior")
                .build();

        // When
        bonusBetService.resolveBonusBets(request);

        // Then
        // Expected Points: 10 (initial) + 20 (champion) = 30
        assertEquals(30, user.getTotalPoints());
        verify(userRepository, times(1)).save(user);
    }
}
