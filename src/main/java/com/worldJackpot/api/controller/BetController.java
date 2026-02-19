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

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;
    private final BonusBetService bonusBetService;

    @PostMapping("/bets")
    public ResponseEntity<Void> placeBets(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody @Valid List<BetDto.BetRequest> bets) {
        Long userId = ((User) userDetails).getId();
        betService.placeBets(userId, bets);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bonus-bets")
    public ResponseEntity<Void> placeBonusBet(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody @Valid BonusBetDto.BonusBetRequest request) {
        Long userId = ((User) userDetails).getId();
        bonusBetService.placeBonusBet(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bonus-bets")
    public ResponseEntity<BonusBetDto.BonusBetResponse> getBonusBet(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((User) userDetails).getId();
        return ResponseEntity.ok(bonusBetService.getBonusBet(userId));
    }
}
