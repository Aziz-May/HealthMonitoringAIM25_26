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
import java.util.Optional;
import java.util.logging.Logger;

@Path("/")
@RequestScoped
public class AuthenticationEndpoint {

    private static final String COOKIE_ID = "signInId";
    private static final int COOKIE_MAX_AGE = 1800; // 30 minutes

    @Inject
    private Logger logger;

    @Inject
    private PhoenixIAMManager iamManager;

    /**
     * 1. Point d'entrée OAuth2
     */
    @GET
    @Path("/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorize(@Context UriInfo uriInfo) {
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            AuthorizationRequest authRequest = validateAuthorizationRequest(params);

            // On sauvegarde l'état OAuth dans un cookie
            String cookieValue = buildCookieValue(authRequest);
            NewCookie sessionCookie = createSessionCookie(cookieValue);

            logger.info("Authorize request validated for: " + authRequest.clientId);

            return Response.ok(loadHtmlResource("/login.html"))
                    .cookie(sessionCookie)
                    .build();
        } catch (InvalidRequestException e) {
            return buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Internal server error");
        }
    }

    /**
     * 2. Page d'inscription
     */
    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public Response showRegistrationPage() {
        return Response.ok(loadHtmlResource("/register.html")).build();
    }

    /**
     * 3. Traitement Inscription -> Envoi Email -> Redirection vers Activation
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("confirm_password") String confirmPassword,
            @FormParam("email") String email,
            @FormParam("phone") String phone,
            @FormParam("birthdate") String birthdate,
            // Paramètres OAuth (cachés dans le formulaire register)
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("scope") String scope,
            @FormParam("response_type") String responseType,
            @FormParam("code_challenge") String codeChallenge,
            @FormParam("state") String state) {

        try {
            validatePasswords(password, confirmPassword);
            validateEmail(email);
            validatePhone(phone);
            java.time.LocalDate birthDate = parseBirthdate(birthdate);

            // Création du compte (Inactif + Code généré + Email envoyé)
            iamManager.createIdentity(username, password, email, phone, birthDate);
            logger.info("User registered pending activation: " + username);

            // IMPORTANT: On redirige vers la page d'activation en transmettant les infos OAuth
            // pour ne pas perdre le contexte (sinon le futur Login plantera).
            return showActivationPage(username, clientId, redirectUri, scope, responseType, codeChallenge, state, null);

        } catch (Exception e) {
            return showRegisterPageWithError(e.getMessage(), clientId, redirectUri, scope, responseType, codeChallenge, state);
        }
    }

    /**
     * 4. Validation du Code Email -> Retour vers Login (cookie restauré)
     */
    @POST
    @Path("/register/activate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response activateAccount(
            @FormParam("username") String username,
            @FormParam("code") String code,
            // Paramètres OAuth (cachés dans le formulaire activation)
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("scope") String scope,
            @FormParam("response_type") String responseType,
            @FormParam("code_challenge") String codeChallenge,
            @FormParam("state") String state) {

        try {
            // Activation métier
            iamManager.activateAccount(username, code);
            logger.info("Account activated: " + username);

            // --- RECONSTRUCTION DU COOKIE (CRUCIAL) ---
            // On recrée l'objet Request comme au début (/authorize)
            AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.clientId = clientId;
            authRequest.redirectUri = redirectUri;
            authRequest.scope = scope;
            authRequest.responseType = responseType;
            authRequest.codeChallenge = codeChallenge;
            authRequest.state = state;

            // On génère le cookie pour que le Login puisse le lire
            String cookieValue = buildCookieValue(authRequest);
            NewCookie sessionCookie = createSessionCookie(cookieValue);
            // ------------------------------------------

            // On affiche la page de Login avec le message de succès
            String loginHtml = loadHtmlResource("/login.html");
            loginHtml = loginHtml.replace("<div id=\"errorMessage\" class=\"error-message\"></div>",
                    "<div class='error-message show' style='background:#d4edda; color:#155724; border-color:#c3e6cb;'>Account activated! Please Sign In.</div>");

            return Response.ok(loginHtml).cookie(sessionCookie).build();

        } catch (Exception e) {
            return showActivationPage(username, clientId, redirectUri, scope, responseType, codeChallenge, state, e.getMessage());
        }
    }

    /**
     * 5. Login -> Consentement
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
            // Vérif Cookie
            if (sessionCookie == null || sessionCookie.getValue() == null || sessionCookie.getValue().trim().isEmpty()) {
                return buildErrorResponse("Session expired (Cookie missing). Please return to the App and try again.");
            }

            // Authentification (Vérifie MDP + Activation)
            Identity identity = authenticateUser(username, password);
            logger.info("User authenticated: " + username);

            // Parsing Cookie (C'est ici que vous aviez l'erreur IndexOutOfBounds)
            AuthorizationRequest authRequest = parseCookieValue(sessionCookie.getValue());

            // Sécurité supplémentaire : si le parsing a échoué (clientId vide), on arrête
            if (authRequest.clientId == null || authRequest.clientId.isEmpty()) {
                logger.severe("Cookie corrupted for user: " + username);
                return buildErrorResponse("Session corrupted. Please restart from the application.");
            }

            // Logique Grant / Consentement
            Optional<Grant> existingGrant = iamManager.findGrant(authRequest.clientId, identity.getId());
            String approvedScopes = existingGrant.map(Grant::getApprovedScopes).orElse(authRequest.scope);

            if (existingGrant.isPresent()) {
                String redirectUri = buildRedirectUri(authRequest, username, approvedScopes);
                return Response.seeOther(URI.create(redirectUri)).build();
            } else {
                return showConsentPageWithCookie(username, authRequest);
            }

        } catch (InvalidCredentialsException e) {
            return showLoginPageWithError(e.getMessage(), sessionCookie);
        } catch (Exception e) {
            logger.severe("Login unexpected error: " + e.getMessage());
            e.printStackTrace();
            return showLoginPageWithError("Login failed. Please try again.", sessionCookie);
        }
    }

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

    /**
     * Parsing Sécurisé du Cookie pour éviter "Index 0 out of bounds"
     */
    private AuthorizationRequest parseCookieValue(String cookieValue) {
        AuthorizationRequest request = new AuthorizationRequest();
        if (cookieValue == null || cookieValue.isEmpty()) return request;

        try {
            // Format: clientId#scope$redirectUri&responseType@codeChallenge!state%username

            // 1. Extraire Username
            String[] usernameParts = cookieValue.split("%");
            if (usernameParts.length > 1) {
                request.username = usernameParts[1];
                cookieValue = usernameParts[0];
            }

            // 2. Séparer ClientInfo du reste ($)
            String[] mainParts = cookieValue.split("\\$");
            if (mainParts.length == 0) return request;

            // 3. ClientId et Scope (#)
            if (mainParts[0].contains("#")) {
                String[] clientScope = mainParts[0].split("#");
                request.clientId = clientScope.length > 0 ? clientScope[0] : "";
                request.scope = clientScope.length > 1 ? clientScope[1] : "";
            } else {
                request.clientId = mainParts[0];
                request.scope = "";
            }

            // 4. Le reste (RedirectUri, PKCE, State)
            if (mainParts.length > 1) {
                String[] uriAndRest = mainParts[1].split("&", 2);
                request.redirectUri = uriAndRest.length > 0 ? uriAndRest[0] : "";

                if (uriAndRest.length > 1) {
                    String[] typeAndRest = uriAndRest[1].split("@", 2);
                    request.responseType = typeAndRest.length > 0 ? typeAndRest[0] : "";

                    if (typeAndRest.length > 1) {
                        String[] challengeAndState = typeAndRest[1].split("!", 2);
                        request.codeChallenge = challengeAndState.length > 0 ? challengeAndState[0] : "";
                        if (challengeAndState.length > 1) {
                            request.state = challengeAndState[1];
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Cookie parse error: " + e.getMessage());
        }
        return request;
    }

    private Identity authenticateUser(String username, String password) throws InvalidCredentialsException {
        try {
            Identity identity = iamManager.findIdentityByName(username);

            // VÉRIFICATION ACTIVATION
            if (!identity.isAccountActivated()) {
                throw new InvalidCredentialsException("Account not activated. Please check your email.");
            }

            if (!Argon2Utility.check(identity.getPassword(), password.toCharArray())) {
                throw new InvalidCredentialsException("Invalid credentials");
            }
            return identity;
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    private Response processConsent(Cookie sessionCookie, String approvedScopes, String approvalStatus, String username) {
        try {
            if (sessionCookie == null) return buildErrorResponse("Session expired");
            AuthorizationRequest authRequest = parseCookieValue(sessionCookie.getValue());
            if (username == null || username.isEmpty()) username = authRequest.username;

            if ("NO".equals(approvalStatus)) {
                return redirectWithError(authRequest.redirectUri, "access_denied", "User denied", authRequest.state);
            }

            iamManager.saveGrant(authRequest.clientId, username, approvedScopes);
            String redirectUri = buildRedirectUri(authRequest, username, approvedScopes);
            return Response.seeOther(URI.create(redirectUri)).build();
        } catch (Exception e) {
            return buildErrorResponse("Consent Error: " + e.getMessage());
        }
    }

    // --- Gestion des Pages et Formulaires Cachés ---

    private Response showActivationPage(String username, String clientId, String redirectUri, String scope,
                                        String responseType, String codeChallenge, String state, String errorMessage) {
        String html = loadHtmlResource("/activate.html");
        html = html.replace("{{username}}", username != null ? username : "");

        if (errorMessage != null) {
            html = html.replace("<div id=\"errorMessage\" class=\"error-message\"></div>",
                    "<div class='error-message show'>" + escapeHtml(errorMessage) + "</div>");
        }

        StringBuilder hidden = new StringBuilder();
        // Injection sécurisée (vérifie null)
        appendHidden(hidden, "client_id", clientId);
        appendHidden(hidden, "redirect_uri", redirectUri);
        appendHidden(hidden, "scope", scope);
        appendHidden(hidden, "response_type", responseType);
        appendHidden(hidden, "code_challenge", codeChallenge);
        appendHidden(hidden, "state", state);

        html = html.replace("<!-- OAUTH_PARAMS -->", hidden.toString());
        return Response.ok(html).build();
    }

    private void appendHidden(StringBuilder sb, String name, String value) {
        if(value != null && !value.isEmpty()) {
            sb.append("<input type='hidden' name='").append(name).append("' value='").append(escapeHtml(value)).append("'>");
        }
    }

    private Response showRegisterPageWithError(String msg, String clientId, String redirectUri, String scope, String rt, String cc, String state) {
        String html = loadHtmlResource("/register.html");
        html = html.replace("<div id=\"errorMessage\" class=\"error-message\"></div>",
                "<div class='error-message show'>" + escapeHtml(msg) + "</div>");

        // Réinjecter les valeurs pour le retry
        if(clientId != null) html = html.replace("id=\"client_id\"", "id=\"client_id\" value=\"" + escapeHtml(clientId) + "\"");
        if(redirectUri != null) html = html.replace("id=\"redirect_uri\"", "id=\"redirect_uri\" value=\"" + escapeHtml(redirectUri) + "\"");
        // ... (etc pour les autres si nécessaire)

        return Response.ok(html).build();
    }

    private Response showLoginPageWithError(String msg, Cookie cookie) {
        String html = loadHtmlResource("/login.html");
        html = html.replace("<div id=\"errorMessage\" class=\"error-message\"></div>",
                "<div class='error-message show'>" + escapeHtml(msg) + "</div>");
        Response.ResponseBuilder rb = Response.ok(html);
        if(cookie != null) rb.cookie(new NewCookie.Builder(COOKIE_ID).value(cookie.getValue()).path("/").httpOnly(true).maxAge(COOKIE_MAX_AGE).build());
        return rb.build();
    }

    // --- Helpers Utilitaires ---

    private String buildCookieValue(AuthorizationRequest request) {
        return String.format("%s#%s$%s&%s@%s!%s",
                request.clientId != null ? request.clientId : "",
                request.scope != null ? request.scope : "",
                request.redirectUri != null ? request.redirectUri : "",
                request.responseType != null ? request.responseType : "",
                request.codeChallenge != null ? request.codeChallenge : "",
                request.state != null ? request.state : "");
    }

    private NewCookie createSessionCookie(String value) {
        return new NewCookie.Builder(COOKIE_ID).value(value).path("/").httpOnly(true).maxAge(COOKIE_MAX_AGE).build();
    }

    private Response showConsentPageWithCookie(String username, AuthorizationRequest request) {
        String cookieValue = buildCookieValue(request) + "%" + username;
        NewCookie cookie = createSessionCookie(cookieValue);
        String html = loadHtmlResource("/consent.html")
                .replace("{{scope}}", request.scope)
                .replace("{{username}}", username);
        return Response.ok(html).cookie(cookie).build();
    }

    private AuthorizationRequest validateAuthorizationRequest(MultivaluedMap<String, String> params) throws InvalidRequestException {
        AuthorizationRequest req = new AuthorizationRequest();
        req.clientId = params.getFirst("client_id");
        if (req.clientId == null || req.clientId.isEmpty()) throw new InvalidRequestException("client_id required");

        var tenant = iamManager.findTenantByName(req.clientId);
        if (tenant == null) throw new InvalidRequestException("Unknown client: " + req.clientId);

        req.redirectUri = params.getFirst("redirect_uri");
        if (req.redirectUri == null) throw new InvalidRequestException("redirect_uri required");

        req.responseType = params.getFirst("response_type");
        req.codeChallenge = params.getFirst("code_challenge");
        if (req.codeChallenge == null) throw new InvalidRequestException("code_challenge required");

        req.scope = params.getFirst("scope");
        if(req.scope == null) req.scope = tenant.getRequiredScopes();
        req.state = params.getFirst("state");
        return req;
    }

    private void validatePasswords(String p, String cp) throws InvalidRequestException {
        if (p == null || !p.equals(cp)) throw new InvalidRequestException("Passwords mismatch");
    }
    private void validateEmail(String e) throws InvalidRequestException {
        if (e == null || !e.contains("@")) throw new InvalidRequestException("Invalid email");
    }
    private void validatePhone(String p) throws InvalidRequestException {
        if (p == null || p.length() < 8) throw new InvalidRequestException("Invalid phone");
    }
    private java.time.LocalDate parseBirthdate(String d) throws InvalidRequestException {
        try { return java.time.LocalDate.parse(d); } catch(Exception e) { throw new InvalidRequestException("Invalid date"); }
    }

    private String buildRedirectUri(AuthorizationRequest request, String username, String approvedScopes) throws Exception {
        AuthorizationCode authCode = new AuthorizationCode(request.clientId, username, approvedScopes, Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond(), request.redirectUri);
        String code = URLEncoder.encode(authCode.getCode(request.codeChallenge), StandardCharsets.UTF_8);
        return request.redirectUri + "?code=" + code + (request.state != null ? "&state=" + URLEncoder.encode(request.state, StandardCharsets.UTF_8) : "");
    }

    private Response redirectWithError(String uri, String err, String desc, String state) {
        try {
            UriBuilder b = UriBuilder.fromUri(uri).queryParam("error", err).queryParam("error_description", desc);
            if(state != null) b.queryParam("state", state);
            return Response.seeOther(b.build()).build();
        } catch (Exception e) { return buildErrorResponse(desc); }
    }

    private Response buildErrorResponse(String msg) {
        return Response.status(Response.Status.BAD_REQUEST).entity(escapeHtml(msg)).build();
    }

    private String loadHtmlResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) { return new String(is.readAllBytes(), StandardCharsets.UTF_8); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String escapeHtml(String text) {
        if(text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // Classes Internes
    private static class AuthorizationRequest {
        String clientId=""; String redirectUri=""; String responseType=""; String scope=""; String codeChallenge=""; String state=""; String username="";
    }
    private static class InvalidRequestException extends Exception { public InvalidRequestException(String m) { super(m); } }
    private static class InvalidCredentialsException extends Exception { public InvalidCredentialsException(String m) { super(m); } }
}