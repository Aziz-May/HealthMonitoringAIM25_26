import {switchToSubdomain} from './util.js';

var config = {
    client_id: "health-monitoring-app",
    // Frontend callback URL - Update this to match your frontend server
    redirect_uri: "http://127.0.0.1:5500/app/",

    // IAM Backend Endpoints
    registration_endpoint: "http://localhost:8080/iam-1.0/rest-iam/register",
    authorization_endpoint: "http://localhost:8080/iam-1.0/rest-iam/authorize",
    token_endpoint: "http://localhost:8080/iam-1.0/rest-iam/oauth/token",
    requested_scopes: "resource.read resource.write"
};

function parseJwt(token){
    var base64Url = token.split('.')[1];
    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    var jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
}

export function checkSession(){
    let accessToken = sessionStorage.getItem('accessToken');
    if(accessToken !== null){
        let payload = parseJwt(accessToken);
        if(payload.exp < Math.round(Date.now() / 1000)){
            sessionStorage.removeItem('accessToken');
            sessionStorage.removeItem('subject');
            sessionStorage.removeItem('groups');
            return false;
        }
        sessionStorage.setItem('subject',payload.sub);
        sessionStorage.setItem('groups',payload.groups);
        return true;
    }
    return false;
}

//////////////////////////////////////////////////////////////////////
// OAUTH REQUEST
export function registerPKCEClickListener() {
    const signinButton = document.getElementById("signin");
    console.log("Registering PKCE click listener, button found:", signinButton !== null);

    if (signinButton) {
        signinButton.addEventListener("click", async function(e) {
            e.preventDefault();
            console.log("Login button clicked! Starting OAuth flow...");

            // Create and store a random "state" value
            var state = generateRandomString();
            localStorage.setItem("pkce_state", state);

            // Create and store a new PKCE code_verifier
            var code_verifier = generateRandomString();
            localStorage.setItem("pkce_code_verifier", code_verifier);

            // Hash and base64-urlencode the secret to use as the challenge
            var code_challenge = await pkceChallengeFromVerifier(code_verifier);

            // Build the authorization URL
            var url = config.authorization_endpoint
                + "?response_type=code"
                + "&client_id=" + encodeURIComponent(config.client_id)
                + "&state=" + encodeURIComponent(state)
                + "&scope=" + encodeURIComponent(config.requested_scopes)
                + "&redirect_uri=" + encodeURIComponent(config.redirect_uri)
                + "&code_challenge=" + encodeURIComponent(code_challenge)
                + "&code_challenge_method=S256";

            console.log("Redirecting to:", url);
            window.location = url;
        });
    } else {
        console.error("Signin button not found in the DOM");
    }
}

////////////////////////////////////////////////////////////
// REGISTRATION REQUEST
export function registration() {
    const signupButton = document.getElementById("signup");
    console.log("Registering signup click listener, button found:", signupButton !== null);

    if (signupButton) {
        signupButton.addEventListener("click", async function (e) {
            e.preventDefault();
            console.log("Signup button clicked! Starting registration flow...");

            if (!config.registration_endpoint || config.registration_endpoint === "") {
                alert("Registration is not available. Please contact the administrator.");
                return;
            }

            // Create and store OAuth parameters
            var state = generateRandomString();
            localStorage.setItem("pkce_state", state);

            var code_verifier = generateRandomString();
            localStorage.setItem("pkce_code_verifier", code_verifier);

            var code_challenge = await pkceChallengeFromVerifier(code_verifier);

            // Build the registration URL with OAuth parameters
            var url = config.registration_endpoint
                + "?client_id=" + encodeURIComponent(config.client_id)
                + "&redirect_uri=" + encodeURIComponent(config.redirect_uri)
                + "&scope=" + encodeURIComponent(config.requested_scopes)
                + "&response_type=code"
                + "&code_challenge=" + encodeURIComponent(code_challenge)
                + "&code_challenge_method=S256"
                + "&state=" + encodeURIComponent(state);

            console.log("Redirecting to registration:", url);
            window.location = url;
        });
    } else {
        console.error("Signup button not found in the DOM");
    }
}

