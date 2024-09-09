package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User operations")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    @Operation(summary = "Create or update user", description = "Creates a new user or updates an existing one")
    public ResponseEntity<Integer> createOrUpdateUser(
            @Parameter(description = "Discord ID of the user") @RequestParam String discordId,
            @Parameter(description = "Username") @RequestParam String username) {
        int userId = userService.createOrUpdateUser(discordId, username);
        return ResponseEntity.ok(userId);
    }

    @PostMapping("/{userId}/seasons/{seasonId}")
    @Operation(summary = "Add user to season", description = "Adds a user to a specific season")
    public ResponseEntity<Void> addUserToSeason(
            @Parameter(description = "User ID") @PathVariable int userId,
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        userService.addUserToSeason(userId, seasonId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/seasons/{seasonId}")
    @Operation(summary = "Get users by season", description = "Retrieves all users participating in a specific season")
    public ResponseEntity<List<Map<String, Object>>> getUsersBySeason(
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        return ResponseEntity.ok(userService.getUsersBySeason(seasonId));
    }

    @GetMapping("/{userId}/seasons/{seasonId}/coins")
    @Operation(summary = "Get user coins", description = "Retrieves the number of coins a user has in a specific season")
    public ResponseEntity<Integer> getUserCoins(
            @Parameter(description = "User ID") @PathVariable int userId,
            @Parameter(description = "Season ID") @PathVariable int seasonId) {
        return ResponseEntity.ok(userService.getUserCoins(userId, seasonId));
    }

    @PostMapping("/join-season")
    @Operation(summary = "Create user and join season", description = "Creates a new user and adds them to a specific season")
    public ResponseEntity<Void> createUserAndJoinSeason(
            @Parameter(description = "Discord ID of the user") @RequestParam String discordId,
            @Parameter(description = "Username") @RequestParam String username,
            @Parameter(description = "Season ID") @RequestParam int seasonId) {
        userService.createUserAndJoinSeason(discordId, username, seasonId);
        return ResponseEntity.ok().build();
    }
}