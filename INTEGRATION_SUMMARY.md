# Frontend Integration with IAM - Summary

## ✅ Completed Changes

### 1. IAM Configuration (`app/scripts/iam.js`)
```javascript
✓ authorization_endpoint: "http://localhost:8080/iam-1.0/authorize"
✓ token_endpoint: "http://localhost:8080/iam-1.0/oauth/token"
✓ client_id: "health-monitoring-app"
✓ redirect_uri: "http://127.0.0.1:5500/"
✓ Registration endpoint validation added
✓ Profile name extraction from JWT token (no API call needed)
```

### 2. API Endpoints (`app/scripts/home.js`)
```javascript
✓ Changed from: https://api.smarthomecot.lme:8443/rest-api/sensors/
✓ Changed to: http://localhost:8080/api/sensors/
```

### 3. MQTT Configuration (`app/scripts/websocket.js`)
```javascript
✓ Username: "HealthMonitor" (was "SmartHomeCot")
✓ Password: "HealthMonitor2025*" (was "SmartHomeCot2025*")
```

---

## 🎯 How It Works Now

### Login Flow:
1. User clicks "Login" → Frontend redirects to `http://localhost:8080/iam-1.0/authorize`
2. IAM shows login page → User enters credentials
3. IAM validates → Redirects back with authorization code
4. Frontend exchanges code at `http://localhost:8080/iam-1.0/oauth/token`
5. IAM returns JWT access token
6. Frontend stores token in sessionStorage
7. Username extracted from JWT payload (`upn` or `sub` field)

### Sign Up:
- Shows alert: "Registration is not available. Please contact the administrator."
- Manual user creation in MongoDB required

---

## 🔑 Required MongoDB Data

### Tenant Document:
```javascript
{
  "_id": "tenant-001",
  "name": "health-monitoring-app",      // Must match client_id
  "redirectUri": "http://127.0.0.1:5500/",  // Must match frontend URL
  "supportedGrantTypes": "authorization_code",
  "requiredScopes": "resource.read resource.write",
  "secret": null,
  "allowedRoles": null,
  "version": 0
}
```

### Identity Document:
```javascript
{
  "_id": "user-001",
  "username": "testuser",
  "password": "<argon2-hash>",  // Use PasswordHashGenerator to create
  "roles": 1,
  "providedScopes": "resource.read resource.write",
  "version": 0
}
```

---

## 🛠️ Tools Created

1. **`SETUP_GUIDE.md`** - Complete setup instructions
2. **`database-setup.js`** - MongoDB initialization script
3. **`PasswordHashGenerator.java`** - Utility to generate Argon2 password hashes

---

## 🚀 Quick Start Commands

### Generate Password Hashes:
```bash
cd iam
mvn compile
mvn exec:java -Dexec.mainClass="tn.supcom.cot.iam.util.PasswordHashGenerator"
```

### Build IAM Server:
```bash
cd iam
mvn clean package
# Deploy target/iam-1.0.war to your app server
```

### Setup MongoDB:
```bash
mongosh
# Then run the commands from database-setup.js
```

### Run Frontend:
```bash
cd app
# Open index.html with Live Server (port 5500)
```

---

## ✅ Testing Checklist

- [ ] MongoDB running on localhost:27017
- [ ] Tenant document created with name "health-monitoring-app"
- [ ] Test user created with Argon2 hashed password
- [ ] IAM server deployed and running on http://localhost:8080/iam-1.0/
- [ ] Frontend running on http://127.0.0.1:5500/
- [ ] Click "Login" → Should redirect to IAM login page
- [ ] Enter credentials → Should redirect back with token
- [ ] Username displayed in header after login

---

## 🎉 Next Steps

After successful login:
1. Build health sensor API backend
2. Connect real IoT devices or create simulators
3. Test MQTT real-time data flow
4. Add registration endpoint to IAM
5. Create admin panel for user management

---

All SmartHome references removed and replaced with Health Monitoring specific configuration! 🏥
