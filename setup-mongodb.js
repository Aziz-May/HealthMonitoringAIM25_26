// MongoDB Initialization Script for Health Monitoring System
// Run this in MongoDB shell or MongoDB Compass

// Switch to database
// usehealthmonitoring;

// ============================================
// 1. CREATE TENANT (OAuth2 Client)
// ============================================
db.Tenant.insertOne({
    "_id": "tenant-health-app-001",
    "name": "health-monitoring-app",
    "secret": null, // Public client, no secret needed for PKCE
    "redirectUri": "http://127.0.0.1:5500/app/",
    "allowedRoles": NumberLong("9223372036854775807"), // All roles (Long.MAX_VALUE)
    "requiredScopes": "resource.read resource.write",
    "supportedGrantTypes": "authorization_code refresh_token",
    "version": NumberLong("0")
});

// ============================================
// 2. CREATE TEST USERS (Identities)
// ============================================

// Admin User
db.Identity.insertOne({
    "_id": "identity-admin-001",
    "username": "admin",
    // Password: Admin123! (you need to hash this with Argon2)
    // Run the PasswordHashGenerator utility to generate the hash
    "password": "$argon2id$v=19$m=65536,t=3,p=1$GENERATE_THIS_HASH",
    "roles": NumberLong("16"), // ADMIN role (adjust based on your Role enum)
    "providedScopes": "resource.read resource.write",
    "version": NumberLong("0")
});

// Doctor User
db.Identity.insertOne({
    "_id": "identity-doctor-001",
    "username": "doctor1",
    // Password: Doctor123!
    "password": "$argon2id$v=19$m=65536,t=3,p=1$GENERATE_THIS_HASH",
    "roles": NumberLong("1"), // DOCTOR role
    "providedScopes": "resource.read resource.write",
    "version": NumberLong("0")
});

// Patient User
db.Identity.insertOne({
    "_id": "identity-patient-001",
    "username": "patient1",
    // Password: Patient123!
    "password": "$argon2id$v=19$m=65536,t=3,p=1$GENERATE_THIS_HASH",
    "roles": NumberLong("4"), // PATIENT role
    "providedScopes": "resource.read",
    "version": NumberLong("0")
});

// Family User
db.Identity.insertOne({
    "_id": "identity-family-001",
    "username": "family1",
    // Password: Family123!
    "password": "$argon2id$v=19$m=65536,t=3,p=1$GENERATE_THIS_HASH",
    "roles": NumberLong("2"), // FAMILY role
    "providedScopes": "resource.read",
    "version": NumberLong("0")
});

// Sensor User (IoT Device)
db.Identity.insertOne({
    "_id": "identity-sensor-001",
    "username": "sensor1",
    // Password: Sensor123!
    "password": "$argon2id$v=19$m=65536,t=3,p=1$GENERATE_THIS_HASH",
    "roles": NumberLong("8"), // SENSOR role
    "providedScopes": "resource.write",
    "version": NumberLong("0")
});

// ============================================
// 3. CREATE SAMPLE PATIENT PROFILE
// ============================================
db.Patient.insertOne({
    "_id": "patient-profile-001",
    "identityId": "identity-patient-001",
    "firstName": "John",
    "lastName": "Doe",
    "age": 68,
    "emergencyPhone": "+216-12-345-678",
    "medicalHistory": "Hypertension, Type 2 Diabetes"
});

// ============================================
// 4. CREATE SAMPLE HEALTH RECORDS
// ============================================
db.HealthRecord.insertMany([
    {
        "_id": "record-001",
        "patientId": "patient-profile-001",
        "type": "HEART_RATE",
        "value": 75.0,
        "unit": "bpm",
        "timestamp": new Date()
    },
    {
        "_id": "record-002",
        "patientId": "patient-profile-001",
        "type": "SPO2",
        "value": 98.0,
        "unit": "%",
        "timestamp": new Date()
    },
    {
        "_id": "record-003",
        "patientId": "patient-profile-001",
        "type": "TEMPERATURE",
        "value": 36.8,
        "unit": "°C",
        "timestamp": new Date()
    },
    {
        "_id": "record-004",
        "patientId": "patient-profile-001",
        "type": "BLOOD_PRESSURE",
        "value": 120.0,
        "unit": "mmHg",
        "timestamp": new Date()
    }
]);

// ============================================
// 5. CREATE SAMPLE ALERT
// ============================================
db.Alert.insertOne({
    "_id": "alert-001",
    "patientId": "patient-profile-001",
    "message": "High temperature detected: 39.5°C",
    "severity": "HIGH",
    "resolved": false,
    "timestamp": new Date()
});

// ============================================
// 6. CREATE INDEXES FOR PERFORMANCE
// ============================================
db.Identity.createIndex({ "username": 1 }, { unique: true });
db.Tenant.createIndex({ "name": 1 }, { unique: true });
db.Patient.createIndex({ "identityId": 1 });
db.HealthRecord.createIndex({ "patientId": 1, "type": 1 });
db.HealthRecord.createIndex({ "timestamp": -1 });
db.Alert.createIndex({ "patientId": 1 });
db.Alert.createIndex({ "resolved": 1 });
db.Grant.createIndex({ "tenantId": 1, "identityId": 1 });

print("✅ Database initialized successfully!");
print("\n⚠️  IMPORTANT: Generate Argon2 password hashes using the PasswordHashGenerator utility");
print("   Location: iam/src/main/java/tn/supcom/cot/iam/util/PasswordHashGenerator.java");
print("\n🔐 Test Credentials:");
print("   Admin:    admin / Admin123!");
print("   Doctor:   doctor1 / Doctor123!");
print("   Patient:  patient1 / Patient123!");
print("   Family:   family1 / Family123!");
print("   Sensor:   sensor1 / Sensor123!");