# Health Monitoring System (AIM 2025-26)

A comprehensive Health Monitoring System featuring a Jakarta EE backend (IAM), a Vanilla JavaScript frontend (PWA), and MongoDB for data storage.

## Project Structure

*   **`app/`**: Frontend application (Progressive Web App).
*   **`iam/`**: Identity and Access Management backend (Jakarta EE 11).
*   **`docs/`**: Project documentation.

## Features

### Authentication & Authorization
- **OAuth2 PKCE Flow**: Secure authorization code flow with S256 code challenge
- **User Registration**: Create new accounts with password strength validation
- **User Login**: Authenticate existing users with session management
- **Consent Management**: One-time consent approval for scopes (appears only during registration)
- **JWT Tokens**: Secure access and refresh tokens for API authorization

### Frontend Validation
- **Real-time Field Validation**: Instant feedback as users type
- **Username Validation**: 3-50 characters, alphanumeric with _ and -
- **Password Strength Indicator**: Visual bar showing Weak/Medium/Strong
- **Password Requirements**: Minimum 8 characters with mix of letters, numbers, and symbols
- **Confirm Password**: Ensures passwords match before submission
- **Inline Error Messages**: Clear, user-friendly error display without page redirects

### Security
- **Argon2 Password Hashing**: Industry-standard secure password storage
- **PKCE Protection**: Prevents authorization code interception attacks
- **HTTP-Only Cookies**: Secure session management
- **Input Sanitization**: HTML escaping to prevent XSS attacks

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
2.  Click **Login** to sign in with existing credentials.
3.  Click **SignUp** to create a new account.
    - Fill in username (3-50 characters, alphanumeric)
    - Create a strong password (8+ characters, mix of uppercase, lowercase, numbers, symbols)
    - Confirm your password
    - Approve consent for requested permissions (one-time only)
4.  After authentication, you will be redirected to the Dashboard.

### Validation Features

**Login Page:**
- Username: Required, 3-50 characters
- Password: Required, minimum 6 characters
- Real-time error display with clear messages

**Registration Page:**
- Username: Required, 3-50 characters, alphanumeric with _ and -
- Password: Required, minimum 8 characters with strength indicator
- Confirm Password: Must match password
- Visual password strength bar (Weak - Medium - Strong)

## Configuration

*   **Backend Config**: `iam/src/main/resources/META-INF/microprofile-config.properties`
    *   MongoDB connection
    *   Argon2 password hashing settings
    *   JWT settings
*   **Frontend Config**: `app/scripts/iam.js`
    *   OAuth2 endpoints
    *   Client ID and Redirect URI

## API Endpoints

### Authentication
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/iam-1.0/rest-iam/login/authorization` | GET | Start OAuth2 authorization flow |
| `/iam-1.0/rest-iam/login/authorization` | POST | Process login credentials |
| `/iam-1.0/rest-iam/register` | POST | Register new user |
| `/iam-1.0/rest-iam/login/authorization/consent` | POST | Process consent approval |
| `/iam-1.0/rest-iam/token` | POST | Exchange authorization code for tokens |

### Token Management
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/iam-1.0/rest-iam/token` | POST | Refresh access token |
| `/iam-1.0/rest-iam/.well-known/jwks.json` | GET | Get JWK Set for token verification |
