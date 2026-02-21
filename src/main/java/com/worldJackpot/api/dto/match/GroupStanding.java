package com.worldJackpot.api.dto.match;

import com.worldJackpot.api.model.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupStanding {
    private Team team;
    private int points;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
}
