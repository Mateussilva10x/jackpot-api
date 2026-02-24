package com.worldJackpot.api.dto.match;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchScoreUpdateDto {

    @Schema(description = "Final score for the home team", example = "2")
    @NotNull(message = "Home score is required")
    @Min(value = 0, message = "Home score must be non-negative")
    private Integer homeScore;

    @Schema(description = "Final score for the away team", example = "1")
    @NotNull(message = "Away score is required")
    @Min(value = 0, message = "Away score must be non-negative")
    private Integer awayScore;

    @Schema(description = "ID of the team that won on penalties, if applicable", example = "3")
    private Long penaltyWinnerId;
}
