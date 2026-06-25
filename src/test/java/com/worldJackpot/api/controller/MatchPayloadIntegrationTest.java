package com.worldJackpot.api.controller;

import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.BetRepository;
import com.worldJackpot.api.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchPayloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchRepository matchRepository;

    @MockitoBean
    private BetRepository betRepository;

    @BeforeEach
    void setup() {
        Team brazil = Team.builder().id(5L).name("Brazil").flagUrl("BR").build();
        Team argentina = Team.builder().id(8L).name("Argentina").flagUrl("AR").build();

        Match match = Match.builder()
                .id(10L)
                .teamHome(brazil)
                .teamAway(argentina)
                .phase(MatchPhase.QUARTER)
                .status(MatchStatus.SCHEDULED)
                .matchDate(Instant.parse("2026-07-10T18:00:00Z"))
                .build();

        when(matchRepository.findAll()).thenReturn(List.of(match));
    }

    @Test
    @WithMockUser(roles = "USER")
    void matchesPayloadExposesRealTeamIds() throws Exception {
        mockMvc.perform(get("/matches").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches[0].id").value(10))
                .andExpect(jsonPath("$[0].matches[0].homeTeamId").value(5))
                .andExpect(jsonPath("$[0].matches[0].awayTeamId").value(8))
                .andExpect(jsonPath("$[0].matches[0].homeTeam").value("Brazil"))
                .andExpect(jsonPath("$[0].matches[0].awayTeam").value("Argentina"));
    }
}
