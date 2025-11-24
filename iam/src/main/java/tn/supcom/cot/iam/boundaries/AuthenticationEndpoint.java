package tn.supcom.cot.iam.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;


@Path("/")
@RequestScoped
public class AuthenticationEndpoint {
    public static final String CHALLENGE_RESPONSE_COOKIE_ID = "signInId";
    @Inject
    private Logger logger;

    @Inject
    PhoenixIAMManager phoenixIAMManager;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/authorize")
    public Response authorize(@Context UriInfo uriInfo) {
        var params = uriInfo.getQueryParameters();
        //1. Check tenant
        var clientId = params.getFirst("client_id");
        if (clientId == null || clientId.isEmpty()) {
            return informUserAboutError("Invalid client_id :" + clientId);
        }
        var tenant = phoenixIAMManager.findTenantByName(clientId);
        if (tenant == null) {
            return informUserAboutError("Invalid client_id :" + clientId);
        }
        //2. Client Authorized Grant Type
        if (tenant.getSupportedGrantTypes() != null && !tenant.getSupportedGrantTypes().contains("authorization_code")) {
            return informUserAboutError("Authorization Grant type, authorization_code, is not allowed for this tenant :" + clientId);
        }
        //3. redirectUri
        String redirectUri = params.getFirst("redirect_uri");
        if (tenant.getRedirectUri() != null && !tenant.getRedirectUri().isEmpty()) {
            if (redirectUri != null && !redirectUri.isEmpty() && !tenant.getRedirectUri().equals(redirectUri)) {
                //sould be in the client.redirectUri
                return informUserAboutError("redirect_uri is pre-registred and should match");
            }
            redirectUri = tenant.getRedirectUri();
        } else {
            if (redirectUri == null || redirectUri.isEmpty()) {
                return informUserAboutError("redirect_uri is not pre-registred and should be provided");
            }
        }

        //4. response_type
        String responseType = params.getFirst("response_type");
        if (!"code".equals(responseType) && !"token".equals(responseType)) {
            String error = "invalid_grant :" + responseType + ", response_type params should be code or token:";
            return informUserAboutError(error);
        }

        //5. check scope
        String requestedScope = params.getFirst("scope");
        if (requestedScope == null || requestedScope.isEmpty()) {
            requestedScope = tenant.getRequiredScopes();
        }
        //6. code_challenge_method must be S256
        String codeChallengeMethod = params.getFirst("code_challenge_method");
        if(codeChallengeMethod==null || !codeChallengeMethod.equals("S256")){
            String error = "invalid_grant :" + codeChallengeMethod + ", code_challenge_method must be 'S256'";
            return informUserAboutError(error);
        }
        StreamingOutput stream = output -> {
            try (InputStream is = Objects.requireNonNull(getClass().getResource("/login.html")).openStream()){
                output.write(is.readAllBytes());
            }
        };
        return Response.ok(stream).location(uriInfo.getBaseUri().resolve("/login/authorization"))
                .cookie(new NewCookie.Builder(CHALLENGE_RESPONSE_COOKIE_ID)
                        .httpOnly(true)
                        .secure(false) // Changed to false for HTTP
                        .sameSite(NewCookie.SameSite.LAX) // Changed to LAX for better compatibility
                        .path("/") // Added path
                        .maxAge(600) // Added max age (10 minutes)
                        .value(tenant.getName()+"#"+requestedScope+"$"+redirectUri+"&"+params.getFirst("response_type")+"@"+params.getFirst("code_challenge")+"!"+params.getFirst("state"))
                        .build()).build();
    }

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public Response registerPage(@Context UriInfo uriInfo) {
        StreamingOutput stream = output -> {
            try (InputStream is = Objects.requireNonNull(getClass().getResource("/register.html")).openStream()){
                output.write(is.readAllBytes());
            }
        };
        return Response.ok(stream).build();
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@FormParam("username") String username,
                             @FormParam("password") String password,
                             @FormParam("confirm_password") String confirmPassword,
                             @FormParam("client_id") String clientId,
                             @FormParam("redirect_uri") String redirectUri,
                             @FormParam("scope") String scope,
                             @FormParam("response_type") String responseType,
                             @FormParam("code_challenge") String codeChallenge,
                             @FormParam("state") String state) {
        
        if (!password.equals(confirmPassword)) {
             return informUserAboutError("Passwords do not match");
        }
        
        try {
            Identity newIdentity = phoenixIAMManager.createIdentity(username, password);
            
            // After registration, show consent page (only time user will see it)
            if (clientId != null && redirectUri != null) {
                // Create cookie with OAuth params - store username for the grant
                // Format: clientId#scope$redirectUri&responseType@codeChallenge!state%username
                String cookieValue = clientId + "#" + (scope != null ? scope : "openid") + "$" 
                    + redirectUri + "&" + (responseType != null ? responseType : "code") 
                    + "@" + (codeChallenge != null ? codeChallenge : "") 
                    + "!" + (state != null ? state : "")
                    + "%" + username; // Add username to cookie
                NewCookie cookie = new NewCookie.Builder(CHALLENGE_RESPONSE_COOKIE_ID)
                        .value(cookieValue)
                        .path("/")
                        .httpOnly(true)
                        .secure(false)
                        .maxAge(600)
                        .build();
                
                // Load consent page
                StreamingOutput stream = output -> {
                    try (InputStream is = Objects.requireNonNull(getClass().getResource("/consent.html")).openStream()){
                        String html = new String(is.readAllBytes());
                        html = html.replace("{{scope}}", scope != null ? scope : "openid")
                                   .replace("{{username}}", username);
                        output.write(html.getBytes());
                    }
                };
                return Response.ok(stream).cookie(cookie).build();
            }
            
            return Response.ok("Registration successful! You can now login.").build();
            
        } catch (Exception e) {
            return informUserAboutError("Registration failed: " + e.getMessage());
        }
    }

