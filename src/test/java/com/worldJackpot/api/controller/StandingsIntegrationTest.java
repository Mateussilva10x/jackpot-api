package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.match.GroupStandingDto;
import com.worldJackpot.api.service.StandingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StandingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StandingsService standingsService;

    @Test
    void shouldReturnStandings() throws Exception {
        GroupStandingDto brazil = GroupStandingDto.builder()
                .teamName("Brazil")
                .points(9)
                .matchesPlayed(3)
                .wins(3)
                .draws(0)
                .losses(0)
                .goalsFor(6)
                .goalsAgainst(1)
                .goalDifference(5)
                .build();

        GroupStandingDto serbia = GroupStandingDto.builder()
                .teamName("Serbia")
                .points(4)
                .build();

        when(standingsService.calculateAllGroupStandings())
                .thenReturn(Map.of("G", List.of(brazil, serbia)));

        mockMvc.perform(get("/standings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.G[0].teamName").value("Brazil"))
                .andExpect(jsonPath("$.G[0].points").value(9))
                .andExpect(jsonPath("$.G[0].matchesPlayed").value(3))
                .andExpect(jsonPath("$.G[0].wins").value(3))
                .andExpect(jsonPath("$.G[0].goalDifference").value(5))
                .andExpect(jsonPath("$.G[1].teamName").value("Serbia"))
                .andExpect(jsonPath("$.G[1].points").value(4));
    }
}
