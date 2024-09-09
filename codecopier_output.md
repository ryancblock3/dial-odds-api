# CodeCopier Output

## File: src/main/java/com/dialodds/dialodds_api/config/RestTemplateConfig.java

```java
package com.dialodds.dialodds_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

```

## File: src/main/java/com/dialodds/dialodds_api/config/SecurityConfig.java

```java
package com.dialodds.dialodds_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${remember.me.key}")
    private String rememberMeKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/api/**").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .tokenValiditySeconds(2592000)); // 30 days

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/config/SwaggerConfig.java

```java
package com.dialodds.dialodds_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI dialOddsOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("DialOdds API")
                        .description("NFL betting application API")
                        .version("v0.0.1"));
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/controller/AdminController.java

```java
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
```

## File: src/main/java/com/dialodds/dialodds_api/controller/BetController.java

```java
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
```

## File: src/main/java/com/dialodds/dialodds_api/controller/HomeController.java

```java
package com.dialodds.dialodds_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = "text/html")
    public String home() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>NFL API Home</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            color: #2c3e50;\n" +
                "        }\n" +
                "        .endpoint {\n" +
                "            background-color: #f4f4f4;\n" +
                "            border: 1px solid #ddd;\n" +
                "            border-radius: 4px;\n" +
                "            padding: 10px;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .method {\n" +
                "            font-weight: bold;\n" +
                "            color: #2980b9;\n" +
                "        }\n" +
                "        .path {\n" +
                "            color: #27ae60;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Welcome to the DialOdds API</h1>\n" +
                "    <p>Here are the available endpoints:</p>\n" +
                "    \n" +
                "    <div class=\"endpoint\">\n" +
                "        <span class=\"method\">GET</span> \n" +
                "        <span class=\"path\">/api/nfl/weeks</span>\n" +
                "        <p>Retrieves a list of available NFL weeks.</p>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"endpoint\">\n" +
                "        <span class=\"method\">GET</span> \n" +
                "        <span class=\"path\">/api/nfl/games/{week}</span>\n" +
                "        <p>Retrieves NFL games for a specific week. Replace {week} with the desired week number.</p>\n"
                +
                "    </div>\n" +
                // Add this to the HTML string in the home() method
                "    <div class=\"endpoint\">\n" +
                "        <span class=\"method\">GET</span> \n" +
                "        <span class=\"path\">/api/nfl/schedule/{team}</span>\n" +
                "        <p>Retrieves the schedule for a specific NFL team. Replace {team} with the team name.</p>\n" +
                "    </div>\n" +
                "    \n" +
                "    <p>To use these endpoints, append them to the base URL: <code>http://localhost:8080</code></p>\n" +
                "</body>\n" +
                "</html>";
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/controller/LoginController.java

```java
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
```

## File: src/main/java/com/dialodds/dialodds_api/controller/ManualUpdateController.java

```java
package com.dialodds.dialodds_api.controller;

import com.dialodds.dialodds_api.service.OddsPopulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Manual Update", description = "Manual database update operations")
public class ManualUpdateController {

    @Autowired
    private OddsPopulationService oddsPopulationService;

    @PostMapping("/update-database")
    @Operation(summary = "Manually update database", description = "Triggers a manual update of the database")
    public ResponseEntity<String> manuallyUpdateDatabase() {
        String result = oddsPopulationService.manuallyPopulateDatabase();
        return ResponseEntity.ok(result);
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/controller/NflController.java

```java
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
```

## File: src/main/java/com/dialodds/dialodds_api/controller/SeasonController.java

```java
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
```

## File: src/main/java/com/dialodds/dialodds_api/controller/UserController.java

```java
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
```

## File: src/main/java/com/dialodds/dialodds_api/DatabasePopulationScheduler.java

```java
package com.dialodds.dialodds_api;

import com.dialodds.dialodds_api.service.OddsPopulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabasePopulationScheduler {

    @Autowired
    private OddsPopulationService oddsPopulationService;

    // Run every day at 6:00 AM EST
    @Scheduled(cron = "0 0 6 * * *", zone = "America/New_York")
    public void morningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every day at 8:00 PM EST
    @Scheduled(cron = "0 0 20 * * *", zone = "America/New_York")
    public void eveningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Thursday at 6:00 PM EST
    @Scheduled(cron = "0 0 18 * * THU", zone = "America/New_York")
    public void thursdayEveningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Sunday at 11:00 AM EST
    @Scheduled(cron = "0 0 11 * * SUN", zone = "America/New_York")
    public void sundayMorningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Sunday at 2:30 PM EST
    @Scheduled(cron = "0 30 14 * * SUN", zone = "America/New_York")
    public void sundayAfternoonUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Sunday at 6:00 PM EST
    @Scheduled(cron = "0 0 18 * * SUN", zone = "America/New_York")
    public void sundayEveningUpdate() {
        oddsPopulationService.populateDatabase();
    }

    // Run every Monday at 6:00 PM EST
    @Scheduled(cron = "0 0 18 * * MON", zone = "America/New_York")
    public void mondayEveningUpdate() {
        oddsPopulationService.populateDatabase();
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/DialoddsApiApplication.java

```java
package com.dialodds.dialodds_api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class DialoddsApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(DialoddsApiApplication.class);

    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure()
                              .directory("./")  // Look in the current directory
                              .ignoreIfMissing()
                              .load();
        
        // Set environment variables
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
            logger.info("Setting env variable: {} = {}", entry.getKey(), maskSensitiveValue(entry.getKey(), entry.getValue()));
        });
        
        SpringApplication.run(DialoddsApiApplication.class, args);
    }

    private static String maskSensitiveValue(String key, String value) {
        if (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
            return "*****";
        }
        return value;
    }

    private static String maskUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("://.*?@", "://*****@");
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/service/BetService.java

