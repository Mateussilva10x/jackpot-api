package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.bet.BonusBetDto;
import com.worldJackpot.api.dto.match.MatchScoreUpdateDto;
import com.worldJackpot.api.service.BonusBetService;
import com.worldJackpot.api.service.MatchService;
import com.worldJackpot.api.service.SystemResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints for administrative actions")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final MatchService matchService;
    private final BonusBetService bonusBetService;
    private final SystemResetService systemResetService;

    @PutMapping("/matches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Finalize a match", description = "Updates a match score, checks bets, and assigns points to users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Match successfully finalized"),
            @ApiResponse(responseCode = "400", description = "Invalid request or match already finalized"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<Void> finalizeMatch(@PathVariable Long id, @RequestBody @Valid MatchScoreUpdateDto dto) {
        matchService.finalizeMatch(id, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bonus-bets/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve Bonus Bets", description = "Resolves all users' bonus bets against the official outcome and awards points.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bonus bets resolved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<Void> resolveBonusBets(@RequestBody @Valid BonusBetDto.BonusBetResolutionRequest request) {
        bonusBetService.resolveBonusBets(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/system/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset the entire database environment", description = "Wipes out all bets, results, and non-admin users. Re-seeds the matches from the initial backup. WARNING: extremely destructive operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Database successfully reset to initial state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<Void> resetDatabase() {
        systemResetService.resetDatabase();
        return ResponseEntity.ok().build();
    }
}
