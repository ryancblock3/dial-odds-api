package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.NflService;
import com.dialodds.dialodds_api.service.OddsPopulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin operations")
public class AdminController {

    @Autowired
    private NflService nflService;

    @Autowired
    private OddsPopulationService oddsPopulationService;

    @GetMapping("/pending-games")
    @Operation(summary = "Get games with pending results", description = "Retrieves all games that don't have a result yet")
    public String pendingGames(Model model) {
        model.addAttribute("games", nflService.getGamesWithPendingResults());
        return "pending-games";
    }

    @PostMapping("/update-result")
    @Operation(summary = "Update game result", description = "Updates the result of a specific game")
    public String updateResult(
            @Parameter(description = "ID of the game") @RequestParam int gameId,
            @Parameter(description = "Winner of the game (home/away)") @RequestParam String winner) {
        nflService.updateGameResult(gameId, winner);
        return "redirect:/admin/pending-games";
    }

    @PostMapping("/update-database")
    @ResponseBody
    @Operation(summary = "Manually update database", description = "Triggers a manual update of the database")
    public String manuallyUpdateDatabase() {
        return oddsPopulationService.manuallyPopulateDatabase();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard", description = "Displays the admin dashboard")
    public String dashboard() {
        return "admin-dashboard";
    }
}