```java
package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BetService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public int placeBet(int userId, int seasonId, int gameId, String betType, int amount) {
        // Check if user has enough coins
        String checkCoinsSQL = "SELECT coins FROM user_seasons WHERE user_id = ? AND season_id = ?";
        Integer userCoins = jdbcTemplate.queryForObject(checkCoinsSQL, Integer.class, userId, seasonId);

        if (userCoins == null || userCoins < amount) {
            throw new IllegalArgumentException("Not enough coins to place bet");
        }

        // Place the bet
        String placeBetSQL = "INSERT INTO bets (user_id, season_id, game_id, bet_type, amount) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Integer betId = jdbcTemplate.queryForObject(placeBetSQL, Integer.class, userId, seasonId, gameId, betType,
                amount);

        // Update user's coins
        String updateCoinsSQL = "UPDATE user_seasons SET coins = coins - ? WHERE user_id = ? AND season_id = ?";
        jdbcTemplate.update(updateCoinsSQL, amount, userId, seasonId);

        return betId != null ? betId : -1;
    }

    public List<Map<String, Object>> getUserBets(int userId, int seasonId) {
        String sql = "SELECT b.id, b.game_id, g.home_team, g.away_team, b.bet_type, b.amount, b.result " +
                "FROM bets b JOIN games g ON b.game_id = g.id " +
                "WHERE b.user_id = ? AND b.season_id = ? " +
                "ORDER BY b.created_at DESC";
        return jdbcTemplate.queryForList(sql, userId, seasonId);
    }

    public List<Map<String, Object>> getUnsettledBetsForGame(int gameId) {
        String sql = "SELECT b.id, b.user_id, u.username, b.season_id, b.bet_type, b.amount " +
                "FROM bets b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE b.game_id = ? AND b.result IS NULL";
        return jdbcTemplate.queryForList(sql, gameId);
    }

    @Transactional
    public void settleBets(int gameId, String winner) {
        // Get all unsettled bets for this game
        List<Map<String, Object>> unsettledBets = getUnsettledBetsForGame(gameId);

        for (Map<String, Object> bet : unsettledBets) {
            int betId = (Integer) bet.get("id");
            int userId = (Integer) bet.get("user_id");
            int seasonId = (Integer) bet.get("season_id");
            String betType = ((String) bet.get("bet_type")).toLowerCase();
            int amount = (Integer) bet.get("amount");

            boolean won = (betType.equals("home") && winner.toLowerCase().equals("home"))
                    || (betType.equals("away") && winner.toLowerCase().equals("away"));

            // Update bet result
            String updateBetSQL = "UPDATE bets SET result = ? WHERE id = ?";
            jdbcTemplate.update(updateBetSQL, won ? "won" : "lost", betId);

            if (won) {
                // Get odds for the game
                String getOddsSQL = "SELECT home_odds, away_odds FROM odds WHERE game_id = ?";
                Map<String, Object> odds = jdbcTemplate.queryForMap(getOddsSQL, gameId);
                double winningOdds = betType.equals("home") ? (Double) odds.get("home_odds")
                        : (Double) odds.get("away_odds");

                // Calculate winnings
                int winnings = (int) (amount * (winningOdds - 1)); // Modified calculation

                // Update user's coins
                String updateCoinsSQL = "UPDATE user_seasons SET coins = coins + ? WHERE user_id = ? AND season_id = ?";
                jdbcTemplate.update(updateCoinsSQL, winnings + amount, userId, seasonId); // Return the original bet
                                                                                          // amount as well
            }
        }
    }

    public List<Map<String, Object>> getBetsForSeason(int seasonId) {
        String sql = "SELECT b.id, b.user_id, u.username, b.game_id, g.home_team, g.away_team, b.bet_type, " +
                "b.amount, b.result, g.commence_time, g.result AS game_result " +
                "FROM bets b " +
                "JOIN users u ON b.user_id = u.id " +
                "JOIN games g ON b.game_id = g.id " +
                "WHERE b.season_id = ? " +
                "ORDER BY g.commence_time DESC";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, seasonId);

        // Post-process to add bet_on_team and correct result
        for (Map<String, Object> bet : results) {
            String betType = (String) bet.get("bet_type");
            String homeTeam = (String) bet.get("home_team");
            String awayTeam = (String) bet.get("away_team");
            String gameResult = (String) bet.get("game_result");

            // Determine bet_on_team
            bet.put("bet_on_team", betType.equalsIgnoreCase("HOME") ? homeTeam : awayTeam);

            // Correct the result if necessary
            if (gameResult != null) {
                boolean betWon = (betType.equalsIgnoreCase("HOME") && gameResult.equalsIgnoreCase("home")) ||
                        (betType.equalsIgnoreCase("AWAY") && gameResult.equalsIgnoreCase("away"));
                bet.put("result", betWon ? "won" : "lost");
            }
        }

        return results;
    }

    @Transactional
    public void correctBetResult(int betId, String newResult) {
        // Fetch the bet
        String fetchBetSQL = "SELECT b.*, g.result AS game_result FROM bets b JOIN games g ON b.game_id = g.id WHERE b.id = ?";
        Map<String, Object> bet = jdbcTemplate.queryForMap(fetchBetSQL, betId);

        int userId = (Integer) bet.get("user_id");
        int seasonId = (Integer) bet.get("season_id");
        int gameId = (Integer) bet.get("game_id");
        String betType = ((String) bet.get("bet_type")).toLowerCase();
        int amount = (Integer) bet.get("amount");
        String currentResult = (String) bet.get("result");
        String gameResult = (String) bet.get("game_result");

        // Determine the correct result based on the game result and bet type
        boolean correctResult = (betType.equals("home") && gameResult.equalsIgnoreCase("home")) ||
                (betType.equals("away") && gameResult.equalsIgnoreCase("away"));
        String correctResultString = correctResult ? "won" : "lost";

        // Only update if the new result matches the correct result
        if (!correctResultString.equalsIgnoreCase(newResult)) {
            throw new IllegalArgumentException("The new result does not match the game outcome.");
        }

        // Update bet result
        String updateBetSQL = "UPDATE bets SET result = ? WHERE id = ?";
        jdbcTemplate.update(updateBetSQL, newResult.toLowerCase(), betId);

        // Correct user's coins
        correctUserCoins(userId, seasonId, amount, newResult.toLowerCase(), currentResult, gameId, betType);
    }

    private void correctUserCoins(int userId, int seasonId, int amount, String newResult, String currentResult,
            int gameId, String betType) {
        String getOddsSQL = "SELECT home_odds, away_odds FROM odds WHERE game_id = ?";
        Map<String, Object> odds = jdbcTemplate.queryForMap(getOddsSQL, gameId);
        double winningOdds = betType.equals("home") ? (Double) odds.get("home_odds") : (Double) odds.get("away_odds");

        int coinAdjustment = 0;

        if (newResult.equals("won") && !currentResult.equals("won")) {
            // Bet should be won but wasn't marked as such
            int winnings = (int) (amount * (winningOdds - 1));
            coinAdjustment = winnings + amount;
        } else if (newResult.equals("lost") && currentResult.equals("won")) {
            // Bet was incorrectly marked as won
            int winnings = (int) (amount * (winningOdds - 1));
            coinAdjustment = -(winnings + amount);
        }

        if (coinAdjustment != 0) {
            String updateCoinsSQL = "UPDATE user_seasons SET coins = coins + ? WHERE user_id = ? AND season_id = ?";
            jdbcTemplate.update(updateCoinsSQL, coinAdjustment, userId, seasonId);
        }
    }

    @Transactional
    public void auditAndCorrectAllBets() {
        String sql = "SELECT b.id, b.user_id, b.season_id, b.game_id, b.bet_type, b.amount, b.result, " +
                "g.result AS game_result " +
                "FROM bets b " +
                "JOIN games g ON b.game_id = g.id " +
                "WHERE g.result IS NOT NULL";

        List<Map<String, Object>> allBets = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> bet : allBets) {
            int betId = (Integer) bet.get("id");
            int userId = (Integer) bet.get("user_id");
            int seasonId = (Integer) bet.get("season_id");
            int gameId = (Integer) bet.get("game_id");
            String betType = ((String) bet.get("bet_type")).toLowerCase();
            int amount = (Integer) bet.get("amount");
            String currentResult = (String) bet.get("result");
            String gameResult = (String) bet.get("game_result");

            boolean correctResult = (betType.equals("home") && gameResult.equalsIgnoreCase("home")) ||
                    (betType.equals("away") && gameResult.equalsIgnoreCase("away"));
            String correctResultString = correctResult ? "won" : "lost";

            if (!correctResultString.equals(currentResult)) {
                // Update bet result
                String updateBetSQL = "UPDATE bets SET result = ? WHERE id = ?";
                jdbcTemplate.update(updateBetSQL, correctResultString, betId);

                // Correct user's coins
                correctUserCoins(userId, seasonId, amount, correctResultString, currentResult, gameId, betType);
            }
        }
    }

}
```

## File: src/main/java/com/dialodds/dialodds_api/service/NflService.java

```java
package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.sql.Timestamp;

@Service
public class NflService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BetService betService;

    public List<Integer> getNflWeeks() {
        String sql = "SELECT DISTINCT nfl_week " +
                     "FROM games " +
                     "WHERE sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND commence_time > NOW() " +
                     "ORDER BY nfl_week";
        return jdbcTemplate.queryForList(sql, Integer.class);
    }

    public List<Map<String, Object>> getNflGamesByWeek(int week) {
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, o.home_odds, o.away_odds " +
                     "FROM games g " +
                     "JOIN odds o ON g.id = o.game_id " +
                     "WHERE g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND g.nfl_week = ? " +
                     "AND g.commence_time > NOW() - INTERVAL '5 hours' " +  // Adjust for EST
                     "ORDER BY g.commence_time";
        return jdbcTemplate.queryForList(sql, week);
    }

    public List<Map<String, Object>> getTeamSchedule(String team) {
        // Remove spaces from the input team name
        String formattedTeam = team.replaceAll("\\s+", "");
        
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, g.nfl_week, o.home_odds, o.away_odds " +
                     "FROM games g " +
                     "JOIN odds o ON g.id = o.game_id " +
                     "WHERE g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND (REPLACE(g.home_team, ' ', '') = ? OR REPLACE(g.away_team, ' ', '') = ?) " +
                     "AND g.commence_time > NOW() - INTERVAL '5 hours' " + // Adjust for EST
                     "ORDER BY g.commence_time";
        return jdbcTemplate.queryForList(sql, formattedTeam, formattedTeam);
    }

    public Map<String, Object> getGameById(int gameId) {
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, g.nfl_week, o.home_odds, o.away_odds " +
                     "FROM games g " +
                     "JOIN odds o ON g.id = o.game_id " +
                     "WHERE g.id = ? " +
                     "AND g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl')";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, gameId);
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public void updateGameResult(int gameId, String winner) {
        if (!winner.equals("home") && !winner.equals("away")) {
            throw new IllegalArgumentException("Winner must be either 'home' or 'away'");
        }

        String sql = "UPDATE games SET result = ? WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql, winner, gameId);
        
        if (updatedRows == 0) {
            throw new IllegalArgumentException("No game found with id: " + gameId);
        }
        
        // After updating the game result, settle the bets
        betService.settleBets(gameId, winner);
    }

    public List<Map<String, Object>> getGamesWithPendingResults() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String sql = "SELECT g.id, g.home_team, g.away_team, g.commence_time, g.nfl_week " +
                     "FROM games g " +
                     "WHERE g.sport_id = (SELECT id FROM sports WHERE key = 'americanfootball_nfl') " +
                     "AND g.result IS NULL " +
                     "AND g.commence_time < ? " +
                     "ORDER BY g.commence_time";
        return jdbcTemplate.queryForList(sql, Timestamp.from(now.toInstant()));
    }

}

```

## File: src/main/java/com/dialodds/dialodds_api/service/OddsPopulationService.java

