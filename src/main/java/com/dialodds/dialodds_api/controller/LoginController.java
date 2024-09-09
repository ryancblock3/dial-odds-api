package com.dialodds.dialodds_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Tag(name = "Login", description = "Login operations")
public class LoginController {

    @GetMapping("/login")
    @Operation(summary = "Get login page", description = "Retrieves the login page")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    @Operation(summary = "Redirect to pending games", description = "Redirects to the pending games page")
    public String home() {
        return "redirect:/admin/pending-games";
    }
}