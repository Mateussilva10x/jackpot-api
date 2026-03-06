package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.bet.BetDto;
import com.worldJackpot.api.dto.bet.BonusBetDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.service.BetService;
import com.worldJackpot.api.service.BonusBetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Bets", description = "Endpoints for placing normal and bonus bets")
@SecurityRequirement(name = "bearerAuth")
public class BetController {

    private final BetService betService;
    private final BonusBetService bonusBetService;

    @PostMapping("/bets")
    @Operation(summary = "Place regular bets", description = "Saves or updates user predictions for match scores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bets successfully placed"),
            @ApiResponse(responseCode = "400", description = "Invalid bet requested"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> placeBets(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody @Valid List<BetDto.BetRequest> bets) {
        Long userId = ((User) userDetails).getId();
        betService.placeBets(userId, bets);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bonus-bets")
    @Operation(summary = "Place a bonus bet", description = "Saves user predictions for champion, runner-up, and top scorer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bonus bet successfully placed"),
            @ApiResponse(responseCode = "400", description = "Invalid bonus bet requested"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> placeBonusBet(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody @Valid BonusBetDto.BonusBetRequest request) {
        Long userId = ((User) userDetails).getId();
        bonusBetService.placeBonusBet(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bonus-bets")
    @Operation(summary = "Get user's bonus bet", description = "Retrieves the user's previously placed bonus bet details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bonus bet"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<BonusBetDto.BonusBetResponse> getBonusBet(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((User) userDetails).getId();
        return ResponseEntity.ok(bonusBetService.getBonusBet(userId));
    }
}
