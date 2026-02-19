package com.worldJackpot.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldJackpot.api.dto.auth.AuthDto;
import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.BetRepository;
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
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

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

    private String userToken;
    private String adminToken;
    private Long matchId;

    @BeforeEach
    void setup() throws Exception {
        betRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        // Register User
        AuthDto.RegisterRequest userRequest = new AuthDto.RegisterRequest("User", "user@example.com", "password", "USER");
        String userAuthResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(userAuthResponse).get("token").asText();

        // Register Admin
        AuthDto.RegisterRequest adminRequest = new AuthDto.RegisterRequest("Admin", "admin@example.com", "password", "ADMIN");
        String adminAuthResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminAuthResponse).get("token").asText();

        // Create Match (Scheduled in future)
        Team teamA = teamRepository.save(Team.builder().name("A").group("A").isoCode("A").build());
        Team teamB = teamRepository.save(Team.builder().name("B").group("A").isoCode("B").build());
        
        Match match = Match.builder()
                .teamHome(teamA)
                .teamAway(teamB)
                .matchDate(LocalDateTime.now().plusDays(1))
                .phase(MatchPhase.GROUP)
                .status(MatchStatus.SCHEDULED)
                .groupName("A")
                .referenceCode("M1")
                .build();
        matchId = matchRepository.save(match).getId();
    }

    @Test
    void shouldReturn401WhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/bonus-bets")) // Protected endpoint
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/bonus-bets")
                        .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
    }

    @Test
    void shouldReturn403WhenUserAccessesAdminRoute() throws Exception {
        mockMvc.perform(post("/admin/matches")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
    }

    @Test
    void shouldAllowAdminAccessToAdminRoute() throws Exception {
        mockMvc.perform(post("/admin/matches")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
    }

    @Test
    void shouldRejectBetAfterMatchStart() throws Exception {
        // Create a past match
        Match pastMatch = matchRepository.save(Match.builder()
                .teamHome(teamRepository.findAll().get(0))
                .teamAway(teamRepository.findAll().get(1))
                .matchDate(LocalDateTime.now().minusHours(1))
                .phase(MatchPhase.GROUP)
                .status(MatchStatus.FINISHED)
                .groupName("A")
                .referenceCode("M2")
                .build());

        List<BetDto.BetRequest> bets = List.of(new BetDto.BetRequest(pastMatch.getId(), 2, 1));

        mockMvc.perform(post("/bets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bets)))
                .andExpect(status().isBadRequest()) // Check GlobalExceptionHandler maps IllegalArgumentException to 400
                .andExpect(jsonPath("$.message", containsString("has already started")));
    }
}
