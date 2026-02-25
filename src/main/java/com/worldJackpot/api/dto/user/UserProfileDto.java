package com.worldJackpot.api.dto.user;

import com.worldJackpot.api.dto.bet.BetDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    @Schema(description = "User ID", example = "10")
    private Long id;
    
    @Schema(description = "User Name", example = "John Doe")
    private String name;
    
    @Schema(description = "User's total points", example = "150")
    private Integer totalPoints;
    
    @Schema(description = "User's position in global ranking", example = "5")
    private Integer rankingPosition;
    
    @Schema(description = "List of matches with the user's bets")
    private List<BetDto.MatchGroupResponse> bets;
}
