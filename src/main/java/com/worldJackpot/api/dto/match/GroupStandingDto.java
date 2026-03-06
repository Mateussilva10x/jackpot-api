package com.worldJackpot.api.dto.match;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupStandingDto {
    @Schema(description = "Name of the team", example = "Brazil")
    private String teamName;
    @Schema(description = "ISO code of the country", example = "BR")
    private String isoCode;
    @Schema(description = "URL to the team's flag image", example = "https://flag-url.com/br.png")
    private String flagUrl;
    @Schema(description = "Total points in the group stage", example = "9")
    private int points;
    @Schema(description = "Matches played", example = "3")
    private int matchesPlayed;
    @Schema(description = "Matches won", example = "3")
    private int wins;
    @Schema(description = "Matches drawn", example = "0")
    private int draws;
    @Schema(description = "Matches lost", example = "0")
    private int losses;
    @Schema(description = "Goals scored", example = "6")
    private int goalsFor;
    @Schema(description = "Goals conceded", example = "1")
    private int goalsAgainst;
    @Schema(description = "Goal difference", example = "5")
    private int goalDifference;
}