    private String cipher(String codeChallenge) {
        return null;
    }

    @POST
    @Path("/login/authorization")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response login(@CookieParam(CHALLENGE_RESPONSE_COOKIE_ID) Cookie cookie,
                          @FormParam("username")String username,
                          @FormParam("password")String password,
                          @Context UriInfo uriInfo) throws Exception {
        Identity identity = phoenixIAMManager.findIdentityByName(username);
        if(Argon2Utility.check(identity.getPassword(),password.toCharArray())){
            logger.info("Authenticated identity:"+username);
            
            // Parse OAuth params from cookie: tenant#scope$redirectUri&responseType@codeChallenge!state
            String cookieValue = cookie.getValue();
            logger.info("LOGIN DEBUG - Cookie: " + cookieValue);
            
            String[] parts = cookieValue.split("\\$");
            String clientId = cookieValue.split("#")[0];
            String requestedScope = cookieValue.split("#")[1].split("\\$")[0];
            String redirectUri = parts[1].split("&")[0];
            
            String responseType = "code";
            String codeChallenge = null;
            String state = null;
            
            if (parts.length > 1 && parts[1].contains("&")) {
                String[] oauthParams = parts[1].split("&");
                if (oauthParams.length > 1) {
                    responseType = oauthParams[1].split("@")[0];
                    if (oauthParams[1].contains("@")) {
                        String afterAt = oauthParams[1].split("@")[1];
                        codeChallenge = afterAt.split("!")[0];
                        if (afterAt.contains("!")) {
                            state = afterAt.split("!")[1];
                        }
                    }
                }
            }
            
            logger.info("LOGIN DEBUG - Parsed state: '" + state + "'");
            logger.info("LOGIN DEBUG - Code challenge: " + codeChallenge);
            
            // Login NEVER shows consent - always redirect directly
            String redirectURI = buildActualRedirectURI(
                    redirectUri, responseType,
                    clientId,
                    username,
                    requestedScope,
                    codeChallenge, state
            );
            logger.info("LOGIN DEBUG - Redirect URI: " + redirectURI);
            return Response.seeOther(UriBuilder.fromUri(redirectURI).build()).build();
        } else {
            logger.info("Failure when authenticating identity:"+username);
            URI location = UriBuilder.fromUri(cookie.getValue().split("\\$")[1].split("&")[0])
                    .queryParam("error", "Invalid credentials.")
                    .queryParam("error_description", "Invalid username or password.")
                    .build();
            return Response.seeOther(location).build();
        }
    }

    @POST
    @Path("/login/authorization/consent")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response grantConsentPost(@CookieParam(CHALLENGE_RESPONSE_COOKIE_ID) Cookie cookie,
                                     @FormParam("approved_scope") String scope,
                                     @FormParam("approval_status") String approvalStatus,
                                     @FormParam("username") String username){
        return grantConsent(cookie, scope, approvalStatus, username);
    }

