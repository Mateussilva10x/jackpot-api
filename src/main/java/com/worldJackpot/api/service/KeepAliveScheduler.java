package com.worldJackpot.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Keeps the API awake on Render free tier during game hours (09:00–23:59 BRT).
 * Pings the app every 14 minutes — just under Render's 15-minute inactivity timeout.
 * Outside of game hours the instance is allowed to sleep, preserving free-tier compute hours.
 */
@Slf4j
@Component
public class KeepAliveScheduler {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.self.url}")
    private String selfUrl;

    /**
     * Runs every 14 minutes between 09:00 and 23:59, Brasília time (UTC-3).
     * Covers all typical Copa do Mundo match windows (10h, 13h, 16h, 19h, 22h).
     */
    @Scheduled(cron = "0 */14 9-23 * * *", zone = "America/Sao_Paulo")
    public void keepAlive() {
        try {
            restTemplate.getForObject(selfUrl + "/matches", String.class);
            log.debug("Keep-alive ping OK → {}", selfUrl);
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
