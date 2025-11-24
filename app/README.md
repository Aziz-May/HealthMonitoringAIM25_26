# Smart Health Monitoring System - Frontend Application

## Overview
The Smart Health Monitoring System is a Progressive Web App (PWA) designed for continuous, real-time patient health monitoring. Built using Vanilla JavaScript and following the MVP (Model-View-Presenter) architectural pattern, this application provides a robust, scalable solution for healthcare providers and family members to monitor patients remotely.

## Key Features

### 🏥 Real-Time Vital Monitoring
- **Heart Rate**: Continuous BPM tracking with real-time updates
- **SpO2**: Blood oxygen saturation monitoring
- **Blood Pressure**: Systolic/Diastolic pressure readings
- **Body Temperature**: Core temperature monitoring

### 🚨 Emergency Systems
- **Fall Detection**: AI-powered fall detection with instant alerts
- **SOS Button**: Manual emergency alert system
- **GPS Tracking**: Real-time location tracking for emergency response

### 🔔 Notification System
- Real-time health alerts
- Fall detection notifications
- Emergency status updates

### 👤 User Management
- PKCE Authentication flow
- Secure session management
- User profile display

## Architecture

### Design Pattern: MVP (Model-View-Presenter)
The application follows a clean separation of concerns:

- **Model**: Handles data fetching, business logic, and API communication
- **View**: Manages UI rendering and user input
- **Presenter**: Coordinates between Model and View

### Technology Stack
- **Frontend**: Vanilla JavaScript (ES6+)
- **Communication**: MQTT (HiveMQ) + REST APIs
- **Authentication**: PKCE (Proof Key for Code Exchange)
- **Styling**: Bootstrap 5 + Custom CSS
- **PWA**: Service Workers for offline capability

## Project Structure

```
app/
├── index.html                 # Main entry point
├── pwa.webmanifest           # PWA manifest
├── sw.js                     # Service Worker
├── pages/
│   └── dashboard.html        # Dashboard view
├── scripts/
│   ├── events.js            # Event system (Broker, Observable)
│   ├── mvp.js               # MVP framework implementation
│   ├── router.js            # SPA routing (History API)
│   ├── util.js              # Utility functions
│   ├── websocket.js         # MQTT client configuration
│   ├── iam.js               # Authentication (PKCE)
│   ├── home.js              # Health monitoring logic (Presenter)
│   ├── main.js              # Application bootstrap
│   └── routes.js            # Route configuration
├── styles/
│   └── main.css             # Application styles
└── images/
    └── icons/               # PWA icons
```

## Core Components

### 1. Event System (`events.js`)
- **Emitter**: Custom event dispatcher
- **IntentEvent**: User action events (Display, Create, Update, Delete)
- **StateChangeEvent**: Model state change events (Loaded, Created, Updated)
- **MVPEvent**: Wrapper for MVP events

### 2. MVP Framework (`mvp.js`)
- **Observable**: Subject in Observer pattern
- **ComputedObservable**: Derived state management
- **Model**: Data layer with API integration
- **View**: UI layer with data binding
- **Presenter**: Coordination layer

### 3. Router (`router.js`)
- History API-based SPA routing
- Declarative route configuration
- Event-driven navigation

### 4. MQTT Communication (`websocket.js`)
- HiveMQ Cloud integration
- Real-time vital signs updates
- Emergency alert handling
- Fall detection events

### 5. Authentication (`iam.js`)
- PKCE flow implementation
- Token management
- Session validation
- User profile fetching

### 6. Health Monitoring (`home.js`)
- **HomeModel**: Fetches sensor data from API
- **HomeView**: Renders health dashboard
- **HomePresenter**: Coordinates updates and event handling

## API Integration

### REST Endpoints
```javascript
// Sensor data retrieval
GET https://api.smarthomecot.lme:8443/rest-api/sensors/most-recent/{endpoint}
Headers: Authorization: Bearer {token}

// Supported endpoints:
- heartrate
- spo2
- bloodpressure
- bodytemperature
```

### MQTT Topics
```
smarthealth/vitals          # Real-time vital signs
smarthealth/alerts          # Health alerts
smarthealth/emergency       # Emergency notifications
smarthealth/devices/*       # Device control
```

## Best Practices Implemented

### 1. **Separation of Concerns**
   - Clear distinction between data (Model), presentation (View), and logic (Presenter)
   - Each component has a single responsibility

### 2. **Event-Driven Architecture**
   - Loose coupling through event bus (Broker pattern)
   - Observable pattern for reactive state management

### 3. **Security**
   - PKCE authentication flow
   - Secure token storage in sessionStorage
   - CORS and credential management

### 4. **Progressive Enhancement**
   - Service Worker for offline capability
   - Responsive design (mobile-first)
   - PWA installability

### 5. **Code Organization**
   - Modular ES6 imports
   - Clear file structure
   - Reusable utility functions

### 6. **Performance**
   - Lazy loading of routes
   - Presenter caching
   - Efficient DOM updates
   - Service Worker caching

### 7. **Error Handling**
   - Try-catch blocks for API calls
   - Graceful degradation
   - User-friendly error messages

### 8. **Real-Time Updates**
   - WebSocket (MQTT) for live data
   - Periodic polling fallback (10s interval)
   - Automatic reconnection

## Running the Application

### Prerequisites
- Modern web browser with JavaScript enabled
- HTTPS connection (required for Service Workers)
- Network access to:
  - IAM Server: `iam.smarthomecot.lme:8443`
  - API Server: `api.smarthomecot.lme:8443`
  - MQTT Broker: HiveMQ Cloud

### Development Setup
1. Serve the application using a local HTTPS server
2. Ensure proper DNS configuration for subdomains
3. Configure MQTT broker credentials in `websocket.js`

### Authentication Flow
1. User clicks "Login" → Redirected to IAM server
2. IAM authenticates → Returns authorization code
3. Application exchanges code for access token (PKCE)
4. Token stored in sessionStorage
5. Protected routes become accessible

## Customization

### Adding New Vitals
1. Update `HomeModel.loadModel()` with new API endpoint
2. Add corresponding HTML element in `dashboard.html`
3. Update MQTT handler in `websocket.js` if real-time

### Adding New Routes
1. Define route in `routes.js`
2. Create HTML page in `pages/`
3. Create Presenter if needed
4. Update Service Worker cache

### Styling
- Modify `styles/main.css` for theme changes
- Bootstrap 5 utilities available
- CSS variables for consistent theming

## Security Considerations
- All API calls require Bearer token authentication
- PKCE prevents authorization code interception
- MQTT uses TLS encryption (port 8884)
- Sensitive data never logged to console in production

## Browser Support
- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Opera 76+

## Future Enhancements
- [ ] GPS map integration (Google Maps / OpenStreetMap)
- [ ] Heart attack prediction visualization
- [ ] Historical data charts (Chart.js)
- [ ] Push notifications (Web Push API)
- [ ] Offline mode for cached data
- [ ] Multi-language support (i18n)
- [ ] Dark mode theme

## License
Copyright © 2025 Smart Health Team. All rights reserved.

## Support
For issues or questions, contact the development team or refer to the main project documentation.
