import {switchToSubdomain} from './util.js';

var config = {
    client_id: "health-monitoring-app",
    // This is where the user comes back after logging in (Your Frontend URL)
    redirect_uri: "http://127.0.0.1:5500/app/", 
    
    // These point to your WildFly Backend
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
        // console.log("==================================")
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
    // Ensure the 'signin' button exists before adding the event listener
    const signinButton = document.getElementById("signin");
    console.log("Registering PKCE click listener, button found:", signinButton !== null);
    
    if (signinButton) {
        // Initiate the PKCE Auth Code flow when the link is clicked
        signinButton.addEventListener("click", async function(e) {
            e.preventDefault();
            console.log("Login button clicked! Starting OAuth flow...");

            // Create and store a random "state" value
            var state = generateRandomString();
            localStorage.setItem("pkce_state", state);

            // Create and store a new PKCE code_verifier (the plaintext random secret)
            var code_verifier = generateRandomString();
            localStorage.setItem("pkce_code_verifier", code_verifier);

            // Hash and base64-urlencode the secret to use as the challenge
            var code_challenge = await pkceChallengeFromVerifier(code_verifier);

            // Build the authorization URL
            var url = config.authorization_endpoint 
                + "?response_type=code"
                + "&client_id=" +config.client_id
                + "&state=" + encodeURIComponent(state)
                + "&scope=" + encodeURIComponent(config.requested_scopes)
                + "&redirect_uri=" + config.redirect_uri
                + "&code_challenge=" + encodeURIComponent(code_challenge)
                + "&code_challenge_method=S256";

            console.log("Redirecting to:", url);
            
            // Redirect to the authorization server
            window.location = url;
        });
    } else {
        console.error("Signin button not found in the DOM");
    }
}

////////////////////////////////////////////////////////////
// REGISTRATION REQUEST
export function registration() {
    // Ensure the 'signup' button exists before adding the event listener
    const signupButton = document.getElementById("signup");
    console.log("Registering signup click listener, button found:", signupButton !== null);
    
    if (signupButton) {
        // Initiate the registration flow when the link is clicked
        signupButton.addEventListener("click", async function (e) {
            e.preventDefault();
            console.log("Signup button clicked!");

            // Check if registration endpoint is configured
            if (!config.registration_endpoint || config.registration_endpoint === "") {
                alert("Registration is not available. Please contact the administrator to create an account.");
                console.error("Registration endpoint is not configured in the IAM server");
                return;
            }

            // Build the registration URL
            var url = config.registration_endpoint 
                + "?client_id=" + config.client_id
                + "&redirect_uri=" + config.redirect_uri;

            // Redirect to the IAM server
            window.location = url;
        });
    } else {
        console.error("Signup button not found in the DOM");
    }
}


//////////////////////////////////////////////////////////////////////
// GENERAL HELPER FUNCTIONS

// Make a POST request and parse the response as JSON
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
        // console.log("Response Body:", request.response);
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


// Parse a query string into an object
function parseQueryString(string) {
    if(string == "") { return {}; }
    var segments = string.split("&").map(s => s.split("=") );
    var queryString = {};
    segments.forEach(s => queryString[s[0]] = s[1]);
    return queryString;
}


export function handlePKCERedirect(){
    // console.log("Full URL:", window.location.href);  // Check the full URL
    // console.log("Query String:", window.location.search);  // Check just the query string
    const urlParams = new URLSearchParams(window.location.search);
    let q = {};
    urlParams.forEach((value, key) => {
        q[key] = value;
    });

    // console.log("Parsed Query String:", q);  // Verify parsed query params

    if (q.error) {
        alert("Error returned from authorization server: " + q.error);
        console.log(q.error + ": " + q.error_description);
    }
    // console.log(localStorage.getItem("pkce_code_verifier"))
    // console.log(q.code);

    if (q.code) {
        if (localStorage.getItem("pkce_state") !== q.state) {
            alert("Invalid state");
        } else {
            sendPostRequest(config.token_endpoint, {
                grant_type: "authorization_code",
                code: q.code,
                client_id: config.client_id,
                redirect_uri: config.redirect_uri,
                code_verifier: localStorage.getItem("pkce_code_verifier")
            }, function(request, body) {
                sessionStorage.setItem('accessToken', body.access_token);
                const signInEvent = new CustomEvent("signIn", { detail: body });
                document.dispatchEvent(signInEvent);
                window.history.replaceState({}, null, "/");
                location.reload();
            }, function(request, error) {
                console.log(error.error + ": " + error.error_description);
            });
        }
        
        localStorage.removeItem("pkce_state");
        localStorage.removeItem("pkce_code_verifier");
    }
}
export function handleProfilName() {
    let token = sessionStorage.getItem('accessToken');

    // Check if token exists
    if (token) {
        // Parse JWT to get username from the token payload
        try {
            let payload = parseJwt(token);
            const username = payload.upn || payload.sub; // Use 'upn' or 'sub' from JWT
            
            // Update the UI with username
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

// Generate a secure random string using the browser crypto functions
function generateRandomString() {
    var array = new Uint32Array(28);
    window.crypto.getRandomValues(array);
    return Array.from(array, dec => ('0' + dec.toString(16)).substr(-2)).join('');
}

// Calculate the SHA256 hash of the input text. 
// Returns a promise that resolves to an ArrayBuffer
function sha256(plain) {
    const encoder = new TextEncoder();
    const data = encoder.encode(plain);
    return window.crypto.subtle.digest('SHA-256', data);
}

// Base64-urlencodes the input string
function base64urlencode(str) {
    // Convert the ArrayBuffer to string using Uint8 array to conver to what btoa accepts.
    // btoa accepts chars only within ascii 0-255 and base64 encodes them.
    // Then convert the base64 encoded to base64url encoded
    //   (replace + with -, replace / with _, trim trailing =)
    return btoa(String.fromCharCode.apply(null, new Uint8Array(str)))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// Return the base64-urlencoded sha256 hash for the PKCE challenge
async function pkceChallengeFromVerifier(v) {
    let hashed = await sha256(v);
    return base64urlencode(hashed);
}
