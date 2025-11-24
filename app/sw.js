const cacheName = "smarthealth-v1";
const assets = [
  "/",
  "/scripts/events.js",
  "/scripts/iam.js",
  "/scripts/router.js",
  "/scripts/main.js", 
  "/scripts/home.js",
  "/scripts/mvp.js",
  "/scripts/routes.js",
  "/scripts/util.js",
  "/scripts/websocket.js",
  "/pages/dashboard.html",
  "/styles/main.css"
  // Icons temporarily disabled - create these later:
  // "/images/icons/android-chrome-192x192.png",
  // "/images/icons/android-chrome-512x512.png",
  // "/images/icons/apple-touch-icon.png",
  // "/images/icons/favicon-16x16.png",
  // "/images/icons/favicon-32x32.png",
  // "/images/logo.png",
  // "/images/profile.png"
];

// Clear old caches
caches.keys().then(function(names) {
  for (let name of names) {
    if (name !== cacheName) {
      caches.delete(name);
    }
  }
});

self.addEventListener("install", installEvent => {
installEvent.waitUntil(
  caches.open(cacheName).then(cache => {
    cache.addAll(assets).then(() =>{} );
  })
);
});

self.addEventListener("fetch", fetchEvent => {
fetchEvent.respondWith(
  caches.match(fetchEvent.request).then(res => {
    return res || fetch(fetchEvent.request);
  })
);
});
