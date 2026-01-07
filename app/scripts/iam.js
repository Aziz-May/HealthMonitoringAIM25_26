import { switchToSubdomain } from './util.js';

const config = {
    client_id: "health-monitoring-app",
    // Assurez-vous d'utiliser 127.0.0.1 pour éviter les conflits de LocalStorage avec localhost
    redirect_uri: "http://127.0.0.1:5500/app/",

    // Endpoints du Backend Jakarta
    registration_endpoint: "http://localhost:8080/iam-1.0/rest-iam/register",
    authorization_endpoint: "http://localhost:8080/iam-1.0/rest-iam/authorize",
    token_endpoint: "http://localhost:8080/iam-1.0/rest-iam/oauth/token",
    requested_scopes: "resource.read resource.write"
};


function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

/**
 * Vérifie si une session est active et valide
 */
export function checkSession() {
    const accessToken = sessionStorage.getItem('accessToken');
    if (accessToken) {
        const payload = parseJwt(accessToken);
        // Si le token est expiré
        if (!payload || payload.exp < Math.round(Date.now() / 1000)) {
            clearSession();
            return false;
        }
        // Met à jour les infos de session au cas où
        sessionStorage.setItem('subject', payload.sub);
        sessionStorage.setItem('groups', JSON.stringify(payload.groups));

        // Relance le rafraîchissement automatique si nécessaire au rechargement de la page
        scheduleTokenRefresh(accessToken);
        return true;
    }
    return false;
}

/**
 * Nettoie proprement la session
 */
export function clearSession() {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('subject');
    sessionStorage.removeItem('groups');
    console.log("Session cleared.");
}

/**
 * RAFFRAICHISSEMENT AUTOMATIQUE (Silent Refresh)
 */
async function silentRefresh() {
    const refreshToken = sessionStorage.getItem('refreshToken');
    const accessToken = sessionStorage.getItem('accessToken');

    if (!refreshToken || !accessToken) return;

    console.log("Initiating silent token refresh...");

    sendPostRequest(config.token_endpoint, {
        grant_type: "refresh_token",
        // Votre backend attend l'ancien access_token dans 'code' et le refresh dans 'code_verifier'
        code: accessToken,
        code_verifier: refreshToken
    }, function(request, body) {
        console.log("Token successfully refreshed.");
        sessionStorage.setItem('accessToken', body.access_token);
        sessionStorage.setItem('refreshToken', body.refresh_token);

        // Planifie le prochain rafraîchissement
        scheduleTokenRefresh(body.access_token);
    }, function(request, error) {
        console.error("Refresh failed, logging out...", error);
        clearSession();
        window.location.reload();
    });
}

function scheduleTokenRefresh(token) {
    const payload = parseJwt(token);
    if (!payload) return;

    const bufferTime = 60 * 1000; // Rafraîchir 1 minute avant l'expiration
    const expiresAt = payload.exp * 1000;
    const delay = expiresAt - Date.now() - bufferTime;

    if (delay > 0) {
        // Supprimer l'ancien timer s'il existe
        if (window.tokenRefreshTimer) clearTimeout(window.tokenRefreshTimer);
        window.tokenRefreshTimer = setTimeout(silentRefresh, delay);
        console.log(`Next refresh scheduled in ${Math.round(delay/1000)}s`);
    } else {
        // Si on est déjà dans la dernière minute, on rafraîchit tout de suite
        silentRefresh();
    }
}

/**
 * FLUX LOGIN (PKCE)
 */
export function registerPKCEClickListener() {
    const signinButton = document.getElementById("signin");
    if (signinButton) {
        signinButton.addEventListener("click", async function(e) {
            e.preventDefault();

            const state = generateRandomString();
            const code_verifier = generateRandomString();
            localStorage.setItem("pkce_state", state);
            localStorage.setItem("pkce_code_verifier", code_verifier);

            const code_challenge = await pkceChallengeFromVerifier(code_verifier);

            const url = config.authorization_endpoint
                + "?response_type=code"
                + "&client_id=" + encodeURIComponent(config.client_id)
                + "&state=" + encodeURIComponent(state)
                + "&scope=" + encodeURIComponent(config.requested_scopes)
                + "&redirect_uri=" + encodeURIComponent(config.redirect_uri)
                + "&code_challenge=" + encodeURIComponent(code_challenge)
                + "&code_challenge_method=S256";

            window.location = url;
        });
    }
}

