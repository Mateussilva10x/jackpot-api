package com.worldJackpot.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRankingDto {
    private Long id;
    private String name;
    private Integer totalPoints;
    private Integer rankingPosition;
}
