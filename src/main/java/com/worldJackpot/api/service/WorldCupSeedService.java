package com.worldJackpot.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldJackpot.api.dto.MatchDTO;
import com.worldJackpot.api.dto.TeamDTO;
import com.worldJackpot.api.dto.WorldCupDataDTO;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.MatchRepository;
import com.worldJackpot.api.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorldCupSeedService {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void seedWorldCupData() {
        log.info("Starting World Cup 2026 data seeding...");

        try {
            ClassPathResource resource = new ClassPathResource("data/worldcup2026-data.json");
            seedFromInputStream(resource.getInputStream());
            log.info("World Cup 2026 data seeding finished successfully!");
        } catch (IOException e) {
            log.error("Error reading World Cup data file", e);
            throw new RuntimeException("Failed to seed World Cup data", e);
        }
    }

    public void seedFromInputStream(java.io.InputStream inputStream) throws IOException {
        // Read JSON file
        WorldCupDataDTO worldCupData = objectMapper.readValue(inputStream, WorldCupDataDTO.class);

        // Seed teams first
        int teamsInserted = seedTeams(worldCupData.getTeams());
        log.info("Teams seeding completed: {} teams processed", teamsInserted);

        // Seed matches
        int matchesInserted = seedMatches(worldCupData.getMatches());
        log.info("Matches seeding completed: {} matches processed", matchesInserted);
    }

    private int seedTeams(java.util.List<TeamDTO> teamDTOs) {
        int insertedCount = 0;

        for (TeamDTO teamDTO : teamDTOs) {
            // Check if team already exists (idempotency)
            if (teamRepository.existsByIsoCode(teamDTO.getIsoCode())) {
                log.debug("Team {} already exists, skipping...", teamDTO.getName());
                continue;
            }

            Team team = Team.builder()
                    .name(teamDTO.getName())
                    .isoCode(teamDTO.getIsoCode())
                    .group(teamDTO.getGroup())
                    .flagUrl(teamDTO.getFlagUrl())
                    .build();

            teamRepository.save(team);
            insertedCount++;
            log.debug("Inserted team: {} ({})", team.getName(), team.getIsoCode());
        }

        return insertedCount;
    }

    private int seedMatches(java.util.List<MatchDTO> matchDTOs) {
        int insertedCount = 0;

        // Create a map of isoCode -> Team for quick lookup
        Map<String, Team> teamMap = new HashMap<>();
        teamRepository.findAll().forEach(team -> teamMap.put(team.getIsoCode(), team));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (MatchDTO matchDTO : matchDTOs) {
            // Check if match already exists (idempotency)
            if (matchRepository.existsByReferenceCode(matchDTO.getReferenceCode())) {
                log.debug("Match {} already exists, skipping...", matchDTO.getReferenceCode());
                continue;
            }

            Team teamHome = teamMap.get(matchDTO.getTeamHomeIsoCode());
            Team teamAway = teamMap.get(matchDTO.getTeamAwayIsoCode());

            if (teamHome == null || teamAway == null) {
                log.warn("Skipping match {}: team not found (home: {}, away: {})",
                        matchDTO.getReferenceCode(),
                        matchDTO.getTeamHomeIsoCode(),
                        matchDTO.getTeamAwayIsoCode());
                continue;
            }

            Match match = Match.builder()
                    .teamHome(teamHome)
                    .teamAway(teamAway)
                    .matchDate(LocalDateTime.parse(matchDTO.getMatchDate(), formatter))
                    .phase(MatchPhase.valueOf(matchDTO.getPhase()))
                    .groupName(matchDTO.getGroupName())
                    .status(MatchStatus.valueOf(matchDTO.getStatus()))
                    .referenceCode(matchDTO.getReferenceCode())
                    .build();

            matchRepository.save(match);
            insertedCount++;
            log.debug("Inserted match: {} vs {} on {}",
                    teamHome.getName(),
                    teamAway.getName(),
                    match.getMatchDate());
        }

        return insertedCount;
    }
}
