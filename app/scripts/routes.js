import VanillaRouter from "./router.js";
import { checkSession, handleProfilName } from "./iam.js";
import { HomePresenter } from "./home.js";
import {connectClient, toggleDevice} from "./websocket.js";

const presenters = new Map(); // To store presenters and avoid re-instantiating them

const router = new VanillaRouter({
    type: "history",
    routes: {
        "/": "dashboard",
        "/app": "dashboard",
        "/index.html": "dashboard"
    }
}).listen().on("route", async e => {
    if (checkSession()) {
        connectClient()
        console.log(e.detail.route, e.detail.url);
        // Fetch the HTML content for the route
        const mainElem = document.getElementById("mainElem");
        mainElem.innerHTML = await fetch("pages/" + e.detail.route + ".html").then(x => x.text());
        handleProfilName();

        // Change the document title
        document.title = mainElem.getElementsByTagName('title')[0].innerHTML;

        // Load presenter based on the route
        let presenter = null;
        switch (e.detail.route) {
            case 'dashboard':
                // Check if the presenter is already cached
                if (!presenters.has('dashboard')) {
                    presenter = new HomePresenter();
                } else {
                    presenter = presenters.get('dashboard'); // Use cached presenter
                }
                break;
            default:
                console.warn(`No presenter defined for route: ${e.detail.route}`);
        }

        // If we have a presenter, load its associated view
        if (presenter) {
            presenter.load(); 
            presenters.set(e.detail.route, presenter); 
        }


    } else {
        
    }
});