```java
package com.dialodds.dialodds_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OddsPopulationService {

    private static final Logger logger = LoggerFactory.getLogger(OddsPopulationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${API_KEY}")
    private String apiKey;

    @Value("${API_BOOKMAKER}")
    private String bookmakerKey;

    @Value("${API_TARGETED_SPORTS}")
    private String targetedSportsString;

    private List<String> targetedSports;

    @PostConstruct
    public void init() {
        targetedSports = Arrays.asList(targetedSportsString.split(","));
        logger.info("Initialized targeted sports: {}", targetedSports);
        logger.info("API Key: {}", apiKey.substring(0, 4) + "..."); // Log only first 4 characters of API key
        logger.info("Bookmaker Key: {}", bookmakerKey);
    }

    @Transactional
    public void populateDatabase() {
        logger.info("Starting database population");
        logger.info("Targeted sports: {}", targetedSports);

        if (targetedSports == null || targetedSports.isEmpty()) {
            logger.error("No targeted sports configured. Check API_TARGETED_SPORTS environment variable.");
            return;
        }

        for (int i = 0; i < targetedSports.size(); i += 2) {
            if (i + 1 >= targetedSports.size()) {
                logger.error("Incomplete sport configuration at index {}. Skipping.", i);
                continue;
            }

            String sportKey = targetedSports.get(i);
            String marketType = targetedSports.get(i + 1);
            
            logger.info("Processing sport: {}, market type: {}", sportKey, marketType);
            
            try {
                List<Map<String, Object>> data = fetchOdds(sportKey, marketType);
                
                if (data.isEmpty()) {
                    logger.warn("No data available for {}", sportKey);
                    continue;
                }
                
                int sportId = insertSport(sportKey, (String) data.get(0).get("sport_title"));
                
                if ("h2h".equals(marketType)) {
                    processH2hMarket(sportId, data);
                } else if ("outrights".equals(marketType)) {
                    processOutrightMarket(sportId, data);
                } else {
                    logger.warn("Unsupported market type: {} for sport: {}", marketType, sportKey);
                }
                
                logger.info("Successfully updated odds for {}", sportKey);
            } catch (Exception e) {
                logger.error("Error processing sport: {}, market type: {}", sportKey, marketType, e);
            }
            
            try {
                Thread.sleep(1000); // Be nice to the API
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted", e);
            }
        }
        
        logger.info("Database update completed!");
    }

    private List<Map<String, Object>> fetchOdds(String sportKey, String market) {
        String url = "https://api.the-odds-api.com/v4/sports/" + sportKey + "/odds";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("apiKey", apiKey)
            .queryParam("regions", "us")
            .queryParam("markets", market)
            .queryParam("oddsFormat", "decimal")
            .queryParam("bookmakers", bookmakerKey);

        logger.info("Fetching odds from URL: {}", builder.toUriString().replaceAll("apiKey=[^&]+", "apiKey=*****"));

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        return response.getBody();
    }

    private int insertSport(String sportKey, String sportTitle) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO sports (key, name) VALUES (?, ?) " +
            "ON CONFLICT (key) DO UPDATE SET name = EXCLUDED.name " +
            "RETURNING id",
            Integer.class,
            sportKey, sportTitle
        );
    }

    private void processH2hMarket(int sportId, List<Map<String, Object>> data) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));
        for (Map<String, Object> event : data) {
            LocalDateTime commenceTime = LocalDateTime.parse((String) event.get("commence_time"), DateTimeFormatter.ISO_DATE_TIME);
            if (commenceTime.isBefore(now)) {
                logger.info("Skipping game {} as it has already started", event.get("id"));
                continue;
            }

            int gameId = insertGame(sportId, event);
            List<Map<String, Object>> bookmakers = getTypeSafeList(event.get("bookmakers"));
            for (Map<String, Object> bookmaker : bookmakers) {
                if (bookmakerKey.equals(bookmaker.get("key"))) {
                    List<Map<String, Object>> markets = getTypeSafeList(bookmaker.get("markets"));
                    for (Map<String, Object> market : markets) {
                        if ("h2h".equals(market.get("key"))) {
                            List<Map<String, Object>> outcomes = getTypeSafeList(market.get("outcomes"));
                            double homeOdds = outcomes.stream()
                                .filter(o -> o.get("name").equals(event.get("home_team")))
                                .findFirst()
                                .map(o -> (Double) o.get("price"))
                                .orElse(0.0);
                            double awayOdds = outcomes.stream()
                                .filter(o -> o.get("name").equals(event.get("away_team")))
                                .findFirst()
                                .map(o -> (Double) o.get("price"))
                                .orElse(0.0);
                            insertOdds(gameId, "h2h", homeOdds, awayOdds);
                        }
                    }
                    break; // We only need specified bookmaker odds
                }
            }
        }
    }

    private int insertGame(int sportId, Map<String, Object> event) {
        LocalDateTime commenceTime = LocalDateTime.parse((String) event.get("commence_time"), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime easternTime = commenceTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDateTime();
        int nflWeek = getNflWeek(easternTime);

        return jdbcTemplate.queryForObject(
            "INSERT INTO games (sport_id, home_team, away_team, commence_time, nfl_week) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (sport_id, home_team, away_team, commence_time) " +
            "DO UPDATE SET updated_at = CURRENT_TIMESTAMP, nfl_week = EXCLUDED.nfl_week " +
            "RETURNING id",
            Integer.class,
            sportId, event.get("home_team"), event.get("away_team"), easternTime, nflWeek
        );
    }

    private void insertOdds(int gameId, String marketType, double homeOdds, double awayOdds) {
        jdbcTemplate.update(
            "INSERT INTO odds (game_id, market_type, home_odds, away_odds) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (game_id, market_type) " +
            "DO UPDATE SET home_odds = EXCLUDED.home_odds, away_odds = EXCLUDED.away_odds, updated_at = CURRENT_TIMESTAMP",
            gameId, marketType, homeOdds, awayOdds
        );
    }

    private void processOutrightMarket(int sportId, List<Map<String, Object>> data) {
        for (Map<String, Object> event : data) {
            List<Map<String, Object>> bookmakers = getTypeSafeList(event.get("bookmakers"));
            for (Map<String, Object> bookmaker : bookmakers) {
                if (bookmakerKey.equals(bookmaker.get("key"))) {
                    List<Map<String, Object>> markets = getTypeSafeList(bookmaker.get("markets"));
                    for (Map<String, Object> market : markets) {
                        if ("outrights".equals(market.get("key"))) {
                            List<Map<String, Object>> outcomes = getTypeSafeList(market.get("outcomes"));
                            for (Map<String, Object> outcome : outcomes) {
                                insertOutrightOdds(sportId, (String) outcome.get("name"), (Double) outcome.get("price"));
                            }
                        }
                    }
                    break; // We only need specified bookmaker odds
                }
            }
        }
    }

    private void insertOutrightOdds(int sportId, String outcomeName, double price) {
        jdbcTemplate.update(
            "INSERT INTO outright_odds (sport_id, outcome_name, price) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT (sport_id, outcome_name) " +
            "DO UPDATE SET price = EXCLUDED.price, updated_at = CURRENT_TIMESTAMP",
            sportId, outcomeName, price
        );
    }

    private int getNflWeek(LocalDateTime gameDate) {
        LocalDateTime seasonStart = getNflSeasonStart(gameDate.getYear());
        long daysSinceStart = gameDate.toLocalDate().toEpochDay() - seasonStart.toLocalDate().toEpochDay();
        return Math.max(1, (int) (daysSinceStart / 7) + 1);
    }

    private LocalDateTime getNflSeasonStart(int year) {
        // Adjust this method based on the actual NFL season start dates
        if (year == 2024) {
            return LocalDateTime.of(2024, 9, 5, 0, 0);
        }
        return LocalDateTime.of(year, 9, 5, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getTypeSafeList(Object obj) {
        if (obj instanceof List<?>) {
            return (List<T>) obj;
        }
        return new ArrayList<>();
    }

    @Transactional
    public String manuallyPopulateDatabase() {
        try {
            logger.info("Starting manual database population");
            populateDatabase();
            return "Database manually updated successfully";
        } catch (Exception e) {
            logger.error("Error during manual database population", e);
            return "Error updating database: " + e.getMessage();
        }
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/service/SeasonService.java

```java
package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Service
public class SeasonService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int createSeason(int startWeek, int endWeek, int initialCoins) {
        String sql = "INSERT INTO seasons (start_week, end_week, initial_coins, created_at) VALUES (?, ?, ?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(sql, Integer.class, startWeek, endWeek, initialCoins, LocalDateTime.now());
    }

    public List<Map<String, Object>> getAllSeasons() {
        String sql = "SELECT * FROM seasons";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getActiveSeasons() {
        String sql = "SELECT * FROM seasons WHERE end_week >= (SELECT MAX(nfl_week) FROM games WHERE commence_time > NOW())";
        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> getSeasonById(int seasonId) {
        String sql = "SELECT * FROM seasons WHERE id = ?";
        return jdbcTemplate.queryForMap(sql, seasonId);
    }

    @Transactional
    public Map<String, Object> deleteSeason(int seasonId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // First, delete related records in bets
            String deleteBetsSQL = "DELETE FROM bets WHERE season_id = ?";
            int betsDeleted = jdbcTemplate.update(deleteBetsSQL, seasonId);
            
            // Then, delete related records in user_seasons
            String deleteUserSeasonsSQL = "DELETE FROM user_seasons WHERE season_id = ?";
            int userSeasonsDeleted = jdbcTemplate.update(deleteUserSeasonsSQL, seasonId);
            
            // Finally, delete the season
            String deleteSeasonSQL = "DELETE FROM seasons WHERE id = ?";
            int rowsAffected = jdbcTemplate.update(deleteSeasonSQL, seasonId);
            
            if (rowsAffected > 0) {
                result.put("deleted", true);
                result.put("message", String.format("Season successfully deleted. %d bets and %d user season entries were also removed.", betsDeleted, userSeasonsDeleted));
            } else {
                result.put("deleted", false);
                result.put("message", "Season not found or already deleted.");
            }
        } catch (Exception e) {
            result.put("deleted", false);
            result.put("message", "Error deleting season: " + e.getMessage());
        }
        
        return result;
    }
}
```

## File: src/main/java/com/dialodds/dialodds_api/service/UserService.java

```java
package com.dialodds.dialodds_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public int createOrUpdateUser(String discordId, String username) {
        String sql = "INSERT INTO users (discord_id, username) VALUES (?, ?) " +
                "ON CONFLICT (discord_id) DO UPDATE SET username = EXCLUDED.username " +
                "RETURNING id";
        return jdbcTemplate.queryForObject(sql, Integer.class, discordId, username);
    }

    public void addUserToSeason(int userId, int seasonId) {
        String sql = "INSERT INTO user_seasons (user_id, season_id, coins) VALUES (?, ?, (SELECT initial_coins FROM seasons WHERE id = ?))";
        jdbcTemplate.update(sql, userId, seasonId, seasonId);
    }

    public List<Map<String, Object>> getUsersBySeason(int seasonId) {
        String sql = "SELECT u.id, u.username, u.discord_id, us.coins " +
                     "FROM users u " +
                     "JOIN user_seasons us ON u.id = us.user_id " +
                     "WHERE us.season_id = ?";
        return jdbcTemplate.queryForList(sql, seasonId);
    }

    public int getUserCoins(int userId, int seasonId) {
        String sql = "SELECT coins FROM user_seasons WHERE user_id = ? AND season_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId, seasonId);
    }

    @Transactional
    public void createUserAndJoinSeason(String discordId, String username, int seasonId) {
        int userId = createOrUpdateUser(discordId, username);
        addUserToSeason(userId, seasonId);
    }
}

