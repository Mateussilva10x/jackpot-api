package com.worldJackpot.api.dto.bet;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "ID of the match", example = "10")
        @NotNull(message = "Match ID is required")
        private Long matchId;

        @Schema(description = "Predicted score for the home team", example = "2")
        @NotNull(message = "Home score is required")
        @Min(value = 0, message = "Score must be non-negative")
        private Integer homeScore;

        @Schema(description = "Predicted score for the away team", example = "1")
        @NotNull(message = "Away score is required")
        @Min(value = 0, message = "Score must be non-negative")
        private Integer awayScore;

        @Schema(description = "Selected winner ID for knockout matches if predicted score is a draw", example = "2")
        private Long selectedWinnerId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BetResponse {
        @Schema(description = "Bet ID", example = "100")
        private Long id;
        @Schema(description = "Match ID", example = "10")
        private Long matchId;
        @Schema(description = "Predicted home score", example = "2")
        private Integer homeScore;
        @Schema(description = "Predicted away score", example = "1")
        private Integer awayScore;
        @Schema(description = "Selected winner ID", example = "2")
        private Long selectedWinnerId;
        @Schema(description = "Timestamp when the bet was last updated")
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchBetResponse {
        @Schema(description = "Match ID", example = "10")
        private Long id;
        @Schema(description = "Home team name", example = "Brazil")
        private String homeTeam;
        @Schema(description = "Away team name", example = "Argentina")
        private String awayTeam;
        @Schema(description = "URL or code for home team flag", example = "BR")
        private String homeTeamFlag;
        @Schema(description = "URL or code for away team flag", example = "AR")
        private String awayTeamFlag;
        @Schema(description = "Match Date and Time")
        private LocalDateTime dateTime;
        @Schema(description = "Match Status", example = "SCHEDULED")
        private String status;
        @Schema(description = "Group the match belongs to", example = "G")
        private String group;
        @Schema(description = "User's current bet for this match (if any)")
        private BetResponse userBet;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchGroupResponse {
        @Schema(description = "Group Name", example = "G")
        private String group;
        @Schema(description = "List of matches in the group")
        private List<MatchBetResponse> matches;
        @Schema(description = "Optional rules or details for the group")
        private String ruleInfo;
    }
}
