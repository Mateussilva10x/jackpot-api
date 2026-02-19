package com.worldJackpot.api.dto.bet;

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
        private Long championTeamId;
        private Long runnerUpTeamId;
        private String topScorer;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BonusBetResponse {
        private Long id;
        private Long championTeamId;
        private String championTeamName;
        private Long runnerUpTeamId;
        private String runnerUpTeamName;
        private String topScorer;
    }
}
