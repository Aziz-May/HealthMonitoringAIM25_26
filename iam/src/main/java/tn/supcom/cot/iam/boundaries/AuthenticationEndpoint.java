package tn.supcom.cot.iam.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tn.supcom.cot.iam.controllers.managers.PhoenixIAMManager;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.security.Argon2Utility;
import tn.supcom.cot.iam.security.AuthorizationCode;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

/**
 * OAuth2 Authorization Endpoint with PKCE Support
 * Handles authorization requests, user login, and consent management
 */
@Path("/")
@RequestScoped
public class AuthenticationEndpoint {

    private static final String COOKIE_ID = "signInId";
    private static final int COOKIE_MAX_AGE = 600; // 10 minutes

    @Inject
    private Logger logger;

    @Inject
    private PhoenixIAMManager iamManager;

    /**
     * OAuth2 Authorization Endpoint
     * GET /authorize?client_id=...&redirect_uri=...&response_type=code&scope=...&code_challenge=...&state=...
     */
    @GET
    @Path("/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorize(@Context UriInfo uriInfo) {
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

            // Validate OAuth2 parameters
            AuthorizationRequest authRequest = validateAuthorizationRequest(params);

            // Create session cookie
            String cookieValue = buildCookieValue(authRequest);
            NewCookie sessionCookie = createSessionCookie(cookieValue);

            logger.info("Authorization request validated for client: " + authRequest.clientId);

            // Return login page
            return Response.ok(loadHtmlResource("/login.html"))
                    .cookie(sessionCookie)
                    .build();

        } catch (InvalidRequestException e) {
            logger.warning("Invalid authorization request: " + e.getMessage());
            return buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error processing authorization request: " + e.getMessage());
            return buildErrorResponse("Internal server error");
        }
    }

    /**
     * Registration Page
     * GET /register
     */
    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public Response showRegistrationPage(@Context UriInfo uriInfo) {
        return Response.ok(loadHtmlResource("/register.html")).build();
    }

    /**
     * User Registration Endpoint
     * POST /register
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("confirm_password") String confirmPassword,
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("scope") String scope,
            @FormParam("response_type") String responseType,
            @FormParam("code_challenge") String codeChallenge,
            @FormParam("state") String state) {

        try {
            // Validate passwords
            validatePasswords(password, confirmPassword);

            // Create identity
            Identity identity = iamManager.createIdentity(username, password);
            logger.info("Successfully registered user: " + username);

            // If OAuth parameters present, show consent
            if (clientId != null && !clientId.isEmpty()) {
                return showConsentPage(username, clientId, redirectUri, scope,
                        responseType, codeChallenge, state);
            }

            return buildSuccessResponse("Registration successful! You can now login.");

        } catch (IllegalArgumentException e) {
            return buildErrorResponse("Username already exists");
        } catch (InvalidRequestException e) {
            return buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.severe("Registration error: " + e.getMessage());
            return buildErrorResponse("Registration failed: " + e.getMessage());
        }
    }

    /**
     * User Login Endpoint
     * POST /login/authorization
     */
    @POST
    @Path("/login/authorization")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response login(
            @CookieParam(COOKIE_ID) Cookie sessionCookie,
            @FormParam("username") String username,
            @FormParam("password") String password) {

        try {
            // Validate session
            if (sessionCookie == null || sessionCookie.getValue() == null) {
                return buildErrorResponse("Session expired. Please try again.");
            }

            // Authenticate user
            Identity identity = authenticateUser(username, password);
            logger.info("User authenticated: " + username);

            // Parse OAuth parameters from cookie
            AuthorizationRequest authRequest = parseCookieValue(sessionCookie.getValue());

            // Check for existing grant (consent)
            Optional<Grant> existingGrant = iamManager.findGrant(
                    authRequest.clientId, identity.getId());

            if (existingGrant.isPresent()) {
                // User already gave consent - redirect directly
                logger.info("Existing grant found, skipping consent");
                String redirectUri = buildRedirectUri(authRequest, username,
                        existingGrant.get().getApprovedScopes());
                return Response.seeOther(URI.create(redirectUri)).build();
            } else {
                // First time - show consent page
                logger.info("No grant found, showing consent page");
                return showConsentPageWithCookie(username, authRequest);
            }

        } catch (InvalidCredentialsException e) {
            logger.info("Failed login attempt for: " + username);
            return buildErrorResponse("Invalid username or password");
        } catch (Exception e) {
            logger.severe("Login error: " + e.getMessage());
            return buildErrorResponse("Login failed. Please try again.");
        }
    }

    /**
     * Consent Endpoint (POST)
     * POST /login/authorization/consent
     */
    @POST
    @Path("/login/authorization/consent")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response handleConsent(
            @CookieParam(COOKIE_ID) Cookie sessionCookie,
            @FormParam("approved_scope") String approvedScopes,
            @FormParam("approval_status") String approvalStatus,
            @FormParam("username") String username) {

        return processConsent(sessionCookie, approvedScopes, approvalStatus, username);
    }

    /**
     * Consent Endpoint (PATCH) - For X-HTTP-Method-Override support
     * PATCH /login/authorization
     */
    @PATCH
    @Path("/login/authorization")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response handleConsentPatch(
            @CookieParam(COOKIE_ID) Cookie sessionCookie,
            @FormParam("approved_scope") String approvedScopes,
            @FormParam("approval_status") String approvalStatus,
            @FormParam("username") String username) {

        return processConsent(sessionCookie, approvedScopes, approvalStatus, username);
    }

    // ==================== Private Helper Methods ====================

    private Response processConsent(Cookie sessionCookie, String approvedScopes,
                                    String approvalStatus, String username) {
        try {
            if (sessionCookie == null || sessionCookie.getValue() == null) {
                return buildErrorResponse("Session expired");
            }

            AuthorizationRequest authRequest = parseCookieValue(sessionCookie.getValue());

            // Extract username from cookie if not in form
            if (username == null || username.isEmpty()) {
                username = authRequest.username;
            }

            // Handle denial
            if ("NO".equals(approvalStatus)) {
                logger.info("User denied consent: " + username);
                return redirectWithError(authRequest.redirectUri, "access_denied",
                        "User denied the request", authRequest.state);
            }

            // Validate approved scopes
            if (approvedScopes == null || approvedScopes.trim().isEmpty()) {
                return redirectWithError(authRequest.redirectUri, "invalid_scope",
                        "No scopes approved", authRequest.state);
            }

            // Save grant
            iamManager.saveGrant(authRequest.clientId, username, approvedScopes);
            logger.info("Grant saved for user: " + username);

            // Generate authorization code and redirect
            String redirectUri = buildRedirectUri(authRequest, username, approvedScopes);
            return Response.seeOther(URI.create(redirectUri)).build();

        } catch (Exception e) {
            logger.severe("Consent processing error: " + e.getMessage());
            return buildErrorResponse("An error occurred processing your consent");
        }
    }

    private AuthorizationRequest validateAuthorizationRequest(
            MultivaluedMap<String, String> params) throws InvalidRequestException {

        AuthorizationRequest request = new AuthorizationRequest();

        // Validate client_id
        request.clientId = params.getFirst("client_id");
        if (request.clientId == null || request.clientId.isEmpty()) {
            throw new InvalidRequestException("client_id is required");
        }

        var tenant = iamManager.findTenantByName(request.clientId);
        if (tenant == null) {
            throw new InvalidRequestException("Unknown client: " + request.clientId);
        }

        // Validate grant type support
        if (tenant.getSupportedGrantTypes() != null &&
                !tenant.getSupportedGrantTypes().contains("authorization_code")) {
            throw new InvalidRequestException("Authorization code flow not supported");
        }

        // Validate redirect_uri
        request.redirectUri = params.getFirst("redirect_uri");
        if (tenant.getRedirectUri() != null && !tenant.getRedirectUri().isEmpty()) {
            if (request.redirectUri != null && !request.redirectUri.isEmpty() &&
                    !tenant.getRedirectUri().equals(request.redirectUri)) {
                throw new InvalidRequestException("redirect_uri mismatch");
            }
            request.redirectUri = tenant.getRedirectUri();
        } else {
            if (request.redirectUri == null || request.redirectUri.isEmpty()) {
                throw new InvalidRequestException("redirect_uri is required");
            }
        }

        // Validate response_type
        request.responseType = params.getFirst("response_type");
        if (!"code".equals(request.responseType)) {
            throw new InvalidRequestException("Only response_type=code is supported");
        }

        // Validate PKCE
        request.codeChallengeMethod = params.getFirst("code_challenge_method");
        if (!"S256".equals(request.codeChallengeMethod)) {
            throw new InvalidRequestException("code_challenge_method must be S256");
        }

        request.codeChallenge = params.getFirst("code_challenge");
        if (request.codeChallenge == null || request.codeChallenge.isEmpty()) {
            throw new InvalidRequestException("code_challenge is required");
        }

        // Handle scope
        request.scope = params.getFirst("scope");
        if (request.scope == null || request.scope.isEmpty()) {
            request.scope = tenant.getRequiredScopes();
        }

        // Get state
        request.state = params.getFirst("state");
        if (request.state == null || request.state.isEmpty()) {
            logger.warning("No state parameter - CSRF protection weakened");
        }

        return request;
    }

    private void validatePasswords(String password, String confirmPassword)
            throws InvalidRequestException {
        if (password == null || password.isEmpty()) {
            throw new InvalidRequestException("Password is required");
        }
        if (!password.equals(confirmPassword)) {
            throw new InvalidRequestException("Passwords do not match");
        }
        if (password.length() < 8) {
            throw new InvalidRequestException("Password must be at least 8 characters");
        }
    }

    private Identity authenticateUser(String username, String password)
            throws InvalidCredentialsException {
        try {
            Identity identity = iamManager.findIdentityByName(username);
            if (!Argon2Utility.check(identity.getPassword(), password.toCharArray())) {
                throw new InvalidCredentialsException("Invalid credentials");
            }
            return identity;
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    private String buildCookieValue(AuthorizationRequest request) {
        return String.format("%s#%s$%s&%s@%s!%s",
                request.clientId,
                request.scope,
                request.redirectUri,
                request.responseType,
                request.codeChallenge,
                request.state != null ? request.state : ""
        );
    }

    private AuthorizationRequest parseCookieValue(String cookieValue) {
        AuthorizationRequest request = new AuthorizationRequest();

        // Split by % to get username if present
        String[] usernameParts = cookieValue.split("%");
        if (usernameParts.length > 1) {
            request.username = usernameParts[1];
            cookieValue = usernameParts[0];
        }

        // Parse: clientId#scope$redirectUri&responseType@codeChallenge!state
        String[] mainParts = cookieValue.split("\\$");

        // clientId#scope
        String[] clientScope = mainParts[0].split("#");
        request.clientId = clientScope[0];
        request.scope = clientScope[1];

        // redirectUri&responseType@codeChallenge!state
        if (mainParts.length > 1) {
            String[] uriAndRest = mainParts[1].split("&", 2);
            request.redirectUri = uriAndRest[0];

            if (uriAndRest.length > 1) {
                String[] typeAndRest = uriAndRest[1].split("@", 2);
                request.responseType = typeAndRest[0];

                if (typeAndRest.length > 1) {
                    String[] challengeAndState = typeAndRest[1].split("!", 2);
                    request.codeChallenge = challengeAndState[0];
                    if (challengeAndState.length > 1) {
                        request.state = challengeAndState[1];
                    }
                }
            }
        }

        return request;
    }

    private String buildRedirectUri(AuthorizationRequest request, String username,
                                    String approvedScopes) throws Exception {
        AuthorizationCode authCode = new AuthorizationCode(
                request.clientId,
                username,
                approvedScopes,
                Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond(),
                request.redirectUri
        );

        String code = URLEncoder.encode(
                authCode.getCode(request.codeChallenge),
                StandardCharsets.UTF_8
        );

        StringBuilder uri = new StringBuilder(request.redirectUri);
        uri.append("?code=").append(code);

        if (request.state != null && !request.state.isEmpty()) {
            uri.append("&state=").append(
                    URLEncoder.encode(request.state, StandardCharsets.UTF_8)
            );
        }

        return uri.toString();
    }

    private Response redirectWithError(String redirectUri, String error,
                                       String errorDescription, String state) {
        try {
            UriBuilder builder = UriBuilder.fromUri(redirectUri)
                    .queryParam("error", error)
                    .queryParam("error_description", errorDescription);

            if (state != null && !state.isEmpty()) {
                builder.queryParam("state", state);
            }

            return Response.seeOther(builder.build()).build();
        } catch (Exception e) {
            return buildErrorResponse(errorDescription);
        }
    }

    private Response showConsentPage(String username, String clientId,
                                     String redirectUri, String scope,
                                     String responseType, String codeChallenge,
                                     String state) {
        String cookieValue = String.format("%s#%s$%s&%s@%s!%s%%%s",
                clientId,
                scope != null ? scope : "resource.read resource.write",
                redirectUri,
                responseType != null ? responseType : "code",
                codeChallenge != null ? codeChallenge : "",
                state != null ? state : "",
                username
        );

        NewCookie cookie = createSessionCookie(cookieValue);
        String html = loadHtmlResource("/consent.html")
                .replace("{{scope}}", scope != null ? scope : "resource.read resource.write")
                .replace("{{username}}", username);

        return Response.ok(html).cookie(cookie).build();
    }

    private Response showConsentPageWithCookie(String username,
                                               AuthorizationRequest request) {
        String cookieValue = buildCookieValue(request) + "%" + username;
        NewCookie cookie = createSessionCookie(cookieValue);

        String html = loadHtmlResource("/consent.html")
                .replace("{{scope}}", request.scope)
                .replace("{{username}}", username);

        return Response.ok(html).cookie(cookie).build();
    }

    private NewCookie createSessionCookie(String value) {
        return new NewCookie.Builder(COOKIE_ID)
                .value(value)
                .path("/")
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(COOKIE_MAX_AGE)
                .build();
    }

    private String loadHtmlResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Error loading resource: " + resourcePath);
            throw new RuntimeException(e);
        }
    }

    private Response buildErrorResponse(String errorMessage) {
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <title>Error - Health Monitoring IAM</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .error-container {
                        background: white;
                        border-radius: 12px;
                        padding: 40px;
                        max-width: 500px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        text-align: center;
                    }
                    .error-icon { font-size: 48px; margin-bottom: 20px; }
                    h1 { color: #dc3545; margin-bottom: 20px; }
                    p { color: #666; line-height: 1.6; }
                    .btn {
                        display: inline-block;
                        margin-top: 20px;
                        padding: 12px 24px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">⚠️</div>
                    <h1>Authentication Error</h1>
                    <p>%s</p>
                    <a href="javascript:history.back()" class="btn">Go Back</a>
                </div>
            </body>
            </html>
            """, errorMessage);

        return Response.status(Response.Status.BAD_REQUEST).entity(html).build();
    }

    private Response buildSuccessResponse(String message) {
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <title>Success - Health Monitoring IAM</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .success-container {
                        background: white;
                        border-radius: 12px;
                        padding: 40px;
                        max-width: 500px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        text-align: center;
                    }
                    .success-icon { font-size: 48px; margin-bottom: 20px; }
                    h1 { color: #28a745; margin-bottom: 20px; }
                    p { color: #666; line-height: 1.6; }
                </style>
            </head>
            <body>
                <div class="success-container">
                    <div class="success-icon">✅</div>
                    <h1>Success</h1>
                    <p>%s</p>
                </div>
            </body>
            </html>
            """, message);

        return Response.ok(html).build();
    }

    // ==================== Inner Classes ====================

    private static class AuthorizationRequest {
        String clientId;
        String redirectUri;
        String responseType;
        String scope;
        String codeChallenge;
        String codeChallengeMethod;
        String state;
        String username; // Set after login
    }

    private static class InvalidRequestException extends Exception {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    private static class InvalidCredentialsException extends Exception {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}