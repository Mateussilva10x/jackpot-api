package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.match.GroupStanding;
import com.worldJackpot.api.model.Match;
import com.worldJackpot.api.model.Team;
import com.worldJackpot.api.model.enums.MatchPhase;
import com.worldJackpot.api.model.enums.MatchStatus;
import com.worldJackpot.api.model.enums.NextMatchSlot;
import com.worldJackpot.api.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchProgressionService {

    private final MatchRepository matchRepository;

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

        for (Match m : matches) {
            if (m.getStatus() != MatchStatus.FINISHED) continue;
            
            Team home = m.getTeamHome();
            Team away = m.getTeamAway();
            
            standingMap.putIfAbsent(home, new GroupStanding(home, 0, 0, 0, 0));
            standingMap.putIfAbsent(away, new GroupStanding(away, 0, 0, 0, 0));
            
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
            
            // Add points
            if (homeGoals > awayGoals) {
                homeStanding.setPoints(homeStanding.getPoints() + 3);
            } else if (awayGoals > homeGoals) {
                awayStanding.setPoints(awayStanding.getPoints() + 3);
            } else {
                homeStanding.setPoints(homeStanding.getPoints() + 1);
                awayStanding.setPoints(awayStanding.getPoints() + 1);
            }
        }

        // Sort: Points DESC, GoalDiff DESC, GoalsFor DESC
        return standingMap.values().stream()
                .sorted(Comparator.comparingInt(GroupStanding::getPoints).reversed()
                        .thenComparing(Comparator.comparingInt(GroupStanding::getGoalDifference).reversed())
                        .thenComparing(Comparator.comparingInt(GroupStanding::getGoalsFor).reversed()))
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
        
        List<GroupStanding> allThirds = new ArrayList<>();
        // Group by groupName
        Map<String, List<Match>> matchesByGroup = allGroupMatches.stream().collect(Collectors.groupingBy(Match::getGroupName));
        
        for (List<Match> groupMatches : matchesByGroup.values()) {
            List<GroupStanding> standings = calculateGroupStandings(groupMatches);
            if (standings.size() >= 3) {
                allThirds.add(standings.get(2));
            }
        }
        
        // Sort best 3rds
        allThirds.sort(Comparator.comparingInt(GroupStanding::getPoints).reversed()
                .thenComparing(Comparator.comparingInt(GroupStanding::getGoalDifference).reversed())
                .thenComparing(Comparator.comparingInt(GroupStanding::getGoalsFor).reversed()));
                
        // World Cup 2026 has 12 groups, advancing 8 best 3rds
        int count = Math.min(allThirds.size(), 8);
        for (int i = 0; i < count; i++) {
            Team bestThird = allThirds.get(i).getTeam();
            // The placeholder codes for best thirds in our seed data are "3RD1", "3RD2" ... "3RD8"
            String placeholderIso = "3RD" + (i + 1);
            replacePlaceholderWithRealTeam(placeholderIso, bestThird);
        }
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

        if (finalizedMatch.getNextMatchId() == null) {
            log.info("Match {} has no nextMatchId. This might be the final.", finalizedMatch.getId());
            return;
        }

        Match nextMatch = matchRepository.findById(finalizedMatch.getNextMatchId())
                .orElse(null);

        if (nextMatch == null) {
            log.error("Next match with ID {} not found for match {}", finalizedMatch.getNextMatchId(), finalizedMatch.getId());
            return;
        }

        Team winner = determineWinner(finalizedMatch);
        if (winner == null) {
            log.warn("Could not determine winner for match {} (perhaps a draw in knockout?). Needs manual intervention or penalty logic.", finalizedMatch.getId());
            return;
        }

        if (finalizedMatch.getNextMatchSlot() == NextMatchSlot.HOME) {
            nextMatch.setTeamHome(winner);
        } else if (finalizedMatch.getNextMatchSlot() == NextMatchSlot.AWAY) {
            nextMatch.setTeamAway(winner);
        } else {
             log.error("Match {} has nextMatchId but no nextMatchSlot configured!", finalizedMatch.getId());
             return;
        }
        
        matchRepository.save(nextMatch);
        log.info("Successfully advanced team {} to next match {}", winner.getName(), nextMatch.getId());
    }

    private Team determineWinner(Match match) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) return null;
        
        if (match.getHomeScore() > match.getAwayScore()) {
            return match.getTeamHome();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            return match.getTeamAway();
        }
        
        // TODO: Handle penalty shootouts for knockout draws
        return null;
    }
}