```

## File: src/main/resources/static/index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NFL API Home</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
        }
        h1 {
            color: #2c3e50;
        }
        .endpoint {
            background-color: #f4f4f4;
            border: 1px solid #ddd;
            border-radius: 4px;
            padding: 10px;
            margin-bottom: 10px;
        }
        .method {
            font-weight: bold;
            color: #2980b9;
        }
        .path {
            color: #27ae60;
        }
    </style>
</head>
<body>
    <h1>Welcome to the NFL API</h1>
    <p>Here are the available endpoints:</p>
    
    <div class="endpoint">
        <span class="method">GET</span> 
        <span class="path">/api/nfl/weeks</span>
        <p>Retrieves a list of available NFL weeks.</p>
    </div>
    
    <div class="endpoint">
        <span class="method">GET</span> 
        <span class="path">/api/nfl/games/{week}</span>
        <p>Retrieves NFL games for a specific week. Replace {week} with the desired week number.</p>
    </div>
    
    <p>To use these endpoints, append them to the base URL: <code>http://localhost:8080</code></p>
</body>
</html>
```

## File: src/main/resources/static/ts/dashboard.ts

```typescript
interface Season {
    id: number;
    start_week: number;
    end_week: number;
    initial_coins: number;
}

interface User {
    id: number;
    username: string;
    coins: number;
}

interface Game {
    id: number;
    home_team: string;
    away_team: string;
    nfl_week: number;
    commence_time: string;
    winner?: 'home' | 'away';
}

interface Bet {
    id: number;
    username: string;
    season_id: number;
    bet_type: string;
    amount: number;
}

interface NewSeason {
    startWeek: number;
    endWeek: number;
    initialCoins: number;
}

function dashboardData() {
    return {
        seasons: [] as Season[],
        users: [] as User[],
        pendingGames: [] as Game[],
        unsettledBets: [] as Bet[],
        selectedSeason: '',
        selectedGameId: '',
        updateResult: '',
        errorMessage: '',
        newSeason: {
            startWeek: 0,
            endWeek: 0,
            initialCoins: 0
        } as NewSeason,

        init(): void {
            console.log('Initializing dashboard...');
            this.loadSeasons();
        },

        async fetchWithErrorHandling(url: string, options: RequestInit = {}): Promise<any> {
            try {
                const response = await fetch(url, options);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return await response.json();
            } catch (error) {
                console.error('Fetch error:', error);
                this.errorMessage = `Error: ${error.message}`;
                throw error;
            }
        },

        async loadSeasons(): Promise<void> {
            console.log('Loading seasons...');
            try {
                this.seasons = await this.fetchWithErrorHandling('/api/seasons');
                console.log('Seasons loaded:', this.seasons);
            } catch (error) {
                console.error('Error loading seasons:', error);
            }
        },

        async createSeason(): Promise<void> {
            console.log('Creating season:', this.newSeason);
            try {
                await this.fetchWithErrorHandling('/api/seasons', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(this.newSeason)
                });
                console.log('Season created successfully');
                await this.loadSeasons();
                this.newSeason = { startWeek: 0, endWeek: 0, initialCoins: 0 };
            } catch (error) {
                console.error('Error creating season:', error);
            }
        },

        async deleteSeason(seasonId: number): Promise<void> {
            console.log('Deleting season:', seasonId);
            try {
                await this.fetchWithErrorHandling(`/api/seasons/${seasonId}`, { method: 'DELETE' });
                console.log('Season deleted successfully');
                await this.loadSeasons();
            } catch (error) {
                console.error('Error deleting season:', error);
            }
        },

        async loadUsers(): Promise<void> {
            if (this.selectedSeason) {
                console.log('Loading users for season:', this.selectedSeason);
                try {
                    this.users = await this.fetchWithErrorHandling(`/api/users/seasons/${this.selectedSeason}`);
                    console.log('Users loaded:', this.users);
                } catch (error) {
                    console.error('Error loading users:', error);
                }
            }
        },

        async loadPendingGames(): Promise<void> {
            console.log('Loading pending games...');
            try {
                this.pendingGames = await this.fetchWithErrorHandling('/api/nfl/games/pending-results');
                console.log('Pending games loaded:', this.pendingGames);
            } catch (error) {
                console.error('Error loading pending games:', error);
            }
        },

        async updateGameResult(gameId: number, winner: 'home' | 'away'): Promise<void> {
            console.log('Updating game result:', gameId, winner);
            try {
                await this.fetchWithErrorHandling(`/api/nfl/games/${gameId}/result`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: `winner=${winner}`
                });
                console.log('Game result updated successfully');
                await this.loadPendingGames();
            } catch (error) {
                console.error('Error updating game result:', error);
            }
        },

        async loadUnsettledBets(): Promise<void> {
            if (this.selectedGameId) {
                console.log('Loading unsettled bets for game:', this.selectedGameId);
                try {
                    this.unsettledBets = await this.fetchWithErrorHandling(`/api/bets/unsettled/${this.selectedGameId}`);
                    console.log('Unsettled bets loaded:', this.unsettledBets);
                } catch (error) {
                    console.error('Error loading unsettled bets:', error);
                }
            }
        },

        async updateDatabase(): Promise<void> {
            console.log('Updating database...');
            try {
                const response = await fetch('/api/admin/update-database', { method: 'POST' });
                this.updateResult = await response.text();
                console.log('Database update result:', this.updateResult);
            } catch (error) {
                console.error('Error updating database:', error);
                this.updateResult = 'Error updating database';
            }
        },

        formatDate(dateString: string): string {
            return new Date(dateString).toLocaleString();
        }
    };
}
```

## File: src/main/resources/templates/admin-dashboard.html

