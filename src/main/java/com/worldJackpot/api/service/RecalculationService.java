package com.worldJackpot.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RecalculationService {

    @Transactional
    public void recalculatePoints(Long matchId) {
        log.info("Recalculating points for match ID: {}", matchId);
        // Implementation for BE-06 will go here
    }
}
