package com.worldJackpot.api.service;

import com.worldJackpot.api.model.Bet;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.model.enums.UserRole;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import com.worldJackpot.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class RecalculationIntegrationTest {

    @Autowired
    private RecalculationService recalculationService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    private User user1;
    private User user2;
    private Match match;

    @BeforeEach
    void setup() {
        betRepository.deleteAll();
        matchRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("pass")
                .role(UserRole.USER)
                .totalPoints(0)
                .build());

        user2 = userRepository.save(User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("pass")
                .role(UserRole.USER)
                .totalPoints(0)
                .build());

        Match oldMatch = matchRepository.save(Match.builder()
                .matchDate(LocalDateTime.now().minusDays(2).toInstant(java.time.ZoneOffset.UTC))
                .phase(MatchPhase.GROUP)
                .status(MatchStatus.FINISHED)
                .groupName("B")
                .referenceCode("M0")
                .homeScore(1)
                .awayScore(0)
                .build());

        betRepository.save(Bet.builder()
                .user(user2)
                .match(oldMatch)
                .homeScore(1)
                .awayScore(0)
                .pointsEarned(10) // Mocking that this past bet already awarded 10 points
                .build());

        match = matchRepository.save(Match.builder()
                .matchDate(LocalDateTime.now().minusDays(1).toInstant(java.time.ZoneOffset.UTC))
                .phase(MatchPhase.GROUP)
                .status(MatchStatus.FINISHED)
                .groupName("A")
                .referenceCode("M1")
                .homeScore(2) // Final score: 2 - 1
                .awayScore(1)
                .build());

        // Alice guesses 2-1 (Exact match -> 10 pts)
        betRepository.save(Bet.builder()
                .user(user1)
                .match(match)
                .homeScore(2)
                .awayScore(1)
                .build());

        // Bob guesses 3-1 (Winner + exact away score -> 7 pts: got the winner AND 1 away goal)
        betRepository.save(Bet.builder()
                .user(user2)
                .match(match)
                .homeScore(3)
                .awayScore(1)
                .build());
    }

    @Test
    void shouldRecalculatePointsAndSaveToDatabase() throws InterruptedException {
        // Trigger recalculation (it runs asynchronously due to @Async in the service)
        recalculationService.recalculatePoints(match.getId());

        // Wait a moment for the async thread to process (in a real app, Awaitility is better, but Thread.sleep works for simple tests)
        Thread.sleep(1000);

        List<Bet> allBets = betRepository.findAll();
        assertEquals(3, allBets.size());

        Bet aliceBet = allBets.stream().filter(b -> b.getUser().getId().equals(user1.getId())).findFirst().orElseThrow();
        Bet bobBet = allBets.stream()
                .filter(b -> b.getUser().getId().equals(user2.getId()) && b.getMatch().getId().equals(match.getId()))
                .findFirst().orElseThrow();

        assertEquals(10, aliceBet.getPointsEarned(), "Alice should have earned 10 points for exact match");
        assertEquals(7, bobBet.getPointsEarned(), "Bob should have earned 7 points for winner + exact away score");

        User updatedUser1 = userRepository.findById(user1.getId()).orElseThrow();
        User updatedUser2 = userRepository.findById(user2.getId()).orElseThrow();

        assertEquals(10, updatedUser1.getTotalPoints(), "Alice total points should be 10 (0 + 10)");
        assertEquals(17, updatedUser2.getTotalPoints(), "Bob total points should be 17 (10 + 7)");
    }
}
