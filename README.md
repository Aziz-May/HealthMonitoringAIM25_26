# Health Monitoring System (AIM 2025-26)

A comprehensive Health Monitoring System featuring a Jakarta EE backend (IAM), a Vanilla JavaScript frontend (PWA), and MongoDB for data storage.

## Project Structure

*   **`app/`**: Frontend application (Progressive Web App).
*   **`iam/`**: Identity and Access Management backend (Jakarta EE 11).
*   **`docs/`**: Project documentation.

## Prerequisites

*   **Java Development Kit (JDK)**: Version 21 or higher.
*   **Maven**: Version 3.8 or higher.
*   **WildFly Preview**: Version 38.0.1.Final (Jakarta EE 11 compatible).
*   **MongoDB**: Version 5.0 or higher (running locally on port 27017).


## Setup Instructions

### 1. Database Setup (MongoDB)

1.  Ensure MongoDB is running on `localhost:27017`.
2.  Open a terminal in the project root.
3.  Run the setup script to initialize the database with the required Tenant and Test User:
    ```powershell
    mongosh --file setup-mongodb.js
    ```
    *   **Tenant**: `health-monitoring-app`
    *   **Test User**: `testuser` / `Test123!`

### 2. Backend Setup (IAM)

For detailed instructions on setting up the application server, please refer to [WILDFLY_SETUP.md](WILDFLY_SETUP.md).

**Quick Start:**

1.  Ensure **WildFly Preview 38.0.1.Final** (Jakarta EE 11) is running with **JDK 21**.
2.  Navigate to the `iam` directory:
    ```bash
    cd iam
    ```
3.  Build and deploy the application using Maven:
    ```bash
    mvn clean install wildfly:deploy
    ```

### 3. Frontend Setup (App)

1.  Open the `app` folder in VS Code.
2.  Use the **Live Server** extension to serve `index.html`.
    *   Right-click `app/index.html` -> "Open with Live Server".
3.  The app should open at `http://127.0.0.1:5500/app/`.

## Usage

1.  Open the frontend URL (`http://127.0.0.1:5500/app/`).
2.  Click **Login** to sign in with `aziz` / `12345678!`.
3.  Click **SignUp** to create a new account.
4.  After login, you will be redirected to the Dashboard.

## Configuration

*   **Backend Config**: `iam/src/main/resources/META-INF/microprofile-config.properties`
    *   MongoDB connection
    *   Argon2 password hashing settings
    *   JWT settings
*   **Frontend Config**: `app/scripts/iam.js`
    *   OAuth2 endpoints
    *   Client ID and Redirect URI

