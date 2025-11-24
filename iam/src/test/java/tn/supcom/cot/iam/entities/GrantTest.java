package tn.supcom.cot.iam.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Grant Entity Tests")
class GrantTest {

    private Grant grant;
    private GrantPK grantPK;
    private Tenant tenant;
    private Identity identity;

    @BeforeEach
    void setUp() {
        grant = new Grant();

        grantPK = new GrantPK();
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        tenant = new Tenant();
        tenant.setId("tenant-1");
        tenant.setName("Test Tenant");

        identity = new Identity();
        identity.setId("identity-1");
        identity.setUsername("testuser");
    }

    @Test
    @DisplayName("Should set and get composite id correctly")
    void testIdGetterSetter() {
        grant.setId(grantPK);
        assertEquals(grantPK, grant.getId());
        assertEquals("tenant-1", grant.getId().getTenantId());
        assertEquals("identity-1", grant.getId().getIdentityId());
    }

    @Test
    @DisplayName("Should initialize version to 0")
    void testVersionInitialization() {
        assertEquals(0L, grant.getVersion());
    }

    @Test
    @DisplayName("Should increment version when current version matches")
    void testVersionIncrement() {
        assertEquals(0L, grant.getVersion());
        grant.setVersion(0L);
        assertEquals(1L, grant.getVersion());
        grant.setVersion(1L);
        assertEquals(2L, grant.getVersion());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when version mismatch")
    void testVersionMismatch() {
        grant.setVersion(0L);
        assertThrows(IllegalStateException.class, () -> grant.setVersion(0L));
    }

    @Test
    @DisplayName("Should set and get tenant correctly")
    void testTenantGetterSetter() {
        grant.setTenant(tenant);
        assertEquals(tenant, grant.getTenant());
        assertEquals("tenant-1", grant.getTenant().getId());
        assertEquals("Test Tenant", grant.getTenant().getName());
    }

    @Test
    @DisplayName("Should set and get identity correctly")
    void testIdentityGetterSetter() {
        grant.setIdentity(identity);
        assertEquals(identity, grant.getIdentity());
        assertEquals("identity-1", grant.getIdentity().getId());
        assertEquals("testuser", grant.getIdentity().getUsername());
    }

    @Test
    @DisplayName("Should set and get approved scopes correctly")
    void testApprovedScopesGetterSetter() {
        String scopes = "read write admin";
        grant.setApprovedScopes(scopes);
        assertEquals(scopes, grant.getApprovedScopes());
    }

    @Test
    @DisplayName("Should set and get issuance date time correctly")
    void testIssuanceDateTimeGetterSetter() {
        LocalDateTime now = LocalDateTime.now();
        grant.setIssuanceDateTime(now);
        assertEquals(now, grant.getIssuanceDateTime());
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        grant.setId(null);
        grant.setTenant(null);
        grant.setIdentity(null);
        grant.setApprovedScopes(null);
        grant.setIssuanceDateTime(null);

        assertNull(grant.getId());
        assertNull(grant.getTenant());
        assertNull(grant.getIdentity());
        assertNull(grant.getApprovedScopes());
        assertNull(grant.getIssuanceDateTime());
    }

    @Test
    @DisplayName("Should create complete grant object")
    void testCompleteGrantCreation() {
        LocalDateTime issuanceTime = LocalDateTime.of(2024, 1, 15, 10, 30);

        grant.setId(grantPK);
        grant.setTenant(tenant);
        grant.setIdentity(identity);
        grant.setApprovedScopes("openid profile email");
        grant.setIssuanceDateTime(issuanceTime);

        assertEquals(grantPK, grant.getId());
        assertEquals(tenant, grant.getTenant());
        assertEquals(identity, grant.getIdentity());
        assertEquals("openid profile email", grant.getApprovedScopes());
        assertEquals(issuanceTime, grant.getIssuanceDateTime());
        assertEquals(0L, grant.getVersion());
    }

    @Test
    @DisplayName("Should handle empty approved scopes")
    void testEmptyApprovedScopes() {
        grant.setApprovedScopes("");
        assertEquals("", grant.getApprovedScopes());
    }

    @Test
    @DisplayName("Should handle space-separated scopes")
    void testSpaceSeparatedScopes() {
        String scopes = "openid profile email address phone";
        grant.setApprovedScopes(scopes);
        assertEquals(scopes, grant.getApprovedScopes());

        String[] scopeArray = grant.getApprovedScopes().split(" ");
        assertEquals(5, scopeArray.length);
        assertEquals("openid", scopeArray[0]);
        assertEquals("phone", scopeArray[4]);
    }

    @Test
    @DisplayName("Should handle past issuance date time")
    void testPastIssuanceDateTime() {
        LocalDateTime pastDate = LocalDateTime.of(2023, 6, 1, 12, 0);
        grant.setIssuanceDateTime(pastDate);
        assertEquals(pastDate, grant.getIssuanceDateTime());
        assertTrue(grant.getIssuanceDateTime().isBefore(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle current issuance date time")
    void testCurrentIssuanceDateTime() {
        LocalDateTime now = LocalDateTime.now();
        grant.setIssuanceDateTime(now);
        assertNotNull(grant.getIssuanceDateTime());
    }

    @Test
    @DisplayName("Should maintain relationship between grant and tenant")
    void testGrantTenantRelationship() {
        grant.setId(grantPK);
        grant.setTenant(tenant);

        assertEquals(grantPK.getTenantId(), grant.getTenant().getId());
    }

    @Test
    @DisplayName("Should maintain relationship between grant and identity")
    void testGrantIdentityRelationship() {
        grant.setId(grantPK);
        grant.setIdentity(identity);

        assertEquals(grantPK.getIdentityId(), grant.getIdentity().getId());
    }

    @Test
    @DisplayName("Should maintain version consistency across updates")
    void testVersionConsistency() {
        assertEquals(0L, grant.getVersion());

        grant.setVersion(0L);
        assertEquals(1L, grant.getVersion());

        grant.setVersion(1L);
        assertEquals(2L, grant.getVersion());

        assertThrows(IllegalStateException.class, () -> grant.setVersion(1L));
    }

    @Test
    @DisplayName("Should handle OAuth2 scopes")
    void testOAuth2Scopes() {
        String oauth2Scopes = "openid profile email offline_access";
        grant.setApprovedScopes(oauth2Scopes);
        assertEquals(oauth2Scopes, grant.getApprovedScopes());
        assertTrue(grant.getApprovedScopes().contains("openid"));
        assertTrue(grant.getApprovedScopes().contains("offline_access"));
    }

    @Test
    @DisplayName("Should represent authorization grant between tenant and identity")
    void testAuthorizationGrant() {
        LocalDateTime issuanceTime = LocalDateTime.now();

        grant.setId(grantPK);
        grant.setTenant(tenant);
        grant.setIdentity(identity);
        grant.setApprovedScopes("read write");
        grant.setIssuanceDateTime(issuanceTime);

        assertNotNull(grant.getId());
        assertNotNull(grant.getTenant());
        assertNotNull(grant.getIdentity());
        assertNotNull(grant.getApprovedScopes());
        assertNotNull(grant.getIssuanceDateTime());

        assertEquals(tenant.getId(), grant.getId().getTenantId());
        assertEquals(identity.getId(), grant.getId().getIdentityId());
    }

    @Test
    @DisplayName("Should allow updating tenant reference")
    void testUpdateTenant() {
        grant.setTenant(tenant);
        assertEquals("Test Tenant", grant.getTenant().getName());

        Tenant newTenant = new Tenant();
        newTenant.setId("tenant-2");
        newTenant.setName("Updated Tenant");

        grant.setTenant(newTenant);
        assertEquals("Updated Tenant", grant.getTenant().getName());
        assertEquals("tenant-2", grant.getTenant().getId());
    }

    @Test
    @DisplayName("Should allow updating identity reference")
    void testUpdateIdentity() {
        grant.setIdentity(identity);
        assertEquals("testuser", grant.getIdentity().getUsername());

        Identity newIdentity = new Identity();
        newIdentity.setId("identity-2");
        newIdentity.setUsername("newuser");

        grant.setIdentity(newIdentity);
        assertEquals("newuser", grant.getIdentity().getUsername());
        assertEquals("identity-2", grant.getIdentity().getId());
    }

    @Test
    @DisplayName("Should handle complex composite key scenario")
    void testComplexCompositeKey() {
        GrantPK pk1 = new GrantPK();
        pk1.setTenantId("tenant-a");
        pk1.setIdentityId("identity-x");

        GrantPK pk2 = new GrantPK();
        pk2.setTenantId("tenant-a");
        pk2.setIdentityId("identity-y");

        Grant grant1 = new Grant();
        grant1.setId(pk1);

        Grant grant2 = new Grant();
        grant2.setId(pk2);

        assertNotEquals(grant1.getId(), grant2.getId());
        assertEquals("tenant-a", grant1.getId().getTenantId());
        assertEquals("tenant-a", grant2.getId().getTenantId());
        assertNotEquals(grant1.getId().getIdentityId(), grant2.getId().getIdentityId());
    }
}