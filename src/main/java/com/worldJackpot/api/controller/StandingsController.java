package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.match.GroupStandingDto;
import com.worldJackpot.api.service.StandingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/standings")
@RequiredArgsConstructor
@Tag(name = "Standings", description = "Endpoints for World Cup Group Standings")
public class StandingsController {

    private final StandingsService standingsService;

    @GetMapping
    @Operation(summary = "Get the current World Cup group standings")
    public ResponseEntity<Map<String, List<GroupStandingDto>>> getGroupStandings() {
        return ResponseEntity.ok(standingsService.calculateAllGroupStandings());
    }
}
