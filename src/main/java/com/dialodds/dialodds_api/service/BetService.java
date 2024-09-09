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