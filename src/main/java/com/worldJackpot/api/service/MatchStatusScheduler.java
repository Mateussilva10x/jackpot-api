package com.worldJackpot.api.service;

import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStatusScheduler {

    private final MatchRepository matchRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void markStartedMatchesAsInProgress() {
        var matches = matchRepository.findByStatusAndMatchDateBefore(MatchStatus.SCHEDULED, Instant.now());
        if (matches.isEmpty()) return;

        matches.forEach(match -> {
            match.setStatus(MatchStatus.IN_PROGRESS);
            log.info("Match {} ({} vs {}) marked as IN_PROGRESS", match.getId(),
                    match.getTeamHome().getName(), match.getTeamAway().getName());
        });

        matchRepository.saveAll(matches);
    }
}
