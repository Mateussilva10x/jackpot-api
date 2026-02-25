package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.user.UserRankingDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.model.enums.UserRole;
import com.worldJackpot.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RankingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        User user1 = User.builder().id(1L).name("Player1").totalPoints(150).build();
        User user2 = User.builder().id(2L).name("Player2").totalPoints(100).build();
        User user3 = User.builder().id(3L).name("Player3").totalPoints(50).build();

        when(userRepository.findAllOrderByTotalPointsDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user1, user2, user3)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnRankingList() throws Exception {
        mockMvc.perform(get("/ranking")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Player1"))
                .andExpect(jsonPath("$.content[0].totalPoints").value(150))
                .andExpect(jsonPath("$.content[0].rankingPosition").value(1))
                .andExpect(jsonPath("$.content[1].name").value("Player2"))
                .andExpect(jsonPath("$.content[1].rankingPosition").value(2))
                .andExpect(jsonPath("$.content[2].name").value("Player3"))
                .andExpect(jsonPath("$.content[2].rankingPosition").value(3));
    }
}
