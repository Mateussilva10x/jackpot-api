package com.worldJackpot.api.dto.bet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class BetDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BetRequest {
        @NotNull(message = "Match ID is required")
        private Long matchId;

        @NotNull(message = "Home score is required")
        @Min(value = 0, message = "Score must be non-negative")
        private Integer homeScore;

        @NotNull(message = "Away score is required")
        @Min(value = 0, message = "Score must be non-negative")
        private Integer awayScore;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BetResponse {
        private Long id;
        private Long matchId;
        private Integer homeScore;
        private Integer awayScore;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchBetResponse {
        private Long id;
        private String homeTeam;
        private String awayTeam;
        private String homeTeamFlag;
        private String awayTeamFlag;
        private LocalDateTime dateTime;
        private String status;
        private String group;
        private BetResponse userBet;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchGroupResponse {
        private String group;
        private List<MatchBetResponse> matches;
    }
}