/**
 * FLUX INSCRIPTION
 */
export function registration() {
    const signupButton = document.getElementById("signup");
    if (signupButton) {
        signupButton.addEventListener("click", async function (e) {
            e.preventDefault();

            const state = generateRandomString();
            const code_verifier = generateRandomString();
            localStorage.setItem("pkce_state", state);
            localStorage.setItem("pkce_code_verifier", code_verifier);

            const code_challenge = await pkceChallengeFromVerifier(code_verifier);

            const url = config.registration_endpoint
                + "?client_id=" + encodeURIComponent(config.client_id)
                + "&redirect_uri=" + encodeURIComponent(config.redirect_uri)
                + "&scope=" + encodeURIComponent(config.requested_scopes)
                + "&response_type=code"
                + "&code_challenge=" + encodeURIComponent(code_challenge)
                + "&code_challenge_method=S256"
                + "&state=" + encodeURIComponent(state);

            window.location = url;
        });
    }
}

/**
 * GESTION DU RETOUR (REDIRECT HANDLER)
 */
export function handlePKCERedirect() {
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const state = urlParams.get('state');

    if (code) {
        const storedState = localStorage.getItem("pkce_state");

        // PROTECTION CSRF
        if (!storedState || storedState !== state) {
            console.error("CSRF Warning: State mismatch!");
            alert("Security error: Invalid state. Please login again.");
            localStorage.removeItem("pkce_state");
            localStorage.removeItem("pkce_code_verifier");
            return;
        }

        sendPostRequest(config.token_endpoint, {
            grant_type: "authorization_code",
            code: code,
            client_id: config.client_id,
            redirect_uri: config.redirect_uri,
            code_verifier: localStorage.getItem("pkce_code_verifier")
        }, function(request, body) {
            // Sauvegarde des tokens
            sessionStorage.setItem('accessToken', body.access_token);
            sessionStorage.setItem('refreshToken', body.refresh_token);

            const payload = parseJwt(body.access_token);
            sessionStorage.setItem('subject', payload.sub);
            sessionStorage.setItem('groups', JSON.stringify(payload.groups));

            // Planifie le rafraîchissement
            scheduleTokenRefresh(body.access_token);

            // Nettoyage de l'URL et du LocalStorage
            localStorage.removeItem("pkce_state");
            localStorage.removeItem("pkce_code_verifier");
            window.history.replaceState({}, document.title, "/app/");
            window.location.reload();
        }, function(request, error) {
            alert("Authentication failed: " + (error.error_description || "Server error"));
        });
    }
}

/**
 * AFFICHAGE DU PROFIL
 */
export function handleProfilName() {
    const accessToken = sessionStorage.getItem('accessToken');
    if (accessToken) {
        const payload = parseJwt(accessToken);
        const username = payload.upn || payload.sub;

        const nameElement = document.getElementById('name');
        const userNameElement = document.getElementById('user-name');
        const profileNameElement = document.getElementById('profile-name');

        if (nameElement) nameElement.innerText = `Hi ${username}!`;
        if (userNameElement) userNameElement.innerText = username;
        if (profileNameElement) profileNameElement.innerText = username;
    }
}

/**
 * UTILITAIRES REQUETES & CRYPTO
 */
function sendPostRequest(url, params, success, error) {
    const request = new XMLHttpRequest();
    request.open('POST', url, true);
    request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    request.onload = function() {
        let body = {};
        try { body = JSON.parse(request.response); } catch (e) {}
        if (request.status == 200) success(request, body);
        else error(request, body);
    };
    request.onerror = () => error(request, {});

    const body = Object.keys(params)
        .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
        .join('&');
    request.send(body);
}

function generateRandomString() {
    const array = new Uint32Array(28);
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
    const hashed = await sha256(v);
    return base64urlencode(hashed);
}