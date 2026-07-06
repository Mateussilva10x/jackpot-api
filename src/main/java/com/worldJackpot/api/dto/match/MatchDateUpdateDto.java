package com.worldJackpot.api.dto.match;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchDateUpdateDto {

    @Schema(description = "New kickoff instant in UTC (ISO-8601). Ex: 21h BRT = 00:00 UTC next day",
            example = "2026-07-07T00:00:00Z")
    @NotNull(message = "matchDate is required")
    private Instant matchDate;
}
