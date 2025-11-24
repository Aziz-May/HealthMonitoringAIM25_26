# Complete Setup Script for Health Monitoring IAM
# Run this step by step

Write-Host "=== Health Monitoring IAM Setup ===" -ForegroundColor Green

# Step 1: Setup MongoDB
Write-Host "`n1. Setting up MongoDB..." -ForegroundColor Yellow
mongosh --eval "
use healthmonitoring;

db.Tenant.deleteMany({});
db.Tenant.insertOne({
  '_id': 'health-monitoring-tenant',
  'name': 'health-monitoring-app',
  'redirectUri': 'http://127.0.0.1:5500/app/',
  'supportedGrantTypes': 'authorization_code',
  'requiredScopes': 'resource.read resource.write',
  'secret': null,
  'allowedRoles': null,
  'version': 0
});

db.Identity.deleteMany({});
db.Identity.insertOne({
  '_id': 'test-user-001',
  'username': 'testuser',
  'password': '\$argon2id\$v=19\$m=65536,t=3,p=4\$salthere\$hashhere',
  'roles': 1,
  'providedScopes': 'resource.read resource.write',
  'version': 0
});

print('MongoDB setup complete!');
"

# Step 2: Generate password hash
Write-Host "`n2. Generate password hash for 'Test123!'" -ForegroundColor Yellow
cd C:\Users\azizm\IdeaProjects\HealthMonitoringAIM25_26\iam
mvn compile exec:java -Dexec.mainClass="tn.supcom.cot.iam.util.PasswordHashGenerator"

Write-Host "`nCopy the hash for 'Test123!' and update MongoDB:" -ForegroundColor Cyan
Write-Host "mongosh" -ForegroundColor White
Write-Host "use healthmonitoring" -ForegroundColor White
Write-Host "db.Identity.updateOne({username:'testuser'}, {`$set:{password:'PASTE_HASH_HERE'}})" -ForegroundColor White

# Step 3: Copy WAR to WildFly
Write-Host "`n3. Deploying to WildFly..." -ForegroundColor Yellow
Copy-Item "C:\Users\azizm\IdeaProjects\HealthMonitoringAIM25_26\iam\target\iam-1.0.war" `
          "C:\Users\azizm\wildfly-preview-38.0.1.Final\wildfly-preview-38.0.1.Final\standalone\deployments\" -Force

Write-Host "`n✅ Setup complete!" -ForegroundColor Green
Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Update password hash in MongoDB" -ForegroundColor White
Write-Host "2. Restart WildFly if needed" -ForegroundColor White  
Write-Host "3. Test: http://127.0.0.1:5500/app/" -ForegroundColor White
