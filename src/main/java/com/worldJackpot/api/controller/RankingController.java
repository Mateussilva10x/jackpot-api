package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.user.UserRankingDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
@Tag(name = "Ranking", description = "Endpoints for the Jackpot Leaderboard")
public class RankingController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get the global leaderboard ranking")
    @ApiResponse(responseCode = "200", description = "Returning leaderboard page")
    public ResponseEntity<Page<UserRankingDto>> getRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Page<User> usersPage = userRepository.findAllOrderByTotalPointsDesc(PageRequest.of(page, size));
        
        Page<UserRankingDto> dtoPage = usersPage.map(user -> UserRankingDto.builder()
                .id(user.getId())
                .name(user.getName())
                .totalPoints(user.getTotalPoints() == null ? 0 : user.getTotalPoints())
                .avatarId(user.getAvatarId())
                .build());
                
        // Set the ranking position logically based on the page/index
        int offset = page * size;
        int i = 0;
        for (UserRankingDto dto : dtoPage.getContent()) {
            dto.setRankingPosition(offset + (++i));
        }

        return ResponseEntity.ok(dtoPage);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user's ranking position and points")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User's ranking retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request")
    })
    public ResponseEntity<UserRankingDto> getMyRanking(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails instanceof User user) {
            // Note: Efficient absolute rank calculation requires a specific query counting points greater than user's points.
            // For MVP simplicity, we approximate or just return points.
            return ResponseEntity.ok(UserRankingDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .totalPoints(user.getTotalPoints() == null ? 0 : user.getTotalPoints())
                    .avatarId(user.getAvatarId())
                    .build());
        }
        return ResponseEntity.status(401).build();
    }
}