```html
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DialOdds Admin Dashboard</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
</head>

<body class="bg-gray-900 text-gray-100">
    <div x-data="dashboardData()" x-init="init()" class="container mx-auto px-4 py-8">
        <h1 class="text-4xl font-bold mb-8 text-center text-indigo-400">DialOdds Admin Dashboard</h1>

        <div x-show="errorMessage" x-text="errorMessage" class="bg-red-600 text-white p-4 rounded mb-4"></div>

        <!-- Tabs -->
        <div class="mb-4">
            <nav class="flex space-x-4">
                <button @click="activeTab = 'seasons'"
                    :class="{'bg-blue-600': activeTab === 'seasons', 'bg-gray-700': activeTab !== 'seasons'}"
                    class="px-4 py-2 rounded-t-lg">Seasons</button>
                <button @click="activeTab = 'users'"
                    :class="{'bg-blue-600': activeTab === 'users', 'bg-gray-700': activeTab !== 'users'}"
                    class="px-4 py-2 rounded-t-lg">Users</button>
                <button @click="activeTab = 'games'"
                    :class="{'bg-blue-600': activeTab === 'games', 'bg-gray-700': activeTab !== 'games'}"
                    class="px-4 py-2 rounded-t-lg">Games</button>
                <button @click="activeTab = 'bets'"
                    :class="{'bg-blue-600': activeTab === 'bets', 'bg-gray-700': activeTab !== 'bets'}"
                    class="px-4 py-2 rounded-t-lg">Bets</button>
                <button @click="activeTab = 'database'"
                    :class="{'bg-blue-600': activeTab === 'database', 'bg-gray-700': activeTab !== 'database'}"
                    class="px-4 py-2 rounded-t-lg">Database</button>
            </nav>
        </div>

        <!-- Tab Content -->
        <div class="bg-gray-800 p-6 rounded-lg shadow-lg">
            <!-- Seasons Tab -->
            <div x-show="activeTab === 'seasons'">
                <h2 class="text-2xl font-semibold mb-4">Seasons Management</h2>
                <button @click="loadSeasons()" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 mb-4">
                    Load Seasons
                </button>
                <div x-show="seasons.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">Start Week</th>
                                <th class="px-4 py-2 text-left">End Week</th>
                                <th class="px-4 py-2 text-left">Initial Coins</th>
                                <th class="px-4 py-2 text-left">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="season in seasons" :key="season.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="season.id"></td>
                                    <td class="px-4 py-2" x-text="season.start_week"></td>
                                    <td class="px-4 py-2" x-text="season.end_week"></td>
                                    <td class="px-4 py-2" x-text="season.initial_coins"></td>
                                    <td class="px-4 py-2">
                                        <button @click="deleteSeason(season.id)"
                                            class="bg-red-600 text-white px-2 py-1 rounded hover:bg-red-700">
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
                <form @submit.prevent="createSeason" class="mt-4">
                    <input type="number" x-model="newSeason.startWeek" placeholder="Start Week"
                        class="bg-gray-700 text-white p-2 rounded w-full mb-2">
                    <input type="number" x-model="newSeason.endWeek" placeholder="End Week"
                        class="bg-gray-700 text-white p-2 rounded w-full mb-2">
                    <input type="number" x-model="newSeason.initialCoins" placeholder="Initial Coins"
                        class="bg-gray-700 text-white p-2 rounded w-full mb-2">
                    <button type="submit"
                        class="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 w-full">Create
                        Season</button>
                </form>
            </div>

            <!-- Users Tab -->
            <div x-show="activeTab === 'users'">
                <h2 class="text-2xl font-semibold mb-4">User Management</h2>
                <select x-model="selectedSeason" @change="loadUsers()"
                    class="bg-gray-700 text-white p-2 rounded w-full mb-4">
                    <option value="">Select a Season</option>
                    <template x-for="season in seasons" :key="season.id">
                        <option :value="season.id" x-text="`Season ${season.id}`"></option>
                    </template>
                </select>
                <div x-show="users.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">Username</th>
                                <th class="px-4 py-2 text-left">Coins</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="user in users" :key="user.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="user.id"></td>
                                    <td class="px-4 py-2" x-text="user.username"></td>
                                    <td class="px-4 py-2" x-text="user.coins"></td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Games Tab -->
            <div x-show="activeTab === 'games'">
                <h2 class="text-2xl font-semibold mb-4">Games Management</h2>
                <button @click="loadPendingGames()"
                    class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 mb-4">
                    Load Pending Games
                </button>
                <div x-show="pendingGames.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">Home Team</th>
                                <th class="px-4 py-2 text-left">Away Team</th>
                                <th class="px-4 py-2 text-left">Week</th>
                                <th class="px-4 py-2 text-left">Date</th>
                                <th class="px-4 py-2 text-left">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="game in pendingGames" :key="game.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="game.id"></td>
                                    <td class="px-4 py-2" x-text="game.home_team"></td>
                                    <td class="px-4 py-2" x-text="game.away_team"></td>
                                    <td class="px-4 py-2" x-text="game.nfl_week"></td>
                                    <td class="px-4 py-2" x-text="formatDate(game.commence_time)"></td>
                                    <td class="px-4 py-2">
                                        <select x-model="game.winner" class="bg-gray-600 text-white p-1 rounded">
                                            <option value="">Select winner</option>
                                            <option value="home" x-text="game.home_team"></option>
                                            <option value="away" x-text="game.away_team"></option>
                                        </select>
                                        <button @click="updateGameResult(game.id, game.winner)"
                                            class="bg-green-600 text-white px-2 py-1 rounded hover:bg-green-700 ml-2">
                                            Update
                                        </button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Bets Tab -->
            <div x-show="activeTab === 'bets'">
                <h2 class="text-2xl font-semibold mb-4">Bets Management</h2>
                <select x-model="selectedSeasonForBets" @change="loadBetsForSeason()"
                    class="bg-gray-700 text-white p-2 rounded w-full mb-4">
                    <option value="">Select a Season</option>
                    <template x-for="season in seasons" :key="season.id">
                        <option :value="season.id" x-text="`Season ${season.id}`"></option>
                    </template>
                </select>

                <!-- Add the Audit and Correct All Bets button here -->
                <button @click="auditAndCorrectAllBets"
                    class="bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700 mb-4">
                    Audit and Correct All Bets
                </button>

                <div x-show="bets.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">User</th>
                                <th class="px-4 py-2 text-left">Game</th>
                                <th class="px-4 py-2 text-left">Bet On Team</th>
                                <th class="px-4 py-2 text-left">Amount</th>
                                <th class="px-4 py-2 text-left">Result</th>
                                <th class="px-4 py-2 text-left">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="bet in bets" :key="bet.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="bet.id"></td>
                                    <td class="px-4 py-2" x-text="bet.username"></td>
                                    <td class="px-4 py-2" x-text="`${bet.home_team} vs ${bet.away_team}`"></td>
                                    <td class="px-4 py-2" x-text="bet.bet_on_team"></td>
                                    <td class="px-4 py-2" x-text="bet.amount"></td>
                                    <td class="px-4 py-2" x-text="bet.result || 'Pending'"></td>
                                    <td class="px-4 py-2">
                                        <select x-model="bet.newResult" class="bg-gray-600 text-white p-1 rounded">
                                            <option value="">Select result</option>
                                            <option value="won">Won</option>
                                            <option value="lost">Lost</option>
                                        </select>
                                        <button @click="correctBetResult(bet.id, bet.newResult)"
                                            class="bg-yellow-600 text-white px-2 py-1 rounded hover:bg-yellow-700 ml-2">
                                            Correct
                                        </button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Database Tab -->
            <div x-show="activeTab === 'database'">
                <h2 class="text-2xl font-semibold mb-4">Database Update</h2>
                <button @click="updateDatabase()"
                    class="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 w-full">
                    Manual Database Update
                </button>
                <div x-show="updateResult" x-text="updateResult" class="mt-4 p-2 rounded"
                    :class="{'bg-green-600': updateResult.includes('success'), 'bg-red-600': !updateResult.includes('success')}">
                </div>
            </div>
        </div>
    </div>

    <script>
        function dashboardData() {
            return {
                activeTab: 'seasons',
                seasons: [],
                users: [],
                pendingGames: [],
                bets: [],
                selectedSeason: '',
                selectedSeasonForBets: '',
                updateResult: '',
                errorMessage: '',
                newSeason: {
                    startWeek: '',
                    endWeek: '',
                    initialCoins: ''
                },

                init() {
                    console.log('Initializing dashboard...');
                    this.loadSeasons();
                },

                async fetchWithErrorHandling(url, options = {}) {
                    try {
                        const response = await fetch(url, options);
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        return await response.json();
                    } catch (error) {
                        console.error('Fetch error:', error);
                        this.errorMessage = `Error: ${error.message}`;
                        throw error;
                    }
                },

                async loadSeasons() {
                    console.log('Loading seasons...');
                    try {
                        this.seasons = await this.fetchWithErrorHandling('/api/seasons');
                        console.log('Seasons loaded:', this.seasons);
                    } catch (error) {
                        console.error('Error loading seasons:', error);
                    }
                },

                async createSeason() {
                    console.log('Creating season:', this.newSeason);
                    try {
                        const formData = new URLSearchParams();
                        formData.append('startWeek', this.newSeason.startWeek);
                        formData.append('endWeek', this.newSeason.endWeek);
                        formData.append('initialCoins', this.newSeason.initialCoins);

                        await this.fetchWithErrorHandling('/api/seasons', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: formData
                        });
                        console.log('Season created successfully');
                        await this.loadSeasons();
                        this.newSeason = { startWeek: '', endWeek: '', initialCoins: '' };
                    } catch (error) {
                        console.error('Error creating season:', error);
                    }
                },

                async deleteSeason(seasonId) {
                    console.log('Deleting season:', seasonId);
                    try {
                        await this.fetchWithErrorHandling(`/api/seasons/${seasonId}`, { method: 'DELETE' });
                        console.log('Season deleted successfully');
                        await this.loadSeasons();
                    } catch (error) {
                        console.error('Error deleting season:', error);
                    }
                },

                async loadUsers() {
                    if (this.selectedSeason) {
                        console.log('Loading users for season:', this.selectedSeason);
                        try {
                            this.users = await this.fetchWithErrorHandling(`/api/users/seasons/${this.selectedSeason}`);
                            console.log('Users loaded:', this.users);
                        } catch (error) {
                            console.error('Error loading users:', error);
                        }
                    }
                },

                async loadPendingGames() {
                    console.log('Loading pending games...');
                    try {
                        this.pendingGames = await this.fetchWithErrorHandling('/api/nfl/games/pending-results');
                        console.log('Pending games loaded:', this.pendingGames);
                    } catch (error) {
                        console.error('Error loading pending games:', error);
                    }
                },

                async updateGameResult(gameId, winner) {
                    console.log('Updating game result:', gameId, winner);
                    if (!winner) {
                        this.errorMessage = "Please select a winner before updating the game result.";
                        return;
                    }
                    try {
                        const response = await this.fetchWithErrorHandling(`/api/nfl/games/${gameId}/result`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: `winner=${winner}`
                        });
                        console.log('Game result updated successfully');
                        this.errorMessage = "Game result updated successfully and bets settled.";
                        await this.loadPendingGames();
                    } catch (error) {
                        console.error('Error updating game result:', error);
                        this.errorMessage = `Error updating game result: ${error.message}`;
                    }
                },

                formatDate(dateString) {
                    return new Date(dateString).toLocaleString();
                },

                async loadUnsettledBets() {
                    if (this.selectedGameId) {
                        console.log('Loading unsettled bets for game:', this.selectedGameId);
                        try {
                            this.unsettledBets = await this.fetchWithErrorHandling(`/api/bets/unsettled/${this.selectedGameId}`);
                            console.log('Unsettled bets loaded:', this.unsettledBets);
                        } catch (error) {
                            console.error('Error loading unsettled bets:', error);
                        }
                    }
                },

                async loadBetsForSeason() {
                    if (this.selectedSeasonForBets) {
                        console.log('Loading bets for season:', this.selectedSeasonForBets);
                        try {
                            this.bets = await this.fetchWithErrorHandling(`/api/bets/seasons/${this.selectedSeasonForBets}`);
                            console.log('Bets loaded:', this.bets);
                        } catch (error) {
                            console.error('Error loading bets:', error);
                            this.errorMessage = `Error loading bets: ${error.message}`;
                        }
                    }
                },

                async correctBetResult(betId, newResult) {
                    console.log('Correcting bet result:', betId, newResult);
                    if (!newResult) {
                        this.errorMessage = "Please select a new result before correcting the bet.";
                        return;
                    }
                    try {
                        const response = await this.fetchWithErrorHandling('/api/bets/correct-bet', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: `betId=${betId}&newResult=${newResult}`
                        });
                        console.log('Bet corrected successfully');
                        this.errorMessage = "Bet corrected successfully. Refreshing bets list.";
                        await this.loadBetsForSeason();
                    } catch (error) {
                        console.error('Error correcting bet:', error);
                        this.errorMessage = `Error correcting bet: ${error.message}`;
                    }
                },

                async auditAndCorrectAllBets() {
                    console.log('Auditing and correcting all bets...');
                    try {
                        const response = await this.fetchWithErrorHandling('/api/bets/audit-and-correct', {
                            method: 'POST'
                        });
                        console.log('Bets audited and corrected successfully');
                        this.errorMessage = response; // This will now contain the text response from the server
                        await this.loadBetsForSeason();
                    } catch (error) {
                        console.error('Error auditing and correcting bets:', error);
                        this.errorMessage = `Error auditing and correcting bets: ${error.message}`;
                    }
                },

                async updateDatabase() {
                    console.log('Updating database...');
                    try {
                        const response = await fetch('/api/admin/update-database', { method: 'POST' });
                        this.updateResult = await response.text();
                        console.log('Database update result:', this.updateResult);
                    } catch (error) {
                        console.error('Error updating database:', error);
                        this.updateResult = 'Error updating database';
                    }
                },

                formatDate(dateString) {
                    return new Date(dateString).toLocaleString();
                },

                async fetchWithErrorHandling(url, options = {}) {
                    try {
                        const response = await fetch(url, options);
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        const contentType = response.headers.get("content-type");
                        if (contentType && contentType.includes("application/json")) {
                            return await response.json();
                        } else {
                            return await response.text();
                        }
                    } catch (error) {
                        console.error('Fetch error:', error);
                        this.errorMessage = `Error: ${error.message}`;
                        throw error;
                    }
                }
            };
        }
    </script>
</body>

</html>
```

