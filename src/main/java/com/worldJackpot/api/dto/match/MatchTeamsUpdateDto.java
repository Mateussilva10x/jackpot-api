package com.worldJackpot.api.dto.match;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchTeamsUpdateDto {

    @Schema(description = "ISO code of the team to place in the HOME slot. Null leaves the current home team unchanged.",
            example = "ARG")
    private String homeTeamIsoCode;

    @Schema(description = "ISO code of the team to place in the AWAY slot. Null leaves the current away team unchanged.",
            example = "EGY")
    private String awayTeamIsoCode;
}
