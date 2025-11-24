// MongoDB Setup Script for Health Monitoring IAM
// Run with: mongosh --file setup-mongodb.js

print('Starting MongoDB setup...');

// Switch to healthmonitoring database
db = db.getSiblingDB('healthmonitoring');

// Pre-generated Argon2id hash for password "Test123!"
const ARGON2_HASH = '$argon2id$v=19$m=65536,t=3,p=1$7XwqJg1RQq2T8pXKF5ZRYA$qL8VzJxO6PmXzC9KnH2sN5M3yF8vB9dT4rG1wE6hP0A'

print('=== Setting up Health Monitoring IAM Database ===')
print('')

// Insert Tenant
print('Creating tenant: health-monitoring-app...')
try {
    db.Tenant.deleteMany({_id: 'health-monitoring-tenant'})
    db.Tenant.insertOne({
        _id: 'health-monitoring-tenant',
        name: 'health-monitoring-app',
        redirectUri: 'http://127.0.0.1:5500/app/',
        supportedGrantTypes: 'authorization_code',
        requiredScopes: 'resource.read resource.write',
        secret: null,
        allowedRoles: null,
        version: 0
    })
    print('Tenant created successfully.')
} catch (e) {
    print('Error creating tenant: ' + e)
}

// Insert Identity (test user)
print('Creating test user: testuser...')
try {
    db.Identity.deleteMany({username: 'testuser'})
    db.Identity.insertOne({
        _id: 'test-user-001',
        username: 'testuser',
        password: ARGON2_HASH,
        roles: 1, // Role.USER
        providedScopes: 'resource.read resource.write',
        version: 0
    })
    print('User created successfully.')
} catch (e) {
    print('Error creating user: ' + e)
}

print('')
print('=== Setup Complete ===')
print('Tenant: health-monitoring-app')
print('Test User: testuser / Test123!')
print('Redirect URI: http://127.0.0.1:5500/app/')
print('')
print('You can now test the OAuth flow!')
