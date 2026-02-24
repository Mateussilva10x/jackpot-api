package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Endpoints for fetching match data")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    @Operation(summary = "Get all matches", description = "Retrieves a list of matches grouped by their corresponding stages.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matches")
    public ResponseEntity<List<BetDto.MatchGroupResponse>> getMatches(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String type) {
        Long userId = null;
        if (userDetails != null && userDetails instanceof User) {
            userId = ((User) userDetails).getId();
        }
        
        // If UserDetails isn't castable to User directly in implementation, 
        // we might need to look it up, but usually our CustomUserDetailsService returns our User entity.
        // Let's verify CustomUserDetailsService implementation.
        // It returns 'User' which implements UserDetails. So the cast (User) userDetails is safe IF authenticated.

        return ResponseEntity.ok(matchService.getMatchesGroupedByGroup(userId, type));
    }
}