## File: src/main/resources/templates/login.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900 text-gray-100 flex items-center justify-center h-screen">
    <div class="bg-gray-800 p-8 rounded-lg shadow-lg w-96">
        <h1 class="text-2xl font-bold mb-6 text-center text-indigo-400">Login</h1>
        <form th:action="@{/login}" method="post" class="space-y-4">
            <div>
                <label for="username" class="block text-sm font-medium text-gray-300">Username</label>
                <input type="text" id="username" name="username" required class="mt-1 block w-full rounded-md bg-gray-700 border-gray-600 text-gray-100 focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50">
            </div>
            <div>
                <label for="password" class="block text-sm font-medium text-gray-300">Password</label>
                <input type="password" id="password" name="password" required class="mt-1 block w-full rounded-md bg-gray-700 border-gray-600 text-gray-100 focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50">
            </div>
            <div>
            </div>
            <div class="flex items-center">
                <input type="checkbox" id="remember-me" name="remember-me" class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                <label for="remember-me" class="ml-2 block text-sm text-gray-300">
                    Remember me
                </label>
            </div>
            <button type="submit" class="w-full bg-indigo-600 text-white rounded-md py-2 px-4 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-gray-800">Sign In</button>
        </form>
    </div>
</body>
</html>
```

## File: src/main/resources/templates/pending-games.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pending Game Results</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900 text-gray-100 p-8">
    <div class="max-w-7xl mx-auto">
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-3xl font-bold text-indigo-400">Pending Game Results</h1>
            <button id="manualUpdateBtn" class="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
                Manual Database Update
            </button>
        </div>
        <div id="updateMessage" class="mb-4 p-4 rounded hidden"></div>
        <div class="bg-gray-800 shadow-md rounded-lg overflow-hidden">
            <table class="min-w-full divide-y divide-gray-700">
                <thead class="bg-gray-700">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">ID</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Home Team</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Away Team</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Week</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Date</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Action</th>
                    </tr>
                </thead>
                <tbody class="bg-gray-800 divide-y divide-gray-700">
                    <tr th:each="game : ${games}">
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.id}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.home_team}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.away_team}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.nfl_week}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${#dates.format(game.commence_time, 'yyyy-MM-dd HH:mm')}"></td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <form th:action="@{/admin/update-result}" method="post" class="flex items-center space-x-2">
                                <input type="hidden" name="gameId" th:value="${game.id}" />
                                <select name="winner" required class="block w-full mt-1 rounded-md bg-gray-700 border-gray-600 text-gray-100 focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50">
                                    <option value="">Select winner</option>
                                    <option value="home" th:text="${game.home_team}"></option>
                                    <option value="away" th:text="${game.away_team}"></option>
                                </select>
                                <button type="submit" class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-indigo-500">
                                    Update Result
                                </button>
                            </form>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <script>
        document.getElementById('manualUpdateBtn').addEventListener('click', function() {
            fetch('/api/admin/update-database', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            })
            .then(response => response.text())
            .then(data => {
                const messageDiv = document.getElementById('updateMessage');
                messageDiv.textContent = data;
                messageDiv.classList.remove('hidden', 'bg-red-600', 'bg-green-600');
                messageDiv.classList.add(data.includes('successfully') ? 'bg-green-600' : 'bg-red-600');
                setTimeout(() => {
                    messageDiv.classList.add('hidden');
                }, 5000);
            })
            .catch((error) => {
                console.error('Error:', error);
                const messageDiv = document.getElementById('updateMessage');
                messageDiv.textContent = 'An error occurred while updating the database.';
                messageDiv.classList.remove('hidden', 'bg-green-600');
                messageDiv.classList.add('bg-red-600');
                setTimeout(() => {
                    messageDiv.classList.add('hidden');
                }, 5000);
            });
        });
    </script>
</body>
</html>
```

## File: src/test/java/com/dialodds/dialodds_api/DialoddsApiApplicationTests.java

```java
package com.dialodds.dialodds_api;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DialoddsApiApplicationTests {

    @BeforeAll
    static void setup() {
        Dotenv dotenv = Dotenv.configure()
                              .directory("./")
                              .ignoreIfMissing()
                              .load();

        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
    }

    @Test
    void contextLoads() {
    }
}
```

## File: target/classes/static/index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NFL API Home</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
        }
        h1 {
            color: #2c3e50;
        }
        .endpoint {
            background-color: #f4f4f4;
            border: 1px solid #ddd;
            border-radius: 4px;
            padding: 10px;
            margin-bottom: 10px;
        }
        .method {
            font-weight: bold;
            color: #2980b9;
        }
        .path {
            color: #27ae60;
        }
    </style>
</head>
<body>
    <h1>Welcome to the NFL API</h1>
    <p>Here are the available endpoints:</p>
    
    <div class="endpoint">
        <span class="method">GET</span> 
        <span class="path">/api/nfl/weeks</span>
        <p>Retrieves a list of available NFL weeks.</p>
    </div>
    
    <div class="endpoint">
        <span class="method">GET</span> 
        <span class="path">/api/nfl/games/{week}</span>
        <p>Retrieves NFL games for a specific week. Replace {week} with the desired week number.</p>
    </div>
    
    <p>To use these endpoints, append them to the base URL: <code>http://localhost:8080</code></p>
</body>
</html>
```

## File: target/classes/static/ts/dashboard.ts

```typescript
interface Season {
    id: number;
    start_week: number;
    end_week: number;
    initial_coins: number;
}

interface User {
    id: number;
    username: string;
    coins: number;
}

interface Game {
    id: number;
    home_team: string;
    away_team: string;
    nfl_week: number;
    commence_time: string;
    winner?: 'home' | 'away';
}

interface Bet {
    id: number;
    username: string;
    season_id: number;
    bet_type: string;
    amount: number;
}

interface NewSeason {
    startWeek: number;
    endWeek: number;
    initialCoins: number;
}

function dashboardData() {
    return {
        seasons: [] as Season[],
        users: [] as User[],
        pendingGames: [] as Game[],
        unsettledBets: [] as Bet[],
        selectedSeason: '',
        selectedGameId: '',
        updateResult: '',
        errorMessage: '',
        newSeason: {
            startWeek: 0,
            endWeek: 0,
            initialCoins: 0
        } as NewSeason,

        init(): void {
            console.log('Initializing dashboard...');
            this.loadSeasons();
        },

        async fetchWithErrorHandling(url: string, options: RequestInit = {}): Promise<any> {
            try {
                const response = await fetch(url, options);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return await response.json();
            } catch (error) {
                console.error('Fetch error:', error);
                this.errorMessage = `Error: ${error.message}`;
                throw error;
            }
        },

        async loadSeasons(): Promise<void> {
            console.log('Loading seasons...');
            try {
                this.seasons = await this.fetchWithErrorHandling('/api/seasons');
                console.log('Seasons loaded:', this.seasons);
            } catch (error) {
                console.error('Error loading seasons:', error);
            }
        },

        async createSeason(): Promise<void> {
            console.log('Creating season:', this.newSeason);
            try {
                await this.fetchWithErrorHandling('/api/seasons', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(this.newSeason)
                });
                console.log('Season created successfully');
                await this.loadSeasons();
                this.newSeason = { startWeek: 0, endWeek: 0, initialCoins: 0 };
            } catch (error) {
                console.error('Error creating season:', error);
            }
        },

        async deleteSeason(seasonId: number): Promise<void> {
            console.log('Deleting season:', seasonId);
            try {
                await this.fetchWithErrorHandling(`/api/seasons/${seasonId}`, { method: 'DELETE' });
                console.log('Season deleted successfully');
                await this.loadSeasons();
            } catch (error) {
                console.error('Error deleting season:', error);
            }
        },

        async loadUsers(): Promise<void> {
            if (this.selectedSeason) {
                console.log('Loading users for season:', this.selectedSeason);
                try {
                    this.users = await this.fetchWithErrorHandling(`/api/users/seasons/${this.selectedSeason}`);
                    console.log('Users loaded:', this.users);
                } catch (error) {
                    console.error('Error loading users:', error);
                }
            }
        },

        async loadPendingGames(): Promise<void> {
            console.log('Loading pending games...');
            try {
                this.pendingGames = await this.fetchWithErrorHandling('/api/nfl/games/pending-results');
                console.log('Pending games loaded:', this.pendingGames);
            } catch (error) {
                console.error('Error loading pending games:', error);
            }
        },

        async updateGameResult(gameId: number, winner: 'home' | 'away'): Promise<void> {
            console.log('Updating game result:', gameId, winner);
            try {
                await this.fetchWithErrorHandling(`/api/nfl/games/${gameId}/result`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: `winner=${winner}`
                });
                console.log('Game result updated successfully');
                await this.loadPendingGames();
            } catch (error) {
                console.error('Error updating game result:', error);
            }
        },

        async loadUnsettledBets(): Promise<void> {
            if (this.selectedGameId) {
                console.log('Loading unsettled bets for game:', this.selectedGameId);
                try {
                    this.unsettledBets = await this.fetchWithErrorHandling(`/api/bets/unsettled/${this.selectedGameId}`);
                    console.log('Unsettled bets loaded:', this.unsettledBets);
                } catch (error) {
                    console.error('Error loading unsettled bets:', error);
                }
            }
        },

        async updateDatabase(): Promise<void> {
            console.log('Updating database...');
            try {
                const response = await fetch('/api/admin/update-database', { method: 'POST' });
                this.updateResult = await response.text();
                console.log('Database update result:', this.updateResult);
            } catch (error) {
                console.error('Error updating database:', error);
                this.updateResult = 'Error updating database';
            }
        },

        formatDate(dateString: string): string {
            return new Date(dateString).toLocaleString();
        }
    };
}
```

## File: target/classes/templates/admin-dashboard.html

```html
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DialOdds Admin Dashboard</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
</head>

