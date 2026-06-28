package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.match.GroupStanding;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchProgressionService {

    private final MatchRepository matchRepository;
    private final com.worldJackpot.api.repository.TeamRepository teamRepository;

    /**
     * Official FIFA World Cup 2026 constraint for placing the 8 best third-placed teams.
     * Maps each Round-of-32 third-place slot (placeholder ISO) to the set of groups whose
     * third-placed team is allowed to fill it. Derived from the official R32 schedule:
     *   3RD1 -> match 74 (1E),  3RD2 -> match 81 (1D),  3RD3 -> match 77 (1I),
     *   3RD4 -> match 79 (1A),  3RD5 -> match 80 (1L),  3RD6 -> match 82 (1G),
     *   3RD7 -> match 85 (1B),  3RD8 -> match 87 (1K).
     */
    private static final Map<String, Set<String>> THIRD_SLOT_ALLOWED_GROUPS = Map.of(
            "3RD1", Set.of("C", "D", "F", "G", "H"),
            "3RD2", Set.of("B", "E", "F", "I", "J"),
            "3RD3", Set.of("C", "D", "F", "G", "H"),
            "3RD4", Set.of("C", "E", "F", "H", "I"),
            "3RD5", Set.of("E", "H", "I", "J", "K"),
            "3RD6", Set.of("A", "E", "H", "I", "J"),
            "3RD7", Set.of("E", "F", "G", "I", "J"),
            "3RD8", Set.of("D", "E", "I", "J", "L")
    );

    private static final List<String> THIRD_SLOTS =
            List.of("3RD1", "3RD2", "3RD3", "3RD4", "3RD5", "3RD6", "3RD7", "3RD8");

    /**
     * Maps each third-place placeholder slot to the R32 match (referenceCode) whose AWAY team it fills.
     * Used to roll the away slot back to its placeholder before re-running the third-place allocation.
     */
    private static final Map<String, String> THIRD_SLOT_TO_R32_REFCODE = Map.of(
            "3RD1", "WC2026-R32-74",
            "3RD2", "WC2026-R32-81",
            "3RD3", "WC2026-R32-77",
            "3RD4", "WC2026-R32-79",
            "3RD5", "WC2026-R32-80",
            "3RD6", "WC2026-R32-82",
            "3RD7", "WC2026-R32-85",
            "3RD8", "WC2026-R32-87"
    );

    @Transactional
    public void processGroupStageCompletion(String groupName) {
        log.info("Checking if group {} is fully completed", groupName);
        
        List<Match> groupMatches = matchRepository.findByGroupNameAndPhase(groupName, MatchPhase.GROUP);
        boolean allFinished = groupMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        
        if (!allFinished) {
            log.debug("Group {} not fully finished yet.", groupName);
            return;
        }

        log.info("Group {} finished. Processing standings...", groupName);
        List<GroupStanding> standings = calculateGroupStandings(groupMatches);
        
        // At this point, standings are sorted. 
        // We'd typically determine 1st, 2nd, and 3rd here.
        // For a full World Cup 2026, we check all 12 groups eventually for the best 3rds.
        // But for assigning 1st and 2nd immediately:
        assignToRoundOf32(groupName, standings);
        
        // TODO: Best 3rds calculation needs to wait for ALL groups.
        checkAndProcessBestThirds();
    }

    private List<GroupStanding> calculateGroupStandings(List<Match> matches) {
        Map<Team, GroupStanding> standingMap = new HashMap<>();

        // Initialize all teams in the group first
        for (Match m : matches) {
            Team home = m.getTeamHome();
            Team away = m.getTeamAway();
            
            if (home != null) standingMap.putIfAbsent(home, new GroupStanding(home, 0, 0, 0, 0, 0, 0, 0, 0));
            if (away != null) standingMap.putIfAbsent(away, new GroupStanding(away, 0, 0, 0, 0, 0, 0, 0, 0));
        }

        for (Match m : matches) {
            if (m.getStatus() != MatchStatus.FINISHED) continue;
            
            Team home = m.getTeamHome();
            Team away = m.getTeamAway();
            
            GroupStanding homeStanding = standingMap.get(home);
            GroupStanding awayStanding = standingMap.get(away);
            
            int homeGoals = m.getHomeScore() == null ? 0 : m.getHomeScore();
            int awayGoals = m.getAwayScore() == null ? 0 : m.getAwayScore();
            
            // Add goals
            homeStanding.setGoalsFor(homeStanding.getGoalsFor() + homeGoals);
            homeStanding.setGoalsAgainst(homeStanding.getGoalsAgainst() + awayGoals);
            homeStanding.setGoalDifference(homeStanding.getGoalsFor() - homeStanding.getGoalsAgainst());
            
            awayStanding.setGoalsFor(awayStanding.getGoalsFor() + awayGoals);
            awayStanding.setGoalsAgainst(awayStanding.getGoalsAgainst() + homeGoals);
            awayStanding.setGoalDifference(awayStanding.getGoalsFor() - awayStanding.getGoalsAgainst());
            
            // Add points and W/D/L stats
            homeStanding.setMatchesPlayed(homeStanding.getMatchesPlayed() + 1);
            awayStanding.setMatchesPlayed(awayStanding.getMatchesPlayed() + 1);
            
            if (homeGoals > awayGoals) {
                homeStanding.setPoints(homeStanding.getPoints() + 3);
                homeStanding.setWins(homeStanding.getWins() + 1);
                awayStanding.setLosses(awayStanding.getLosses() + 1);
            } else if (awayGoals > homeGoals) {
                awayStanding.setPoints(awayStanding.getPoints() + 3);
                awayStanding.setWins(awayStanding.getWins() + 1);
                homeStanding.setLosses(homeStanding.getLosses() + 1);
            } else {
                homeStanding.setPoints(homeStanding.getPoints() + 1);
                awayStanding.setPoints(awayStanding.getPoints() + 1);
                homeStanding.setDraws(homeStanding.getDraws() + 1);
                awayStanding.setDraws(awayStanding.getDraws() + 1);
            }
        }

        // First order by overall points; ties are then broken by the official FIFA criteria.
        List<GroupStanding> byPoints = standingMap.values().stream()
                .sorted(Comparator.comparingInt(GroupStanding::getPoints).reversed())
                .collect(Collectors.toList());

        return resolveTieBreaks(byPoints, matches);
    }

    /**
     * Applies the official FIFA 2026 group tie-break order. Teams are first grouped by equal points;
     * within each tied block the order is decided by head-to-head (points, goal diff, goals for in
     * matches only among the tied teams), then falling back to overall goal difference and goals for.
     *
     * Note: fair-play (disciplinary) and drawing of lots are not implemented — the model does not
     * track cards. Also, this uses the common single-pass head-to-head approximation: it does not
     * recursively re-evaluate a subset that remains tied after head-to-head separates the others.
     */
    private List<GroupStanding> resolveTieBreaks(List<GroupStanding> byPoints, List<Match> matches) {
        List<GroupStanding> result = new ArrayList<>();
        int i = 0;
        while (i < byPoints.size()) {
            int j = i;
            int points = byPoints.get(i).getPoints();
            while (j < byPoints.size() && byPoints.get(j).getPoints() == points) {
                j++;
            }
            List<GroupStanding> tied = byPoints.subList(i, j);
            if (tied.size() == 1) {
                result.add(tied.get(0));
            } else {
                result.addAll(orderTiedTeams(tied, matches));
            }
            i = j;
        }
        return result;
    }

    /**
     * Orders teams tied on points using head-to-head results (matches among the tied teams only),
     * then overall goal difference and overall goals for.
     */
    private List<GroupStanding> orderTiedTeams(List<GroupStanding> tied, List<Match> matches) {
        Set<Team> tiedTeams = tied.stream().map(GroupStanding::getTeam).collect(Collectors.toSet());

        // h2h[team] = {points, goalDifference, goalsFor} considering only matches within the tied set
        Map<Team, int[]> h2h = new HashMap<>();
        for (Team t : tiedTeams) {
            h2h.put(t, new int[]{0, 0, 0});
        }

        for (Match m : matches) {
            if (m.getStatus() != MatchStatus.FINISHED) continue;
            Team home = m.getTeamHome();
            Team away = m.getTeamAway();
            if (home == null || away == null) continue;
            if (!tiedTeams.contains(home) || !tiedTeams.contains(away)) continue;

            int homeGoals = m.getHomeScore() == null ? 0 : m.getHomeScore();
            int awayGoals = m.getAwayScore() == null ? 0 : m.getAwayScore();

            int[] hs = h2h.get(home);
            int[] as = h2h.get(away);
            hs[1] += homeGoals - awayGoals;
            hs[2] += homeGoals;
            as[1] += awayGoals - homeGoals;
            as[2] += awayGoals;

            if (homeGoals > awayGoals) {
                hs[0] += 3;
            } else if (awayGoals > homeGoals) {
                as[0] += 3;
            } else {
                hs[0] += 1;
                as[0] += 1;
            }
        }

        return tied.stream()
                .sorted(Comparator
                        .comparingInt((GroupStanding s) -> h2h.get(s.getTeam())[0]).reversed()       // h2h points
                        .thenComparing(Comparator.comparingInt((GroupStanding s) -> h2h.get(s.getTeam())[1]).reversed()) // h2h goal diff
                        .thenComparing(Comparator.comparingInt((GroupStanding s) -> h2h.get(s.getTeam())[2]).reversed()) // h2h goals for
                        .thenComparing(Comparator.comparingInt(GroupStanding::getGoalDifference).reversed())            // overall goal diff
                        .thenComparing(Comparator.comparingInt(GroupStanding::getGoalsFor).reversed()))                 // overall goals for
                .collect(Collectors.toList());
    }

    private void assignToRoundOf32(String groupName, List<GroupStanding> standings) {
        if (standings.size() < 2) return;
        
        Team firstPlace = standings.get(0).getTeam();
        Team secondPlace = standings.get(1).getTeam();
        
        log.info("Group {} 1st: {}, 2nd: {}", groupName, firstPlace.getIsoCode(), secondPlace.getIsoCode());
        
        // We find the Match placeholders using the team's group placement code, e.g., "1A", "2A".
        // In our seed data, the R32 matches have placeholders like teamHomeIsoCode="1A"
        // This requires an additional query to find by placeholder ISO code, or updating the schema.
        // Let's assume we map it directly based on the ISO code of the placeholders we created.
        
        replacePlaceholderWithRealTeam("1" + groupName, firstPlace);
        replacePlaceholderWithRealTeam("2" + groupName, secondPlace);
    }
    
    private void replacePlaceholderWithRealTeam(String placeholderIso, Team realTeam) {
        // Need to find matches where teamHome or teamAway has this placeholder ISO.
        // This means the placeholder teams in DB ("1A", "2B") must be queried.
        // To be safe, we look across all matches.
        
        List<Match> matchesWithPlaceholderHome = matchRepository.findByTeamHomeIsoCode(placeholderIso);
        List<Match> matchesWithPlaceholderAway = matchRepository.findByTeamAwayIsoCode(placeholderIso);
        
        for (Match m : matchesWithPlaceholderHome) {
            m.setTeamHome(realTeam);
            matchRepository.save(m);
            log.info("Assigned {} to home slot of match {}", realTeam.getName(), m.getId());
        }
        
        for (Match m : matchesWithPlaceholderAway) {
            m.setTeamAway(realTeam);
            matchRepository.save(m);
            log.info("Assigned {} to away slot of match {}", realTeam.getName(), m.getId());
        }
    }
    
    private void checkAndProcessBestThirds() {
        // Check if all group matches in the tournament are done
        List<Match> allGroupMatches = matchRepository.findByPhase(MatchPhase.GROUP);
        boolean allGroupsFinished = allGroupMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        
        if (!allGroupsFinished) {
            return;
        }
        
        log.info("All groups finished. Calculating best 3rds...");

        // Third-placed team per group, keyed by group letter (e.g. "A").
        Map<String, Team> thirdTeamByGroup = new HashMap<>();
        Map<String, GroupStanding> thirdStandingByGroup = new HashMap<>();
        Map<String, List<Match>> matchesByGroup = allGroupMatches.stream().collect(Collectors.groupingBy(Match::getGroupName));

        for (Map.Entry<String, List<Match>> entry : matchesByGroup.entrySet()) {
            List<GroupStanding> standings = calculateGroupStandings(entry.getValue());
            if (standings.size() >= 3) {
                thirdTeamByGroup.put(entry.getKey(), standings.get(2).getTeam());
                thirdStandingByGroup.put(entry.getKey(), standings.get(2));
            }
        }

        // Rank all third-placed teams; the World Cup 2026 advances the 8 best thirds.
        List<String> rankedGroups = new ArrayList<>(thirdStandingByGroup.keySet());
        // FIFA best-third criteria: points -> goal diff -> goals for -> wins -> (fair play / draw, not implemented)
        rankedGroups.sort(Comparator
                .comparingInt((String g) -> thirdStandingByGroup.get(g).getPoints()).reversed()
                .thenComparing(Comparator.comparingInt((String g) -> thirdStandingByGroup.get(g).getGoalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((String g) -> thirdStandingByGroup.get(g).getGoalsFor()).reversed())
                .thenComparing(Comparator.comparingInt((String g) -> thirdStandingByGroup.get(g).getWins()).reversed()));

        int count = Math.min(rankedGroups.size(), 8);
        List<String> qualifiedGroups = new ArrayList<>(rankedGroups.subList(0, count));
        log.info("Qualified best-third groups: {}", qualifiedGroups);

        // Assign qualified groups to slots respecting the official allowed-group constraints.
        // FIFA publishes a fixed combination table; here we resolve a legal assignment so that no
        // third-placed team is ever placed into a slot its group is not allowed to fill. When more
        // than one legal assignment exists, the slots are filled in order trying groups alphabetically.
        Map<String, String> slotToGroup = assignThirdsToSlots(qualifiedGroups);
        if (slotToGroup == null) {
            log.error("No valid third-place assignment found for qualified groups {}. Slots left unfilled.", qualifiedGroups);
            return;
        }

        for (String slot : THIRD_SLOTS) {
            String group = slotToGroup.get(slot);
            if (group == null) continue;
            Team bestThird = thirdTeamByGroup.get(group);
            log.info("Assigning 3rd of group {} ({}) to slot {}", group, bestThird.getIsoCode(), slot);
            replacePlaceholderWithRealTeam(slot, bestThird);
        }
    }

    /**
     * Non-destructive re-resolution of the eight third-place R32 slots. Rolls each away slot back to
     * its placeholder team ("3RD1".."3RD8") and re-runs the third-place allocation with the current
     * (corrected) allowed-groups matrix. Group results and bets are preserved. Third-place R32 matches
     * that have already been played (FINISHED) are skipped so their results are not clobbered.
     */
    @Transactional
    public void reapplyBestThirds() {
        log.info("Re-applying best-third allocation to R32 slots.");

        for (String slot : THIRD_SLOTS) {
            String refCode = THIRD_SLOT_TO_R32_REFCODE.get(slot);
            Match match = matchRepository.findByReferenceCode(refCode).orElse(null);
            if (match == null) {
                log.warn("R32 match {} for slot {} not found. Skipping.", refCode, slot);
                continue;
            }
            if (match.getStatus() == MatchStatus.FINISHED) {
                log.warn("R32 match {} (slot {}) already FINISHED. Skipping reset to avoid clobbering result.", refCode, slot);
                continue;
            }
            Team placeholder = teamRepository.findByIsoCode(slot).orElse(null);
            if (placeholder == null) {
                log.error("Placeholder team {} not found. Cannot reset slot.", slot);
                continue;
            }
            match.setTeamAway(placeholder);
            matchRepository.save(match);
            log.info("Reset away slot of match {} back to placeholder {}.", refCode, slot);
        }

        checkAndProcessBestThirds();
        log.info("Best-third re-allocation finished.");
    }

    /**
     * Resolves a legal assignment of qualified third-place groups to R32 slots via backtracking,
     * honouring {@link #THIRD_SLOT_ALLOWED_GROUPS}. Returns slot->group, or null if impossible.
     */
    Map<String, String> assignThirdsToSlots(List<String> qualifiedGroups) {
        Map<String, String> result = new LinkedHashMap<>();
        if (backtrackAssign(0, qualifiedGroups, new HashSet<>(), result)) {
            return result;
        }
        return null;
    }

    private boolean backtrackAssign(int slotIndex, List<String> qualifiedGroups, Set<String> usedGroups, Map<String, String> result) {
        if (slotIndex == THIRD_SLOTS.size()) {
            return usedGroups.size() == qualifiedGroups.size();
        }
        String slot = THIRD_SLOTS.get(slotIndex);
        Set<String> allowed = THIRD_SLOT_ALLOWED_GROUPS.get(slot);
        List<String> candidates = qualifiedGroups.stream()
                .filter(g -> allowed.contains(g) && !usedGroups.contains(g))
                .sorted()
                .collect(Collectors.toList());
        for (String group : candidates) {
            usedGroups.add(group);
            result.put(slot, group);
            if (backtrackAssign(slotIndex + 1, qualifiedGroups, usedGroups, result)) {
                return true;
            }
            usedGroups.remove(group);
            result.remove(slot);
        }
        return false;
    }
    
    public Map<String, List<com.worldJackpot.api.dto.match.GroupStandingDto>> calculateAllGroupStandingsDto() {
        List<Match> allGroupMatches = matchRepository.findByPhase(MatchPhase.GROUP);
        Map<String, List<Match>> matchesByGroup = allGroupMatches.stream().collect(Collectors.groupingBy(Match::getGroupName));
        
        Map<String, List<com.worldJackpot.api.dto.match.GroupStandingDto>> result = new HashMap<>();
        
        for (Map.Entry<String, List<Match>> entry : matchesByGroup.entrySet()) {
            List<GroupStanding> standings = calculateGroupStandings(entry.getValue());
            List<com.worldJackpot.api.dto.match.GroupStandingDto> dtoList = standings.stream().map(s -> 
                com.worldJackpot.api.dto.match.GroupStandingDto.builder()
                        .teamName(s.getTeam().getName())
                        .isoCode(s.getTeam().getIsoCode())
                        .flagUrl(s.getTeam().getFlagUrl())
                        .points(s.getPoints())
                        .matchesPlayed(s.getMatchesPlayed())
                        .wins(s.getWins())
                        .draws(s.getDraws())
                        .losses(s.getLosses())
                        .goalsFor(s.getGoalsFor())
                        .goalsAgainst(s.getGoalsAgainst())
                        .goalDifference(s.getGoalDifference())
                        .build()
            ).collect(Collectors.toList());
            
            result.put(entry.getKey(), dtoList);
        }
        
        return result;
    }

    @Transactional
    public void processKnockoutProgression(Match finalizedMatch) {

        log.info("Processing knockout progression for match ID: {}", finalizedMatch.getId());

        if (finalizedMatch.getStatus() != MatchStatus.FINISHED) {
            log.warn("Match {} is not FINISHED. Skipping progression.", finalizedMatch.getId());
            return;
        }

        // Only process knockout phases
        if (finalizedMatch.getPhase() == MatchPhase.GROUP) {
            log.debug("Match {} is a group stage match. Knockout logic skipped.", finalizedMatch.getId());
            return;
        }

        // Extract match number from referenceCode (e.g. "WC2026-R32-73" -> "73")
        String referenceCode = finalizedMatch.getReferenceCode();
        if (referenceCode == null) {
            log.warn("Match {} has no referenceCode. Cannot determine winner placeholder.", finalizedMatch.getId());
            return;
        }

        int lastDash = referenceCode.lastIndexOf('-');
        if (lastDash < 0 || lastDash >= referenceCode.length() - 1) {
            log.warn("Match {} referenceCode '{}' has no numeric suffix.", finalizedMatch.getId(), referenceCode);
            return;
        }

        String matchNumberStr = referenceCode.substring(lastDash + 1);
        String winnerPlaceholderIso = "W" + matchNumberStr;

        Team winner = determineWinner(finalizedMatch);
        if (winner == null) {
            log.warn("Could not determine winner for match {} (draw with no penalty winner?).", finalizedMatch.getId());
            return;
        }

        log.info("Advancing winner {} via placeholder {} for match {}", winner.getName(), winnerPlaceholderIso, referenceCode);
        replacePlaceholderWithRealTeam(winnerPlaceholderIso, winner);

        // For SEMI-final matches also place the loser into the 3rd-place match
        if (finalizedMatch.getPhase() == MatchPhase.SEMI) {
            Team loser = determineLoser(finalizedMatch);
            if (loser != null) {
                // Placeholder slots L101 and L102 correspond to losers of the two semi-finals
                // We use numeric suffix to determine which loser slot: 101 -> L101, 102 -> L102
                String loserPlaceholderIso = "L" + matchNumberStr;
                log.info("Placing loser {} into 3rd-place slot {} for semi {}", loser.getName(), loserPlaceholderIso, referenceCode);
                replacePlaceholderWithRealTeam(loserPlaceholderIso, loser);
            }
        }
    }

    private Team determineWinner(Match match) {
        if (match.getPenaltyWinner() != null) {
            return match.getPenaltyWinner();
        }
        
        if (match.getHomeScore() == null || match.getAwayScore() == null) return null;
        
        if (match.getHomeScore() > match.getAwayScore()) {
            return match.getTeamHome();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            return match.getTeamAway();
        }
        
        return null;
    }

    private Team determineLoser(Match match) {
        Team winner = determineWinner(match);
        if (winner == null) return null;

        if (winner.equals(match.getTeamHome())) {
            return match.getTeamAway();
        } else {
            return match.getTeamHome();
        }
    }
}
