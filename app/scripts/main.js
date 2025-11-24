import * as bootstrap from 'bootstrap'
import {checkSession, registerPKCEClickListener, registration, handlePKCERedirect} from './iam.js';
import "./routes.js"
import { toggleDevice } from './websocket.js';

(() => {
    'use strict'

    // Function to handle logout button visibility and functionality
    const setupLogoutButton = () => {
        const logoutButton = document.getElementById("signout");
        const profile=document.getElementById("profile")
        if (logoutButton) {
            if (
            checkSession()) {
                profile.classList.remove("d-none");
                
                // Add event listener for the logout button
                logoutButton.addEventListener('click', function() {
                    sessionStorage.removeItem('accessToken'); // Remove token from sessionStorage
                    profile.classList.add("d-none"); // Hide the button
                    console.log("Logged out and token removed.");
                    
                    // Clear main content and show welcome page
                    let mainContent = document.getElementById('mainElem');
                    mainContent.innerHTML = '';
                    mainContent.appendChild(document.getElementById('welcome-content').content.cloneNode(true));
                    
                    // Register event listeners for the login/signup buttons
                    registerPKCEClickListener();
                    registration();
                });
            } else {
                logoutButton.classList.add("d-none");
            }
        }
    }

    window.addEventListener('DOMContentLoaded', async () => {
        // Event listener to toggle theme
        document.querySelectorAll('[data-bs-theme-value]')
            .forEach(toggle => {
                toggle.addEventListener('click', () => {
                    const theme = toggle.getAttribute('data-bs-theme-value');
                    setStoredTheme(theme); // Store the selected theme
                    setTheme(theme);       // Apply the theme
                    showActiveTheme(theme, true); // Update the UI to show the active theme
                });
            });

        // Initialize Bootstrap tooltips and popovers
        [...document.querySelectorAll('[data-bs-toggle="tooltip"]')]
            .map(el => new bootstrap.Tooltip(el));
        [...document.querySelectorAll('[data-bs-toggle="popover"]')]
            .map(el => new bootstrap.Popover(el));
    });

    // Check session and handle initial page state
    if (!checkSession()) {
        let mainContent = document.getElementById('mainElem');
        mainContent.innerHTML = '';
        mainContent.appendChild(document.getElementById('welcome-content').content.cloneNode(true));
        
        // Register event listeners AFTER template is added to DOM
        registerPKCEClickListener();
        registration();
        
        handlePKCERedirect();
    } else {
        const signInEvent = new CustomEvent("signIn", { 
            detail: sessionStorage.getItem('oauth-session') 
        });
        document.dispatchEvent(signInEvent);
    }

    // Setup logout button
    setupLogoutButton();
    
    // Service Worker registration
    window.onload = () => {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('./sw.js')
                .then(function(registration) {
                    console.log('ServiceWorker registration successful with scope:', registration.scope);
                })
                .catch(function(err) {
                    console.log('Serviceworker registration failed:', err);
                });
        }
    }

})();
