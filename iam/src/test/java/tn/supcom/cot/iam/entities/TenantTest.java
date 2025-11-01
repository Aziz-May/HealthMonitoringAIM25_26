package tn.supcom.cot.iam.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tenant Entity Tests")
class TenantTest {

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
    }

    @Test
    @DisplayName("Should set and get id correctly")
    void testIdGetterSetter() {
        String testId = "tenant-123";
        tenant.setId(testId);
        assertEquals(testId, tenant.getId());
    }

    @Test
    @DisplayName("Should initialize version to 0")
    void testVersionInitialization() {
        assertEquals(0L, tenant.getVersion());
    }

    @Test
    @DisplayName("Should increment version when current version matches")
    void testVersionIncrement() {
        assertEquals(0L, tenant.getVersion());
        tenant.setVersion(0L);
        assertEquals(1L, tenant.getVersion());
        tenant.setVersion(1L);
        assertEquals(2L, tenant.getVersion());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when version mismatch")
    void testVersionMismatch() {
        tenant.setVersion(0L);
        assertThrows(IllegalStateException.class, () -> tenant.setVersion(0L));
    }

    @Test
    @DisplayName("Should set and get name correctly")
    void testNameGetterSetter() {
        String name = "Test Tenant";
        tenant.setName(name);
        assertEquals(name, tenant.getName());
    }

    @Test
    @DisplayName("Should set and get secret correctly")
    void testSecretGetterSetter() {
        String secret = "super-secret-key-123";
        tenant.setSecret(secret);
        assertEquals(secret, tenant.getSecret());
    }

    @Test
    @DisplayName("Should set and get redirect URI correctly")
    void testRedirectUriGetterSetter() {
        String redirectUri = "https://example.com/callback";
        tenant.setRedirectUri(redirectUri);
        assertEquals(redirectUri, tenant.getRedirectUri());
    }

    @Test
    @DisplayName("Should set and get allowed roles correctly")
    void testAllowedRolesGetterSetter() {
        Long allowedRoles = 31L;
        tenant.setAllowedRoles(allowedRoles);
        assertEquals(allowedRoles, tenant.getAllowedRoles());
    }

    @Test
    @DisplayName("Should set and get required scopes correctly")
    void testRequiredScopesGetterSetter() {
        String scopes = "openid profile email";
        tenant.setRequiredScopes(scopes);
        assertEquals(scopes, tenant.getRequiredScopes());
    }

    @Test
    @DisplayName("Should set and get supported grant types correctly")
    void testSupportedGrantTypesGetterSetter() {
        String grantTypes = "authorization_code refresh_token";
        tenant.setSupportedGrantTypes(grantTypes);
        assertEquals(grantTypes, tenant.getSupportedGrantTypes());
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        tenant.setName(null);
        tenant.setSecret(null);
        tenant.setRedirectUri(null);
        tenant.setAllowedRoles(null);
        tenant.setRequiredScopes(null);
        tenant.setSupportedGrantTypes(null);

        assertNull(tenant.getName());
        assertNull(tenant.getSecret());
        assertNull(tenant.getRedirectUri());
        assertNull(tenant.getAllowedRoles());
        assertNull(tenant.getRequiredScopes());
        assertNull(tenant.getSupportedGrantTypes());
    }

    @Test
    @DisplayName("Should create complete tenant object")
    void testCompleteTenantCreation() {
        tenant.setId("tenant-001");
        tenant.setName("My Application");
        tenant.setSecret("client-secret-xyz");
        tenant.setRedirectUri("https://myapp.com/oauth/callback");
        tenant.setAllowedRoles(7L);
        tenant.setRequiredScopes("openid profile");
        tenant.setSupportedGrantTypes("authorization_code client_credentials");

        assertEquals("tenant-001", tenant.getId());
        assertEquals("My Application", tenant.getName());
        assertEquals("client-secret-xyz", tenant.getSecret());
        assertEquals("https://myapp.com/oauth/callback", tenant.getRedirectUri());
        assertEquals(7L, tenant.getAllowedRoles());
        assertEquals("openid profile", tenant.getRequiredScopes());
        assertEquals("authorization_code client_credentials", tenant.getSupportedGrantTypes());
        assertEquals(0L, tenant.getVersion());
    }

    @Test
    @DisplayName("Should handle multiple redirect URIs")
    void testMultipleRedirectUris() {
        String multipleUris = "https://app.com/callback,https://app.com/callback2";
        tenant.setRedirectUri(multipleUris);
        assertEquals(multipleUris, tenant.getRedirectUri());
    }

    @Test
    @DisplayName("Should handle role bitwise operations")
    void testRoleBitwiseOperations() {
        long adminRole = 1L;
        long userRole = 2L;
        long moderatorRole = 4L;

        tenant.setAllowedRoles(adminRole | userRole | moderatorRole);
        assertEquals(7L, tenant.getAllowedRoles());

        assertTrue((tenant.getAllowedRoles() & adminRole) != 0);
        assertTrue((tenant.getAllowedRoles() & userRole) != 0);
        assertTrue((tenant.getAllowedRoles() & moderatorRole) != 0);
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testEmptyStrings() {
        tenant.setName("");
        tenant.setSecret("");
        tenant.setRedirectUri("");
        tenant.setRequiredScopes("");
        tenant.setSupportedGrantTypes("");

        assertEquals("", tenant.getName());
        assertEquals("", tenant.getSecret());
        assertEquals("", tenant.getRedirectUri());
        assertEquals("", tenant.getRequiredScopes());
        assertEquals("", tenant.getSupportedGrantTypes());
    }

    @Test
    @DisplayName("Should handle OAuth2 grant types")
    void testOAuth2GrantTypes() {
        String grantTypes = "authorization_code implicit password client_credentials refresh_token";
        tenant.setSupportedGrantTypes(grantTypes);
        assertEquals(grantTypes, tenant.getSupportedGrantTypes());
        assertTrue(tenant.getSupportedGrantTypes().contains("authorization_code"));
        assertTrue(tenant.getSupportedGrantTypes().contains("client_credentials"));
    }

    @Test
    @DisplayName("Should handle space-separated scopes")
    void testSpaceSeparatedScopes() {
        String scopes = "openid profile email address phone";
        tenant.setRequiredScopes(scopes);
        assertEquals(scopes, tenant.getRequiredScopes());
        String[] scopeArray = tenant.getRequiredScopes().split(" ");
        assertEquals(5, scopeArray.length);
    }

    @Test
    @DisplayName("Should maintain version consistency across updates")
    void testVersionConsistency() {
        assertEquals(0L, tenant.getVersion());

        tenant.setVersion(0L);
        assertEquals(1L, tenant.getVersion());

        tenant.setVersion(1L);
        assertEquals(2L, tenant.getVersion());

        assertThrows(IllegalStateException.class, () -> tenant.setVersion(1L));
    }
}