//////////////////////////////////////////////////////////////////////
// GENERAL HELPER FUNCTIONS

function sendPostRequest(url, params, success, error) {
    var request = new XMLHttpRequest();
    request.open('POST', url, true);
    request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    request.onload = function() {
        var body = {};
        try {
            body = JSON.parse(request.response);
        } catch (e) {
            console.log("Error parsing response as JSON:", e);
        }
        if (request.status == 200) {
            success(request, body);
        } else {
            error(request, body);
        }
    };

    request.onerror = function() {
        console.error("Network Error");
        error(request, {});
    };

    var body = Object.keys(params)
        .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
        .join('&');
    request.send(body);
}

export function handlePKCERedirect(){
    const urlParams = new URLSearchParams(window.location.search);
    let q = {};
    urlParams.forEach((value, key) => {
        q[key] = value;
    });

    if (q.error) {
        alert("Error returned from authorization server: " + q.error);
        console.log(q.error + ": " + q.error_description);
        return;
    }

    if (q.code) {
        const storedState = localStorage.getItem("pkce_state");
        console.log("State validation - Stored:", storedState, "Received:", q.state);

        if (storedState !== q.state) {
            console.error("State mismatch! Stored:", storedState, "Received:", q.state);
            alert("Invalid state - possible CSRF attack. Please try again.");
            localStorage.removeItem("pkce_state");
            localStorage.removeItem("pkce_code_verifier");
            return;
        }

        sendPostRequest(config.token_endpoint, {
            grant_type: "authorization_code",
            code: q.code,
            client_id: config.client_id,
            redirect_uri: config.redirect_uri,
            code_verifier: localStorage.getItem("pkce_code_verifier")
        }, function(request, body) {
            console.log("Token received:", body);
            sessionStorage.setItem('accessToken', body.access_token);
            console.log("Token saved to sessionStorage");

            const signInEvent = new CustomEvent("signIn", { detail: body });
            document.dispatchEvent(signInEvent);

            // Clean up URL and reload
            console.log("Cleaning URL and reloading...");
            window.history.replaceState({}, document.title, "/app/");
            window.location.reload();
        }, function(request, error) {
            console.error("Token exchange failed:", error.error + ": " + error.error_description);
            alert("Login failed: " + (error.error_description || error.error));
        });

        localStorage.removeItem("pkce_state");
        localStorage.removeItem("pkce_code_verifier");
    }
}

export function handleProfilName() {
    let token = sessionStorage.getItem('accessToken');

    if (token) {
        try {
            let payload = parseJwt(token);
            const username = payload.upn || payload.sub;

            const nameElement = document.getElementById('name');
            const userNameElement = document.getElementById('user-name');
            const profileNameElement = document.getElementById('profile-name');

            if (nameElement) nameElement.innerText = `Hi ${username}!`;
            if (userNameElement) userNameElement.innerText = `${username}`;
            if (profileNameElement) profileNameElement.innerText = `${username}`;
        } catch (error) {
            console.error('Error parsing token:', error);
        }
    } else {
        console.log('Token not found. User may not be authenticated.');
    }
}

//////////////////////////////////////////////////////////////////////
// PKCE HELPER FUNCTIONS

function generateRandomString() {
    var array = new Uint32Array(28);
    window.crypto.getRandomValues(array);
    return Array.from(array, dec => ('0' + dec.toString(16)).substr(-2)).join('');
}

function sha256(plain) {
    const encoder = new TextEncoder();
    const data = encoder.encode(plain);
    return window.crypto.subtle.digest('SHA-256', data);
}

function base64urlencode(str) {
    return btoa(String.fromCharCode.apply(null, new Uint8Array(str)))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function pkceChallengeFromVerifier(v) {
    let hashed = await sha256(v);
    return base64urlencode(hashed);
}