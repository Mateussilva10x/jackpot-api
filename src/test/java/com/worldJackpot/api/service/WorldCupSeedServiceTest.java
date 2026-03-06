package com.worldJackpot.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldCupSeedServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchRepository matchRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WorldCupSeedService worldCupSeedService;

    private static final String MOCK_JSON = "{\n" +
            "  \"teams\": [\n" +
            "    { \"name\": \"TeamA\", \"isoCode\": \"TEA\", \"group\": \"A\", \"flagUrl\": \"urlA\" },\n" +
            "    { \"name\": \"TeamB\", \"isoCode\": \"TEB\", \"group\": \"A\", \"flagUrl\": \"urlB\" }\n" +
            "  ],\n" +
            "  \"matches\": [\n" +
            "    {\n" +
            "       \"teamHomeIsoCode\": \"TEA\", \"teamAwayIsoCode\": \"TEB\",\n" +
            "       \"matchDate\": \"2026-06-11T12:00:00\",\n" +
            "       \"phase\": \"GROUP\", \"groupName\": \"A\", \"status\": \"SCHEDULED\",\n" +
            "       \"referenceCode\": \"GAME1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    void shouldSeedDataCorrectly_FreshRun() throws IOException {
        // Given
        when(teamRepository.existsByIsoCode(anyString())).thenReturn(false);
        when(matchRepository.existsByReferenceCode(anyString())).thenReturn(false);
        
        // Mock findAll to return the teams that *would* be created, so match can find them
        Team teamA = Team.builder().isoCode("TEA").name("TeamA").build();
        Team teamB = Team.builder().isoCode("TEB").name("TeamB").build();
        when(teamRepository.findAll()).thenReturn(List.of(teamA, teamB));

        // When
        worldCupSeedService.seedFromInputStream(new ByteArrayInputStream(MOCK_JSON.getBytes(StandardCharsets.UTF_8)));

        // Then
        // Verify Teams
        verify(teamRepository, times(2)).save(any(Team.class));
        verify(teamRepository).existsByIsoCode("TEA");
        verify(teamRepository).existsByIsoCode("TEB");
        
        // Verify Matches
        verify(matchRepository, times(1)).save(any(Match.class));
        verify(matchRepository).existsByReferenceCode("GAME1");
    }

    @Test
    void shouldSeedDataCorrectly_IdempotentRun() throws IOException {
        // Given
        when(teamRepository.existsByIsoCode(anyString())).thenReturn(true);
        // Even if teams exist, findAll() is called to prep map for matches
        Team teamA = Team.builder().isoCode("TEA").name("TeamA").build();
        Team teamB = Team.builder().isoCode("TEB").name("TeamB").build();
        when(teamRepository.findAll()).thenReturn(List.of(teamA, teamB));
        
        // Match exists
        when(matchRepository.existsByReferenceCode(anyString())).thenReturn(true);

        // When
        worldCupSeedService.seedFromInputStream(new ByteArrayInputStream(MOCK_JSON.getBytes(StandardCharsets.UTF_8)));

        // Then
        verify(teamRepository, never()).save(any(Team.class));
        verify(matchRepository, never()).save(any(Match.class));
    }
}
