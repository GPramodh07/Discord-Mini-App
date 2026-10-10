# DiscordMiniServer 🖥️

Java TCP Socket Server backend for the DiscordMini application.

## Quick Start
```bash
# Compile server code
javac -d bin -cp "lib/*" $(find src -name "*.java")

# Run the server on port 5000
java -cp "bin:lib/*" com.discordmini.server.ServerMain
```

## Features & Error Handling
- **Database User Check**: Verifies if username exists in SQLite database before verifying password.
- **Distinct Error Responses**:
  - `User does not exist in database`
  - `Incorrect password. Please try again.`
- **Multithreaded Session Handling**: Manages concurrent user sessions and real-time message broadcasting.

## Tech Stack
- Java 17
- SQLite JDBC (`org.xerial:sqlite-jdbc`)
- JSON Java (`org.json:json`)
