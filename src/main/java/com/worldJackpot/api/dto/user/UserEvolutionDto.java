package com.worldJackpot.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvolutionDto {
    @Schema(description = "Checkpoint date (a day with FINISHED matches, UTC)", example = "2026-06-14")
    private LocalDate date;

    @Schema(description = "User's ranking position considering all points up to and including this date", example = "5")
    private Integer position;

    @Schema(description = "User's cumulative total points up to and including this date", example = "42")
    private Integer totalPoints;
}
