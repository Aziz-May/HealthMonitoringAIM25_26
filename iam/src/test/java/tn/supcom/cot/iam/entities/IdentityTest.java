package tn.supcom.cot.iam.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Identity Entity Tests")
class IdentityTest {

    private Identity identity;

    @BeforeEach
    void setUp() {
        identity = new Identity();
    }

    @Test
    @DisplayName("Should set and get id correctly")
    void testIdGetterSetter() {
        String testId = "test-id-123";
        identity.setId(testId);
        assertEquals(testId, identity.getId());
    }

    @Test
    @DisplayName("Should initialize version to 0")
    void testVersionInitialization() {
        assertEquals(0L, identity.getVersion());
    }

    @Test
    @DisplayName("Should increment version when current version matches")
    void testVersionIncrement() {
        assertEquals(0L, identity.getVersion());
        identity.setVersion(0L);
        assertEquals(1L, identity.getVersion());
        identity.setVersion(1L);
        assertEquals(2L, identity.getVersion());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when version mismatch")
    void testVersionMismatch() {
        identity.setVersion(0L);
        assertThrows(IllegalStateException.class, () -> identity.setVersion(0L));
    }

    @Test
    @DisplayName("Should set and get username correctly")
    void testUsernameGetterSetter() {
        String username = "testuser";
        identity.setUsername(username);
        assertEquals(username, identity.getUsername());
    }

    @Test
    @DisplayName("getName should return username (Principal interface)")
    void testGetName() {
        String username = "testuser";
        identity.setUsername(username);
        assertEquals(username, identity.getName());
    }

    @Test
    @DisplayName("Should set and get password correctly")
    void testPasswordGetterSetter() {
        String password = "hashedPassword123";
        identity.setPassword(password);
        assertEquals(password, identity.getPassword());
    }

    @Test
    @DisplayName("Should set and get roles correctly")
    void testRolesGetterSetter() {
        long roles = 7L; // Binary: 111 (three roles)
        identity.setRoles(roles);
        assertEquals(roles, identity.getRoles());
    }

    @Test
    @DisplayName("Should set and get provided scopes correctly")
    void testProvidedScopesGetterSetter() {
        String scopes = "read write admin";
        identity.setProvidedScopes(scopes);
        assertEquals(scopes, identity.getProvidedScopes());
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        identity.setUsername(null);
        identity.setPassword(null);
        identity.setProvidedScopes(null);

        assertNull(identity.getUsername());
        assertNull(identity.getPassword());
        assertNull(identity.getProvidedScopes());
        assertNull(identity.getName());
    }

    @Test
    @DisplayName("Should create complete identity object")
    void testCompleteIdentityCreation() {
        identity.setId("user-001");
        identity.setUsername("john.doe");
        identity.setPassword("$2a$10$hash");
        identity.setRoles(15L);
        identity.setProvidedScopes("read write delete");

        assertEquals("user-001", identity.getId());
        assertEquals("john.doe", identity.getUsername());
        assertEquals("john.doe", identity.getName());
        assertEquals("$2a$10$hash", identity.getPassword());
        assertEquals(15L, identity.getRoles());
        assertEquals("read write delete", identity.getProvidedScopes());
        assertEquals(0L, identity.getVersion());
    }

    @Test
    @DisplayName("Should implement Principal interface correctly")
    void testPrincipalInterface() {
        assertTrue(identity instanceof java.security.Principal);
        identity.setUsername("principalUser");
        assertEquals("principalUser", identity.getName());
    }

    @Test
    @DisplayName("Should handle role bitwise operations")
    void testRoleBitwiseOperations() {
        long adminRole = 1L;
        long userRole = 2L;
        long moderatorRole = 4L;

        identity.setRoles(adminRole | userRole | moderatorRole);
        assertEquals(7L, identity.getRoles());

        assertTrue((identity.getRoles() & adminRole) != 0);
        assertTrue((identity.getRoles() & userRole) != 0);
        assertTrue((identity.getRoles() & moderatorRole) != 0);
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testEmptyStrings() {
        identity.setUsername("");
        identity.setPassword("");
        identity.setProvidedScopes("");

        assertEquals("", identity.getUsername());
        assertEquals("", identity.getPassword());
        assertEquals("", identity.getProvidedScopes());
    }
}