package com.worldJackpot.api.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchScoreUpdateDto {

    @NotNull(message = "Home score is required")
    @Min(value = 0, message = "Home score must be non-negative")
    private Integer homeScore;

    @NotNull(message = "Away score is required")
    @Min(value = 0, message = "Away score must be non-negative")
    private Integer awayScore;
}
