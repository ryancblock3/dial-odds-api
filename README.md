# DialOdds API

## Overview

DialOdds API is a Spring Boot application designed to manage NFL betting odds and user bets. It provides a robust backend for handling seasons, users, games, and bets, along with an admin dashboard for easy management. The API is designed to work in conjunction with the SeasonsBot Discord bot for user interaction.

## Features

- NFL game and odds management
- User and season management
- Betting system
- Admin dashboard for easy management
- Automated database updates
- RESTful API endpoints
- Integration with SeasonsBot Discord bot

## Technologies Used

- Java 17
- Spring Boot
- Spring Security
- Spring JDBC
- PostgreSQL
- Thymeleaf
- Alpine.js
- Tailwind CSS

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 17 or later
- Maven
- PostgreSQL

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/dialodds-api.git
   ```

2. Navigate to the project directory:
   ```
   cd dialodds-api
   ```

3. Create a `.env` file in the root directory with the following contents:
   ```
   API_KEY=your_odds_api_key
   API_BOOKMAKER=your_preferred_bookmaker
   API_TARGETED_SPORTS=americanfootball_nfl,h2h
   ADMIN_USERNAME=your_admin_username
   ADMIN_PASSWORD=your_admin_password
   REMEMBER_ME_KEY=your_remember_me_key
   ```

4. Build the project:
   ```
   mvn clean install
   ```

5. Run the application:
   ```
   java -jar target/dialodds-api-0.0.1-SNAPSHOT.jar
   ```

The application will start running at `http://localhost:8080`.

## API Endpoints

See the [API Documentation](API_DOCUMENTATION.md) for a full list of available endpoints.

## Admin Dashboard

The admin dashboard is available at `http://localhost:8080/admin/dashboard`. It provides an interface for managing seasons, users, games, and bets.

## Security

The application uses Spring Security for authentication. The admin dashboard is protected and requires login.

## Scheduled Tasks

The application includes scheduled tasks for automatically updating the database at specific times. See the [Scheduled Tasks Documentation](SCHEDULED_TASKS.md) for more details.

## Integration with SeasonsBot

This API is designed to work with the SeasonsBot Discord bot. The bot interacts with this API to provide a user-friendly interface for players to manage their bets and view game information directly through Discord.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

- The Odds API for providing NFL odds data
- Spring Boot team for the excellent framework
- Alpine.js and Tailwind CSS for frontend utilities
