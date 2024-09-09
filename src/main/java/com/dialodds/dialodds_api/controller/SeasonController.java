package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seasons")
@Tag(name = "Seasons", description = "Season operations")
public class SeasonController {

    @Autowired
    private SeasonService seasonService;

    @PostMapping
    @Operation(summary = "Create a season", description = "Creates a new season")
    public ResponseEntity<Integer> createSeason(
            @Parameter(description = "Start week") @RequestParam int startWeek,
            @Parameter(description = "End week") @RequestParam int endWeek,
            @Parameter(description = "Initial coins") @RequestParam int initialCoins) {
        int seasonId = seasonService.createSeason(startWeek, endWeek, initialCoins);
        return ResponseEntity.ok(seasonId);
    }

    @GetMapping
    @Operation(summary = "Get all seasons", description = "Retrieves all seasons")
    public ResponseEntity<List<Map<String, Object>>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllSeasons());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active seasons", description = "Retrieves all active seasons")
    public ResponseEntity<List<Map<String, Object>>> getActiveSeasons() {
        return ResponseEntity.ok(seasonService.getActiveSeasons());
    }

    @GetMapping("/{seasonId}")
    @Operation(summary = "Get season by ID", description = "Retrieves a specific season by its ID")
    public ResponseEntity<Map<String, Object>> getSeasonById(
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        return ResponseEntity.ok(seasonService.getSeasonById(seasonId));
    }

    @DeleteMapping("/{seasonId}")
    @Operation(summary = "Delete season", description = "Deletes a specific season")
    public ResponseEntity<Map<String, Object>> deleteSeason(
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        Map<String, Object> result = seasonService.deleteSeason(seasonId);
        return ResponseEntity.ok(result);
    }
}