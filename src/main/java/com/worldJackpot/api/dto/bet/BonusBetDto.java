package com.worldJackpot.api.dto.bet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BonusBetDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BonusBetRequest {
        @Schema(description = "ID of the predicted champion team", example = "1")
        private Long championTeamId;
        @Schema(description = "ID of the predicted runner-up team", example = "2")
        private Long runnerUpTeamId;
        @Schema(description = "Name of the predicted top scorer", example = "Vinicius Junior")
        private String topScorer;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BonusBetResponse {
        @Schema(description = "Bonus Bet ID", example = "50")
        private Long id;
        @Schema(description = "ID of the predicted champion team", example = "1")
        private Long championTeamId;
        @Schema(description = "Name of the predicted champion team", example = "Brazil")
        private String championTeamName;
        @Schema(description = "ID of the predicted runner-up team", example = "2")
        private Long runnerUpTeamId;
        @Schema(description = "Name of the predicted runner-up team", example = "France")
        private String runnerUpTeamName;
        @Schema(description = "Name of the predicted top scorer", example = "Vinicius Junior")
        private String topScorer;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BonusBetResolutionRequest {
        @Schema(description = "ID of the final champion team", example = "1")
        private Long championTeamId;
        
        @Schema(description = "ID of the final runner-up team", example = "2")
        private Long runnerUpTeamId;
        
        @Schema(description = "Name of the final top scorer", example = "Vinicius Junior")
        private String topScorer;
    }
}