<body class="bg-gray-900 text-gray-100">
    <div x-data="dashboardData()" x-init="init()" class="container mx-auto px-4 py-8">
        <h1 class="text-4xl font-bold mb-8 text-center text-indigo-400">DialOdds Admin Dashboard</h1>

        <div x-show="errorMessage" x-text="errorMessage" class="bg-red-600 text-white p-4 rounded mb-4"></div>

        <!-- Tabs -->
        <div class="mb-4">
            <nav class="flex space-x-4">
                <button @click="activeTab = 'seasons'"
                    :class="{'bg-blue-600': activeTab === 'seasons', 'bg-gray-700': activeTab !== 'seasons'}"
                    class="px-4 py-2 rounded-t-lg">Seasons</button>
                <button @click="activeTab = 'users'"
                    :class="{'bg-blue-600': activeTab === 'users', 'bg-gray-700': activeTab !== 'users'}"
                    class="px-4 py-2 rounded-t-lg">Users</button>
                <button @click="activeTab = 'games'"
                    :class="{'bg-blue-600': activeTab === 'games', 'bg-gray-700': activeTab !== 'games'}"
                    class="px-4 py-2 rounded-t-lg">Games</button>
                <button @click="activeTab = 'bets'"
                    :class="{'bg-blue-600': activeTab === 'bets', 'bg-gray-700': activeTab !== 'bets'}"
                    class="px-4 py-2 rounded-t-lg">Bets</button>
                <button @click="activeTab = 'database'"
                    :class="{'bg-blue-600': activeTab === 'database', 'bg-gray-700': activeTab !== 'database'}"
                    class="px-4 py-2 rounded-t-lg">Database</button>
            </nav>
        </div>

        <!-- Tab Content -->
        <div class="bg-gray-800 p-6 rounded-lg shadow-lg">
            <!-- Seasons Tab -->
            <div x-show="activeTab === 'seasons'">
                <h2 class="text-2xl font-semibold mb-4">Seasons Management</h2>
                <button @click="loadSeasons()" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 mb-4">
                    Load Seasons
                </button>
                <div x-show="seasons.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">Start Week</th>
                                <th class="px-4 py-2 text-left">End Week</th>
                                <th class="px-4 py-2 text-left">Initial Coins</th>
                                <th class="px-4 py-2 text-left">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="season in seasons" :key="season.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="season.id"></td>
                                    <td class="px-4 py-2" x-text="season.start_week"></td>
                                    <td class="px-4 py-2" x-text="season.end_week"></td>
                                    <td class="px-4 py-2" x-text="season.initial_coins"></td>
                                    <td class="px-4 py-2">
                                        <button @click="deleteSeason(season.id)"
                                            class="bg-red-600 text-white px-2 py-1 rounded hover:bg-red-700">
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
                <form @submit.prevent="createSeason" class="mt-4">
                    <input type="number" x-model="newSeason.startWeek" placeholder="Start Week"
                        class="bg-gray-700 text-white p-2 rounded w-full mb-2">
                    <input type="number" x-model="newSeason.endWeek" placeholder="End Week"
                        class="bg-gray-700 text-white p-2 rounded w-full mb-2">
                    <input type="number" x-model="newSeason.initialCoins" placeholder="Initial Coins"
                        class="bg-gray-700 text-white p-2 rounded w-full mb-2">
                    <button type="submit"
                        class="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 w-full">Create
                        Season</button>
                </form>
            </div>

            <!-- Users Tab -->
            <div x-show="activeTab === 'users'">
                <h2 class="text-2xl font-semibold mb-4">User Management</h2>
                <select x-model="selectedSeason" @change="loadUsers()"
                    class="bg-gray-700 text-white p-2 rounded w-full mb-4">
                    <option value="">Select a Season</option>
                    <template x-for="season in seasons" :key="season.id">
                        <option :value="season.id" x-text="`Season ${season.id}`"></option>
                    </template>
                </select>
                <div x-show="users.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">Username</th>
                                <th class="px-4 py-2 text-left">Coins</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="user in users" :key="user.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="user.id"></td>
                                    <td class="px-4 py-2" x-text="user.username"></td>
                                    <td class="px-4 py-2" x-text="user.coins"></td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Games Tab -->
            <div x-show="activeTab === 'games'">
                <h2 class="text-2xl font-semibold mb-4">Games Management</h2>
                <button @click="loadPendingGames()"
                    class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 mb-4">
                    Load Pending Games
                </button>
                <div x-show="pendingGames.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">Home Team</th>
                                <th class="px-4 py-2 text-left">Away Team</th>
                                <th class="px-4 py-2 text-left">Week</th>
                                <th class="px-4 py-2 text-left">Date</th>
                                <th class="px-4 py-2 text-left">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="game in pendingGames" :key="game.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="game.id"></td>
                                    <td class="px-4 py-2" x-text="game.home_team"></td>
                                    <td class="px-4 py-2" x-text="game.away_team"></td>
                                    <td class="px-4 py-2" x-text="game.nfl_week"></td>
                                    <td class="px-4 py-2" x-text="formatDate(game.commence_time)"></td>
                                    <td class="px-4 py-2">
                                        <select x-model="game.winner" class="bg-gray-600 text-white p-1 rounded">
                                            <option value="">Select winner</option>
                                            <option value="home" x-text="game.home_team"></option>
                                            <option value="away" x-text="game.away_team"></option>
                                        </select>
                                        <button @click="updateGameResult(game.id, game.winner)"
                                            class="bg-green-600 text-white px-2 py-1 rounded hover:bg-green-700 ml-2">
                                            Update
                                        </button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Bets Tab -->
            <div x-show="activeTab === 'bets'">
                <h2 class="text-2xl font-semibold mb-4">Bets Management</h2>
                <select x-model="selectedSeasonForBets" @change="loadBetsForSeason()"
                    class="bg-gray-700 text-white p-2 rounded w-full mb-4">
                    <option value="">Select a Season</option>
                    <template x-for="season in seasons" :key="season.id">
                        <option :value="season.id" x-text="`Season ${season.id}`"></option>
                    </template>
                </select>

                <!-- Add the Audit and Correct All Bets button here -->
                <button @click="auditAndCorrectAllBets"
                    class="bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700 mb-4">
                    Audit and Correct All Bets
                </button>

                <div x-show="bets.length > 0" class="overflow-x-auto">
                    <table class="min-w-full bg-gray-700">
                        <thead>
                            <tr>
                                <th class="px-4 py-2 text-left">ID</th>
                                <th class="px-4 py-2 text-left">User</th>
                                <th class="px-4 py-2 text-left">Game</th>
                                <th class="px-4 py-2 text-left">Bet On Team</th>
                                <th class="px-4 py-2 text-left">Amount</th>
                                <th class="px-4 py-2 text-left">Result</th>
                                <th class="px-4 py-2 text-left">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template x-for="bet in bets" :key="bet.id">
                                <tr class="border-t border-gray-600">
                                    <td class="px-4 py-2" x-text="bet.id"></td>
                                    <td class="px-4 py-2" x-text="bet.username"></td>
                                    <td class="px-4 py-2" x-text="`${bet.home_team} vs ${bet.away_team}`"></td>
                                    <td class="px-4 py-2" x-text="bet.bet_on_team"></td>
                                    <td class="px-4 py-2" x-text="bet.amount"></td>
                                    <td class="px-4 py-2" x-text="bet.result || 'Pending'"></td>
                                    <td class="px-4 py-2">
                                        <select x-model="bet.newResult" class="bg-gray-600 text-white p-1 rounded">
                                            <option value="">Select result</option>
                                            <option value="won">Won</option>
                                            <option value="lost">Lost</option>
                                        </select>
                                        <button @click="correctBetResult(bet.id, bet.newResult)"
                                            class="bg-yellow-600 text-white px-2 py-1 rounded hover:bg-yellow-700 ml-2">
                                            Correct
                                        </button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Database Tab -->
            <div x-show="activeTab === 'database'">
                <h2 class="text-2xl font-semibold mb-4">Database Update</h2>
                <button @click="updateDatabase()"
                    class="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 w-full">
                    Manual Database Update
                </button>
                <div x-show="updateResult" x-text="updateResult" class="mt-4 p-2 rounded"
                    :class="{'bg-green-600': updateResult.includes('success'), 'bg-red-600': !updateResult.includes('success')}">
                </div>
            </div>
        </div>
    </div>

    <script>
        function dashboardData() {
            return {
                activeTab: 'seasons',
                seasons: [],
                users: [],
                pendingGames: [],
                bets: [],
                selectedSeason: '',
                selectedSeasonForBets: '',
                updateResult: '',
                errorMessage: '',
                newSeason: {
                    startWeek: '',
                    endWeek: '',
                    initialCoins: ''
                },

                init() {
                    console.log('Initializing dashboard...');
                    this.loadSeasons();
                },

                async fetchWithErrorHandling(url, options = {}) {
                    try {
                        const response = await fetch(url, options);
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        return await response.json();
                    } catch (error) {
                        console.error('Fetch error:', error);
                        this.errorMessage = `Error: ${error.message}`;
                        throw error;
                    }
                },

                async loadSeasons() {
                    console.log('Loading seasons...');
                    try {
                        this.seasons = await this.fetchWithErrorHandling('/api/seasons');
                        console.log('Seasons loaded:', this.seasons);
                    } catch (error) {
                        console.error('Error loading seasons:', error);
                    }
                },

                async createSeason() {
                    console.log('Creating season:', this.newSeason);
                    try {
                        const formData = new URLSearchParams();
                        formData.append('startWeek', this.newSeason.startWeek);
                        formData.append('endWeek', this.newSeason.endWeek);
                        formData.append('initialCoins', this.newSeason.initialCoins);

                        await this.fetchWithErrorHandling('/api/seasons', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: formData
                        });
                        console.log('Season created successfully');
                        await this.loadSeasons();
                        this.newSeason = { startWeek: '', endWeek: '', initialCoins: '' };
                    } catch (error) {
                        console.error('Error creating season:', error);
                    }
                },

                async deleteSeason(seasonId) {
                    console.log('Deleting season:', seasonId);
                    try {
                        await this.fetchWithErrorHandling(`/api/seasons/${seasonId}`, { method: 'DELETE' });
                        console.log('Season deleted successfully');
                        await this.loadSeasons();
                    } catch (error) {
                        console.error('Error deleting season:', error);
                    }
                },

                async loadUsers() {
                    if (this.selectedSeason) {
                        console.log('Loading users for season:', this.selectedSeason);
                        try {
                            this.users = await this.fetchWithErrorHandling(`/api/users/seasons/${this.selectedSeason}`);
                            console.log('Users loaded:', this.users);
                        } catch (error) {
                            console.error('Error loading users:', error);
                        }
                    }
                },

                async loadPendingGames() {
                    console.log('Loading pending games...');
                    try {
                        this.pendingGames = await this.fetchWithErrorHandling('/api/nfl/games/pending-results');
                        console.log('Pending games loaded:', this.pendingGames);
                    } catch (error) {
                        console.error('Error loading pending games:', error);
                    }
                },

                async updateGameResult(gameId, winner) {
                    console.log('Updating game result:', gameId, winner);
                    if (!winner) {
                        this.errorMessage = "Please select a winner before updating the game result.";
                        return;
                    }
                    try {
                        const response = await this.fetchWithErrorHandling(`/api/nfl/games/${gameId}/result`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: `winner=${winner}`
                        });
                        console.log('Game result updated successfully');
                        this.errorMessage = "Game result updated successfully and bets settled.";
                        await this.loadPendingGames();
                    } catch (error) {
                        console.error('Error updating game result:', error);
                        this.errorMessage = `Error updating game result: ${error.message}`;
                    }
                },

                formatDate(dateString) {
                    return new Date(dateString).toLocaleString();
                },

                async loadUnsettledBets() {
                    if (this.selectedGameId) {
                        console.log('Loading unsettled bets for game:', this.selectedGameId);
                        try {
                            this.unsettledBets = await this.fetchWithErrorHandling(`/api/bets/unsettled/${this.selectedGameId}`);
                            console.log('Unsettled bets loaded:', this.unsettledBets);
                        } catch (error) {
                            console.error('Error loading unsettled bets:', error);
                        }
                    }
                },

                async loadBetsForSeason() {
                    if (this.selectedSeasonForBets) {
                        console.log('Loading bets for season:', this.selectedSeasonForBets);
                        try {
                            this.bets = await this.fetchWithErrorHandling(`/api/bets/seasons/${this.selectedSeasonForBets}`);
                            console.log('Bets loaded:', this.bets);
                        } catch (error) {
                            console.error('Error loading bets:', error);
                            this.errorMessage = `Error loading bets: ${error.message}`;
                        }
                    }
                },

                async correctBetResult(betId, newResult) {
                    console.log('Correcting bet result:', betId, newResult);
                    if (!newResult) {
                        this.errorMessage = "Please select a new result before correcting the bet.";
                        return;
                    }
                    try {
                        const response = await this.fetchWithErrorHandling('/api/bets/correct-bet', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: `betId=${betId}&newResult=${newResult}`
                        });
                        console.log('Bet corrected successfully');
                        this.errorMessage = "Bet corrected successfully. Refreshing bets list.";
                        await this.loadBetsForSeason();
                    } catch (error) {
                        console.error('Error correcting bet:', error);
                        this.errorMessage = `Error correcting bet: ${error.message}`;
                    }
                },

                async auditAndCorrectAllBets() {
                    console.log('Auditing and correcting all bets...');
                    try {
                        const response = await this.fetchWithErrorHandling('/api/bets/audit-and-correct', {
                            method: 'POST'
                        });
                        console.log('Bets audited and corrected successfully');
                        this.errorMessage = response; // This will now contain the text response from the server
                        await this.loadBetsForSeason();
                    } catch (error) {
                        console.error('Error auditing and correcting bets:', error);
                        this.errorMessage = `Error auditing and correcting bets: ${error.message}`;
                    }
                },

                async updateDatabase() {
                    console.log('Updating database...');
                    try {
                        const response = await fetch('/api/admin/update-database', { method: 'POST' });
                        this.updateResult = await response.text();
                        console.log('Database update result:', this.updateResult);
                    } catch (error) {
                        console.error('Error updating database:', error);
                        this.updateResult = 'Error updating database';
                    }
                },

                formatDate(dateString) {
                    return new Date(dateString).toLocaleString();
                },

                async fetchWithErrorHandling(url, options = {}) {
                    try {
                        const response = await fetch(url, options);
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        const contentType = response.headers.get("content-type");
                        if (contentType && contentType.includes("application/json")) {
                            return await response.json();
                        } else {
                            return await response.text();
                        }
                    } catch (error) {
                        console.error('Fetch error:', error);
                        this.errorMessage = `Error: ${error.message}`;
                        throw error;
                    }
                }
            };
        }
    </script>
