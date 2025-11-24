# WildFly Setup

## Requirements
*   **JDK 21**
*   **WildFly Preview 38.0.1.Final** (Required for Jakarta EE 11 support)

## Setup & Run
1.  Download and extract **WildFly Preview**.
2.  Start the server by running `bin/standalone.bat` (Windows) or `bin/standalone.sh` (Linux/Mac).

## Deploy
Once the server is running, deploy the application using:

```bash
mvn clean install wildfly:deploy
```
