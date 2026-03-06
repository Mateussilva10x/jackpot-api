package com.worldJackpot.api.config;

import com.worldJackpot.api.service.WorldCupSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeederRunner implements CommandLineRunner {

    private final WorldCupSeedService worldCupSeedService;

    @Override
    public void run(String... args) {
        log.info("Data seeding is enabled. Starting seeding process...");
        worldCupSeedService.seedWorldCupData();
    }
}
