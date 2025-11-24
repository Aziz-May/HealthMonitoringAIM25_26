package tn.supcom.cot.iam.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GrantPK Composite Key Tests")
class GrantPKTest {

    private GrantPK grantPK;

    @BeforeEach
    void setUp() {
        grantPK = new GrantPK();
    }

    @Test
    @DisplayName("Should create empty GrantPK with default constructor")
    void testDefaultConstructor() {
        GrantPK pk = new GrantPK();
        assertNull(pk.getTenantId());
        assertNull(pk.getIdentityId());
    }

    @Test
    @DisplayName("Should set and get tenant id correctly")
    void testTenantIdGetterSetter() {
        String tenantId = "tenant-123";
        grantPK.setTenantId(tenantId);
        assertEquals(tenantId, grantPK.getTenantId());
    }

    @Test
    @DisplayName("Should set and get identity id correctly")
    void testIdentityIdGetterSetter() {
        String identityId = "identity-456";
        grantPK.setIdentityId(identityId);
        assertEquals(identityId, grantPK.getIdentityId());
    }

    @Test
    @DisplayName("Should implement Serializable interface")
    void testSerializableInterface() {
        assertTrue(grantPK instanceof Serializable);
    }

    @Test
    @DisplayName("Should be equal when both ids match")
    void testEqualsWithMatchingIds() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId("tenant-1");
        other.setIdentityId("identity-1");

        assertEquals(grantPK, other);
    }

    @Test
    @DisplayName("Should not be equal when tenant ids differ")
    void testNotEqualsWithDifferentTenantId() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId("tenant-2");
        other.setIdentityId("identity-1");

        assertNotEquals(grantPK, other);
    }

    @Test
    @DisplayName("Should not be equal when identity ids differ")
    void testNotEqualsWithDifferentIdentityId() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId("tenant-1");
        other.setIdentityId("identity-2");

        assertNotEquals(grantPK, other);
    }

    @Test
    @DisplayName("Should be equal to itself")
    void testEqualsSameObject() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        assertEquals(grantPK, grantPK);
    }

    @Test
    @DisplayName("Should not be equal to null")
    void testNotEqualsNull() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        assertNotEquals(grantPK, null);
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void testNotEqualsDifferentType() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        assertNotEquals(grantPK, "not a GrantPK");
    }

    @Test
    @DisplayName("Should be equal when both ids are null")
    void testEqualsWithBothIdsNull() {
        GrantPK other = new GrantPK();

        assertEquals(grantPK, other);
    }

    @Test
    @DisplayName("Should not be equal when one tenant id is null")
    void testNotEqualsWithOneTenantIdNull() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId(null);
        other.setIdentityId("identity-1");

        assertNotEquals(grantPK, other);
    }

    @Test
    @DisplayName("Should not be equal when one identity id is null")
    void testNotEqualsWithOneIdentityIdNull() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId("tenant-1");
        other.setIdentityId(null);

        assertNotEquals(grantPK, other);
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testEmptyStrings() {
        grantPK.setTenantId("");
        grantPK.setIdentityId("");

        assertEquals("", grantPK.getTenantId());
        assertEquals("", grantPK.getIdentityId());
    }

    @Test
    @DisplayName("Should create valid composite key")
    void testCompositeKeyCreation() {
        grantPK.setTenantId("tenant-abc");
        grantPK.setIdentityId("identity-xyz");

        assertEquals("tenant-abc", grantPK.getTenantId());
        assertEquals("identity-xyz", grantPK.getIdentityId());
    }

    @Test
    @DisplayName("Should be symmetric in equality")
    void testEqualsSymmetry() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId("tenant-1");
        other.setIdentityId("identity-1");

        assertEquals(grantPK, other);
        assertEquals(other, grantPK);
    }

    @Test
    @DisplayName("Should be transitive in equality")
    void testEqualsTransitivity() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK second = new GrantPK();
        second.setTenantId("tenant-1");
        second.setIdentityId("identity-1");

        GrantPK third = new GrantPK();
        third.setTenantId("tenant-1");
        third.setIdentityId("identity-1");

        assertEquals(grantPK, second);
        assertEquals(second, third);
        assertEquals(grantPK, third);
    }

    @Test
    @DisplayName("Should be consistent in equality")
    void testEqualsConsistency() {
        grantPK.setTenantId("tenant-1");
        grantPK.setIdentityId("identity-1");

        GrantPK other = new GrantPK();
        other.setTenantId("tenant-1");
        other.setIdentityId("identity-1");

        assertEquals(grantPK, other);
        assertEquals(grantPK, other);
        assertEquals(grantPK, other);
    }
}