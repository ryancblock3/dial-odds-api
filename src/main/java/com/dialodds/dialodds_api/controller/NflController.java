package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.NflService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nfl")
@Tag(name = "NFL", description = "NFL game operations")
public class NflController {

    @Autowired
    private NflService nflService;

    @GetMapping("/weeks")
    @Operation(summary = "Get NFL weeks", description = "Retrieves all available NFL weeks")
    public ResponseEntity<List<Integer>> getNflWeeks() {
        return ResponseEntity.ok(nflService.getNflWeeks());
    }

    @GetMapping("/games/{week}")
    @Operation(summary = "Get NFL games by week", description = "Retrieves all NFL games for a specific week")
    public ResponseEntity<List<Map<String, Object>>> getNflGamesByWeek(
            @Parameter(description = "Week number") @PathVariable int week) {
        return ResponseEntity.ok(nflService.getNflGamesByWeek(week));
    }

    @GetMapping("/schedule/{team}")
    @Operation(summary = "Get team schedule", description = "Retrieves the schedule for a specific NFL team")
    public ResponseEntity<List<Map<String, Object>>> getTeamSchedule(
            @Parameter(description = "Team name") @PathVariable String team) {
        return ResponseEntity.ok(nflService.getTeamSchedule(team));
    }

    @GetMapping("/games/id/{gameId}")
    @Operation(summary = "Get game by ID", description = "Retrieves a specific game by its ID")
    public ResponseEntity<Map<String, Object>> getGameById(
            @Parameter(description = "Game ID") @PathVariable int gameId) {
        Map<String, Object> game = nflService.getGameById(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
    }

    @PostMapping("/games/{gameId}/result")
    @Operation(summary = "Update game result", description = "Updates the result of a specific game")
    public ResponseEntity<String> updateGameResult(
            @Parameter(description = "Game ID") @PathVariable int gameId,
            @Parameter(description = "Winner of the game") @RequestParam String winner) {
        try {
            nflService.updateGameResult(gameId, winner);
            return ResponseEntity.ok("Game result updated successfully and bets settled");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while updating the game result");
        }
    }

    @GetMapping("/games/pending-results")
    @Operation(summary = "Get games with pending results", description = "Retrieves all games that don't have a result yet")
    public ResponseEntity<List<Map<String, Object>>> getGamesWithPendingResults() {
        return ResponseEntity.ok(nflService.getGamesWithPendingResults());
    }
}