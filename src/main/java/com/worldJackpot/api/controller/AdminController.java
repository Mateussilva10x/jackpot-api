package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.match.MatchScoreUpdateDto;
import com.worldJackpot.api.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MatchService matchService;

    @PutMapping("/matches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> finalizeMatch(@PathVariable Long id, @RequestBody @Valid MatchScoreUpdateDto dto) {
        matchService.finalizeMatch(id, dto);
        return ResponseEntity.ok().build();
    }
}
