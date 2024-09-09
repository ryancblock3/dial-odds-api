package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bets")
@Tag(name = "Bets", description = "Bet operations")
public class BetController {

    @Autowired
    private BetService betService;

    @PostMapping
    @Operation(summary = "Place a bet", description = "Places a new bet for a user")
    public ResponseEntity<Integer> placeBet(
            @Parameter(description = "User ID") @RequestParam int userId,
            @Parameter(description = "Season ID") @RequestParam int seasonId,
            @Parameter(description = "Game ID") @RequestParam int gameId,
            @Parameter(description = "Type of bet") @RequestParam String betType,
            @Parameter(description = "Amount of bet") @RequestParam int amount) {
        int betId = betService.placeBet(userId, seasonId, gameId, betType, amount);
        return ResponseEntity.ok(betId);
    }

    @GetMapping("/users/{userId}/seasons/{seasonId}")
    @Operation(summary = "Get user bets", description = "Retrieves all bets for a specific user in a specific season")
    public ResponseEntity<List<Map<String, Object>>> getUserBets(
            @Parameter(description = "User ID") @PathVariable int userId,
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        return ResponseEntity.ok(betService.getUserBets(userId, seasonId));
    }

    @PostMapping("/settle/{gameId}")
    @Operation(summary = "Settle bets", description = "Settles all bets for a specific game")
    public ResponseEntity<Void> settleBets(
            @Parameter(description = "Game ID") @PathVariable int gameId,
            @Parameter(description = "Winner of the game") @RequestParam String winner) {
        betService.settleBets(gameId, winner);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unsettled/{gameId}")
    @Operation(summary = "Get unsettled bets", description = "Retrieves all unsettled bets for a specific game")
    public ResponseEntity<List<Map<String, Object>>> getUnsettledBetsForGame(
            @Parameter(description = "Game ID") @PathVariable int gameId) {
        return ResponseEntity.ok(betService.getUnsettledBetsForGame(gameId));
    }

    @GetMapping("/seasons/{seasonId}")
    @Operation(summary = "Get bets for season", description = "Retrieves all bets for a specific season")
    public ResponseEntity<List<Map<String, Object>>> getBetsForSeason(
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        return ResponseEntity.ok(betService.getBetsForSeason(seasonId));
    }

    @PostMapping("/correct-bet")
    @ResponseBody
    @Operation(summary = "Correct bet result", description = "Corrects the result of a specific bet")
    public ResponseEntity<String> correctBetResult(
            @Parameter(description = "Bet ID") @RequestParam int betId,
            @Parameter(description = "New result") @RequestParam String newResult) {
        try {
            betService.correctBetResult(betId, newResult);
            return ResponseEntity.ok("Bet corrected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error correcting bet: " + e.getMessage());
        }
    }

    @PostMapping("/audit-and-correct")
    @Operation(summary = "Audit and correct all bets", description = "Audits and corrects all bets in the system")
    public ResponseEntity<String> auditAndCorrectAllBets() {
        try {
            betService.auditAndCorrectAllBets();
            return ResponseEntity.ok("All bets audited and corrected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error auditing and correcting bets: " + e.getMessage());
        }
    }
}