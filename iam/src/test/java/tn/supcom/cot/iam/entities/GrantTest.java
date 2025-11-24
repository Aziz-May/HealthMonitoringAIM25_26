package tn.supcom.cot.iam.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Grant Entity Tests")
class GrantTest {

    private Grant grant;

    @BeforeEach
    void setUp() {
        grant = new Grant();
    }

    @Test
    @DisplayName("Should set and get id correctly")
    void testIdGetterSetter() {
        grant.setId("grant-123");
        assertEquals("grant-123", grant.getId());
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
    @DisplayName("Should set and get tenantId correctly")
    void testTenantIdGetterSetter() {
        grant.setTenantId("tenant-1");
        assertEquals("tenant-1", grant.getTenantId());
    }

    @Test
    @DisplayName("Should set and get identityId correctly")
    void testIdentityIdGetterSetter() {
        grant.setIdentityId("identity-1");
        assertEquals("identity-1", grant.getIdentityId());
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
        grant.setTenantId(null);
        grant.setIdentityId(null);
        grant.setApprovedScopes(null);
        grant.setIssuanceDateTime(null);

        assertNull(grant.getId());
        assertNull(grant.getTenantId());
        assertNull(grant.getIdentityId());
        assertNull(grant.getApprovedScopes());
        assertNull(grant.getIssuanceDateTime());
    }

    @Test
    @DisplayName("Should create complete grant object")
    void testCompleteGrantCreation() {
        LocalDateTime issuanceTime = LocalDateTime.of(2024, 1, 15, 10, 30);

        grant.setId("grant-1");
        grant.setTenantId("tenant-1");
        grant.setIdentityId("identity-1");
        grant.setApprovedScopes("openid profile email");
        grant.setIssuanceDateTime(issuanceTime);

        assertEquals("grant-1", grant.getId());
        assertEquals("tenant-1", grant.getTenantId());
        assertEquals("identity-1", grant.getIdentityId());
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
}