</body>

</html>
```

## File: target/classes/templates/login.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900 text-gray-100 flex items-center justify-center h-screen">
    <div class="bg-gray-800 p-8 rounded-lg shadow-lg w-96">
        <h1 class="text-2xl font-bold mb-6 text-center text-indigo-400">Login</h1>
        <form th:action="@{/login}" method="post" class="space-y-4">
            <div>
                <label for="username" class="block text-sm font-medium text-gray-300">Username</label>
                <input type="text" id="username" name="username" required class="mt-1 block w-full rounded-md bg-gray-700 border-gray-600 text-gray-100 focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50">
            </div>
            <div>
                <label for="password" class="block text-sm font-medium text-gray-300">Password</label>
                <input type="password" id="password" name="password" required class="mt-1 block w-full rounded-md bg-gray-700 border-gray-600 text-gray-100 focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50">
            </div>
            <div>
            </div>
            <div class="flex items-center">
                <input type="checkbox" id="remember-me" name="remember-me" class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                <label for="remember-me" class="ml-2 block text-sm text-gray-300">
                    Remember me
                </label>
            </div>
            <button type="submit" class="w-full bg-indigo-600 text-white rounded-md py-2 px-4 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-gray-800">Sign In</button>
        </form>
    </div>
</body>
</html>
```

## File: target/classes/templates/pending-games.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pending Game Results</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900 text-gray-100 p-8">
    <div class="max-w-7xl mx-auto">
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-3xl font-bold text-indigo-400">Pending Game Results</h1>
            <button id="manualUpdateBtn" class="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
                Manual Database Update
            </button>
        </div>
        <div id="updateMessage" class="mb-4 p-4 rounded hidden"></div>
        <div class="bg-gray-800 shadow-md rounded-lg overflow-hidden">
            <table class="min-w-full divide-y divide-gray-700">
                <thead class="bg-gray-700">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">ID</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Home Team</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Away Team</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Week</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Date</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Action</th>
                    </tr>
                </thead>
                <tbody class="bg-gray-800 divide-y divide-gray-700">
                    <tr th:each="game : ${games}">
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.id}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.home_team}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.away_team}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${game.nfl_week}"></td>
                        <td class="px-6 py-4 whitespace-nowrap" th:text="${#dates.format(game.commence_time, 'yyyy-MM-dd HH:mm')}"></td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <form th:action="@{/admin/update-result}" method="post" class="flex items-center space-x-2">
                                <input type="hidden" name="gameId" th:value="${game.id}" />
                                <select name="winner" required class="block w-full mt-1 rounded-md bg-gray-700 border-gray-600 text-gray-100 focus:border-indigo-500 focus:ring focus:ring-indigo-500 focus:ring-opacity-50">
                                    <option value="">Select winner</option>
                                    <option value="home" th:text="${game.home_team}"></option>
                                    <option value="away" th:text="${game.away_team}"></option>
                                </select>
                                <button type="submit" class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-indigo-500">
                                    Update Result
                                </button>
                            </form>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <script>
        document.getElementById('manualUpdateBtn').addEventListener('click', function() {
            fetch('/api/admin/update-database', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            })
            .then(response => response.text())
            .then(data => {
                const messageDiv = document.getElementById('updateMessage');
                messageDiv.textContent = data;
                messageDiv.classList.remove('hidden', 'bg-red-600', 'bg-green-600');
                messageDiv.classList.add(data.includes('successfully') ? 'bg-green-600' : 'bg-red-600');
                setTimeout(() => {
                    messageDiv.classList.add('hidden');
                }, 5000);
            })
            .catch((error) => {
                console.error('Error:', error);
                const messageDiv = document.getElementById('updateMessage');
                messageDiv.textContent = 'An error occurred while updating the database.';
                messageDiv.classList.remove('hidden', 'bg-green-600');
                messageDiv.classList.add('bg-red-600');
                setTimeout(() => {
                    messageDiv.classList.add('hidden');
                }, 5000);
            });
        });
    </script>
</body>
</html>
```

