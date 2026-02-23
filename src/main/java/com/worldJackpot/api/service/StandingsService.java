package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.match.GroupStandingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StandingsService {

    private final MatchProgressionService matchProgressionService;

    public Map<String, List<GroupStandingDto>> calculateAllGroupStandings() {
        // matchProgressionService already has a method calculateGroupStandings(groupName)
        // However, we need to map Team -> GroupStandingDto.
        // Let's implement this calculation securely parsing the logic.
        return matchProgressionService.calculateAllGroupStandingsDto();
    }
}