    @PATCH
    @Path("/login/authorization")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response grantConsent(@CookieParam(CHALLENGE_RESPONSE_COOKIE_ID) Cookie cookie,
                                 @FormParam("approved_scope") String scope,
                                 @FormParam("approval_status") String approvalStatus,
                                 @FormParam("username") String username){
        try {
            logger.info("=== CONSENT DEBUG ===");
            logger.info("Cookie value: " + cookie.getValue());
            logger.info("Approved scope: " + scope);
            logger.info("Approval status: " + approvalStatus);
            logger.info("Username from form: " + username);
            
            // Parse cookie: clientId#scope$redirectUri&responseType@codeChallenge!state%username
            String cookieValue = cookie.getValue();
            
            // First split by % to get username (if it exists - added during registration)
            String[] usernameParts = cookieValue.split("%");
            String usernameFromCookie = (usernameParts.length > 1) ? usernameParts[1] : username;
            String oauthPart = usernameParts[0];
            
            String[] parts = oauthPart.split("\\$");
            
            logger.info("Parts after $ split: " + String.join(" | ", parts));
            logger.info("Username to use: " + usernameFromCookie);
            
            // Extract redirect URI and remaining OAuth params
            String redirectUriAndParams = parts[1];
            String[] uriAndParams = redirectUriAndParams.split("&", 2);
            String redirectUri = uriAndParams[0];
            
            logger.info("Redirect URI: " + redirectUri);
            logger.info("Remaining params: " + (uriAndParams.length > 1 ? uriAndParams[1] : "NONE"));
            
            // Parse responseType@codeChallenge!state
            String responseType = "code";
            String codeChallenge = "";
            String state = "";
            
            if (uriAndParams.length > 1) {
                String remainingParams = uriAndParams[1]; // "responseType@codeChallenge!state"
                String[] typeAndRest = remainingParams.split("@", 2);
                responseType = typeAndRest[0];
                
                if (typeAndRest.length > 1) {
                    String challengeAndState = typeAndRest[1]; // "codeChallenge!state"
                    String[] challengeStateParts = challengeAndState.split("!", 2);
                    codeChallenge = challengeStateParts[0];
                    if (challengeStateParts.length > 1) {
                        state = challengeStateParts[1];
                    }
                }
            }
            
            logger.info("Parsed - responseType: " + responseType + ", codeChallenge: " + codeChallenge + ", state: " + state);
            
            if ("NO".equals(approvalStatus)) {
                var location = UriBuilder.fromUri(redirectUri)
                        .queryParam("error", "User doesn't approved the request.")
                        .queryParam("error_description", "User doesn't approved the request.")
                        .build();
                return Response.seeOther(location).build();
            }
            //==> YES
            List<String> approvedScopes = Arrays.stream(scope.split(" ")).toList();
            if (approvedScopes.isEmpty()) {
                var location = UriBuilder.fromUri(redirectUri)
                        .queryParam("error", "User hasn't approved the request.")
                        .queryParam("error_description", "User hasn't approved the request.")
                        .build();
                return Response.seeOther(location).build();
            }
            
            String clientId = cookieValue.split("#")[0];
            
            // Save the grant so user won't see consent page again - use username from cookie
            phoenixIAMManager.saveGrant(clientId, usernameFromCookie, String.join(" ", approvedScopes));
            
            // Ensure we have a valid codeChallenge (can't be empty for PKCE)
            if (codeChallenge == null || codeChallenge.isEmpty()) {
                logger.warning("Missing code_challenge!");
                var location = UriBuilder.fromUri(redirectUri)
                        .queryParam("error", "invalid_request")
                        .queryParam("error_description", "Missing code_challenge parameter")
                        .build();
                return Response.seeOther(location).build();
            }
            
            return Response.seeOther(UriBuilder.fromUri(buildActualRedirectURI(
                    redirectUri, responseType,
                    clientId, usernameFromCookie, String.join(" ", approvedScopes), 
                    codeChallenge, 
                    state.isEmpty() ? null : state
            )).build()).build();
        } catch (Exception e) {
            logger.severe("Error in grantConsent: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String buildActualRedirectURI(String redirectUri,String responseType,String clientId,String userId,String approvedScopes,String codeChallenge,String state) throws Exception {
        var sb = new StringBuilder(redirectUri);
        if ("code".equals(responseType)) {
            var authorizationCode = new AuthorizationCode(clientId,userId,
                    approvedScopes, Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond(),redirectUri);
            sb.append("?code=").append(URLEncoder.encode(authorizationCode.getCode(codeChallenge), StandardCharsets.UTF_8));
        } else {
            //Implicit: responseType=token : Not Supported
            return null;
        }
        if (state != null) {
            sb.append("&state=").append(state);
        }
        return sb.toString();
    }

    private String checkUserScopes(String userScopes, String requestedScope) {
        Set<String> allowedScopes = new LinkedHashSet<>();
        Set<String> rScopes = new HashSet<>(Arrays.asList(requestedScope.split(" ")));
        Set<String> uScopes = new HashSet<>(Arrays.asList(userScopes.split(" ")));
        for (String scope : uScopes) {
            if (rScopes.contains(scope)) allowedScopes.add(scope);
        }
        return String.join( " ", allowedScopes);
    }

    private Response informUserAboutError(String error) {
        return Response.status(Response.Status.BAD_REQUEST).entity("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"/>
                    <title>Error</title>
                </head>
                <body>
                <aside class="container">
                    <p>%s</p>
                </aside>
                </body>
                </html>
                """.formatted(error)).build();
    }
}
