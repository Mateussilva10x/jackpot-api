package com.worldJackpot.api.service;

import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test") // Ensures we use test configuration if needed, though default might suffice
class SeedingIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private WorldCupSeedService seedService;

    @Test
    void shouldSeedAllWorldCupData() {
        if (teamRepository.count() < 48) {
            seedService.seedWorldCupData();
        }
        
        // Then
        long teamCount = teamRepository.count();
        long matchCount = matchRepository.count();

        // 48 official teams + 64 placeholder teams = 112 teams
        assertThat(teamCount).as("Should have 48 teams + placeholders").isGreaterThanOrEqualTo(112);
        
        // 72 group matches + 32 knockout matches = 104 matches, plus any tests that leaked
        assertThat(matchCount).as("Should have at least 104 total matches").isGreaterThanOrEqualTo(104);

        // Verify specific knockout match
        boolean finalExists = matchRepository.existsByReferenceCode("WC2026-FIN-104");
        assertThat(finalExists).as("Final match should exist").isTrue();
    }
}
