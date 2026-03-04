package com.worldJackpot.api.service;

import com.worldJackpot.api.model.User;
import com.worldJackpot.api.model.enums.UserRole;
import com.worldJackpot.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BetRepository betRepository;
    private final BonusBetRepository bonusBetRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final WorldCupSeedService worldCupSeedService;

    @Transactional
    public void resetDatabase() {
        log.info("Starting database reset process...");

        // 1. Delete all password reset tokens
        passwordResetTokenRepository.deleteAllInBatch();
        log.info("Cleared password reset tokens.");

        // 2. Delete all bets
        betRepository.deleteAllInBatch();
        log.info("Cleared all regular bets.");

        // 3. Delete all bonus bets
        bonusBetRepository.deleteAllInBatch();
        log.info("Cleared all bonus bets.");

        // 4. Delete all matches (teams are preserved)
        // Need to clear foreign key relations first if any recursive ones exist.
        // Match table has a self-referencing nextMatchId but no strict DB-level cascading issues if we delete in batch safely
        // But to be completely safe against foreign keys pointing to itself, JPA might complain with batch delete
        // If it does, we can nullify the nextMatchId first or just use regular deleteAll() which takes care of order.
        // For Match, let's use deleteAll() initially to let Hibernate sort it out, or do a custom update.
        matchRepository.deleteAll();
        log.info("Cleared all matches.");

        // 5. Delete all non-admin users
        userRepository.deleteByRoleNot(UserRole.ADMIN);
        log.info("Cleared non-admin users.");

        // 6. Reset points for remaining admins
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        for (User admin : admins) {
            admin.setTotalPoints(0);
            admin.setRankingPosition(null);
            userRepository.save(admin);
        }
        log.info("Reset points for admin users.");

        // 7. Reseed matches from JSON
        worldCupSeedService.seedWorldCupData();
        log.info("Reseeded World Cup definitions.");

        log.info("Database reset completed successfully.");
    }
}
