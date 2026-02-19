package com.worldJackpot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldJackpot.api.dto.auth.AuthDto;
import com.worldJackpot.api.dto.match.MatchScoreUpdateDto;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import com.worldJackpot.api.repository.UserRepository;
import com.worldJackpot.api.service.RecalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMatchIntegrationTest {

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

    @MockitoSpyBean
    private RecalculationService recalculationService;

    private String userToken;
    private String adminToken;
    private Long matchId;

    @BeforeEach
    void setup() throws Exception {
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

        // Create Match
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
    void shouldReturn403ForNonAdmin() throws Exception {
        MatchScoreUpdateDto dto = new MatchScoreUpdateDto(2, 1);
        
        mockMvc.perform(put("/admin/matches/" + matchId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFinalizeMatchSuccessfully() throws Exception {
        MatchScoreUpdateDto dto = new MatchScoreUpdateDto(2, 1);

        mockMvc.perform(put("/admin/matches/" + matchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Match updatedMatch = matchRepository.findById(matchId).orElseThrow();
        assert(updatedMatch.getStatus() == MatchStatus.FINISHED);
        assert(updatedMatch.getHomeScore() == 2);
        assert(updatedMatch.getAwayScore() == 1);
        
        verify(recalculationService).recalculatePoints(matchId);
    }

    @Test
    void shouldReturn400ForInvalidScore() throws Exception {
        MatchScoreUpdateDto dto = new MatchScoreUpdateDto(-1, 1);

        mockMvc.perform(put("/admin/matches/" + matchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForNonExistentMatch() throws Exception {
        MatchScoreUpdateDto dto = new MatchScoreUpdateDto(2, 1);

        mockMvc.perform(put("/admin/matches/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
