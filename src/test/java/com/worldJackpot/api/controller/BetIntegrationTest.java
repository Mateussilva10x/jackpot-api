package com.worldJackpot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldJackpot.api.dto.auth.AuthDto;
import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.dto.bet.BonusBetDto;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.BonusBetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import com.worldJackpot.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private BonusBetRepository bonusBetRepository;

    private String token;
    private Long matchId;
    private Long teamHomeId;
    private Long teamAwayId;

    @BeforeEach
    void setup() throws Exception {
        betRepository.deleteAll();
        bonusBetRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        // Register User
        AuthDto.RegisterRequest registerRequest = new AuthDto.RegisterRequest("Bettor", "bettor@example.com", "password", null);
        String authResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(authResponse).get("token").asText();

        // Create Teams
        Team teamHome = teamRepository.save(Team.builder().name("Team A").group("A").isoCode("TA").build());
        Team teamAway = teamRepository.save(Team.builder().name("Team B").group("A").isoCode("TB").build());
        teamHomeId = teamHome.getId();
        teamAwayId = teamAway.getId();

        // Create Match (Scheduled in future)
        Match match = Match.builder()
                .teamHome(teamHome)
                .teamAway(teamAway)
                .matchDate(LocalDateTime.now().plusDays(1))
                .phase(MatchPhase.GROUP)
                .status(MatchStatus.SCHEDULED)
                .groupName("A")
                .referenceCode("M1")
                .build();
        matchId = matchRepository.save(match).getId();
    }

    @Test
    void shouldReturnMatchesWithoutUserBetsWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].group").value("A"))
                .andExpect(jsonPath("$[0].matches[0].id").value(matchId))
                .andExpect(jsonPath("$[0].matches[0].userBet").isEmpty());
    }

    @Test
    void shouldPlaceBetSuccessfully() throws Exception {
        List<BetDto.BetRequest> bets = List.of(
                new BetDto.BetRequest(matchId, 2, 1)
        );

        mockMvc.perform(post("/bets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bets)))
                .andExpect(status().isOk());
        
        // Verify via GET /matches
        mockMvc.perform(get("/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches[0].userBet.homeScore").value(2))
                .andExpect(jsonPath("$[0].matches[0].userBet.awayScore").value(1));
    }

    @Test
    void shouldUpdateExistingBet() throws Exception {
        // Place initial bet
        List<BetDto.BetRequest> initialBets = List.of(new BetDto.BetRequest(matchId, 1, 0));
        mockMvc.perform(post("/bets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialBets)));

        // Update bet
        List<BetDto.BetRequest> updatedBets = List.of(new BetDto.BetRequest(matchId, 3, 3));
        mockMvc.perform(post("/bets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBets)))
                .andExpect(status().isOk());

        // Verify update
        mockMvc.perform(get("/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches[0].userBet.homeScore").value(3))
                .andExpect(jsonPath("$[0].matches[0].userBet.awayScore").value(3));
    }

    @Test
    void shouldFailToBetOnStartedMatch() throws Exception {
        // Create a past match
        Match pastMatch = matchRepository.save(Match.builder()
                .teamHome(teamRepository.findById(teamHomeId).get())
                .teamAway(teamRepository.findById(teamAwayId).get())
                .matchDate(LocalDateTime.now().minusHours(1))
                .phase(MatchPhase.GROUP)
                .status(MatchStatus.FINISHED)
                .groupName("A")
                .referenceCode("M2")
                .build());

        List<BetDto.BetRequest> bets = List.of(new BetDto.BetRequest(pastMatch.getId(), 2, 1));

        mockMvc.perform(post("/bets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bets)))
                .andExpect(status().isBadRequest()) // Check GlobalExceptionHandler maps IllegalArgumentException to 400
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("has already started")));
    }

    @Test
    void shouldPlaceBonusBet() throws Exception {
        BonusBetDto.BonusBetRequest request = BonusBetDto.BonusBetRequest.builder()
                .championTeamId(teamHomeId)
                .runnerUpTeamId(teamAwayId)
                .topScorer("Striker")
                .build();

        mockMvc.perform(post("/bonus-bets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/bonus-bets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.championTeamId").value(teamHomeId))
                .andExpect(jsonPath("$.topScorer").value("Striker"));
    }
}
