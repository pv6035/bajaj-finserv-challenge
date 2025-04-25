# Bajaj Finserv Health Programming Challenge

This is a Spring Boot application that automatically interacts with a remote API at application startup, solves either the Mutual Followers problem or the Nth-level Followers problem, and sends the result to a webhook with JWT authentication.

## Project Overview

The application implements the following requirements:
- Makes a POST request to generate a webhook at startup
- Processes data based on registration number (odd = Question 1, even = Question 2)
- Sends the result to the provided webhook using JWT authentication
- Implements a retry mechanism (up to 4 times) for webhook communication

## Project Structure

```
bajaj-finserv-challenge/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── challenge/
│   │   │           └── bfhl/
│   │   │               ├── AppConfig.java
│   │   │               ├── BajajFinservChallengeApplication.java
│   │   │               ├── WebhookRunner.java
│   │   │               └── WebhookService.java
│   │   └── resources/
│   │       └── application.properties
```

## Key Components

- **BajajFinservChallengeApplication.java**: Main Spring Boot application class
- **WebhookRunner.java**: ApplicationRunner that executes on startup
- **WebhookService.java**: Service class containing the business logic
- **AppConfig.java**: Configuration class for bean definitions

## Algorithms

### Mutual Followers (Question 1)
- Identifies pairs where both users follow each other
- Outputs direct 2-node cycles as [min, max] pairs
- Uses a HashMap for efficient lookups

### Nth-Level Followers (Question 2)
- Uses BFS to find users exactly n levels away in the follows network
- Tracks visited nodes to avoid cycles
- Returns a sorted list of user IDs

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/bfhl-0.0.1-SNAPSHOT.jar
```

## Configuration

The application uses the following configuration in `application.properties`:
```
spring.application.name=bajaj-finserv-challenge
logging.level.com.challenge.bfhl=INFO
```

You can modify the logging level as needed for debugging purposes.

## Testing

The application has been tested with the provided example inputs for both questions.

## Notes

- The registration number "REG12347" is used, which has an odd number (47) at the end, so the application solves Question 1 (Mutual Followers)
- The application implements a retry mechanism that will attempt to send results to the webhook up to 4 times in case of failure
