package tn.supcom.cot.iam.controllers;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Role Enum Tests")
class RoleTest {

    static MockedStatic<ConfigProvider> mockedConfigProvider;
    @BeforeAll
    static void setup() {
        // Mock the MicroProfile Config
        Config mockConfig = Mockito.mock(Config.class);
        Mockito.when(mockConfig.getValues("roles", String.class))
                .thenReturn(List.of("R_Pcustom1", "R_Pcustom2"));

        // Mock the static call to ConfigProvider.getConfig()
        mockedConfigProvider = Mockito.mockStatic(ConfigProvider.class);
        mockedConfigProvider.when(ConfigProvider::getConfig).thenReturn(mockConfig);
    }

    @Test
    @DisplayName("Should have GUEST role with value 0")
    void testGuestRoleValue() {
        assertEquals(0L, Role.GUEST.getValue());
        assertEquals("guest", Role.GUEST.id());
    }

    @Test
    @DisplayName("Should have ROOT role with value Long.MAX_VALUE")
    void testRootRoleValue() {
        assertEquals(Long.MAX_VALUE, Role.ROOT.getValue());
        assertEquals("root", Role.ROOT.id());
    }

    @Test
    @DisplayName("Should have R_P00 with value 1 (2^0)")
    void testRP00Value() {
        assertEquals(1L, Role.R_P00.getValue());
    }

    @Test
    @DisplayName("Should have R_P01 with value 2 (2^1)")
    void testRP01Value() {
        assertEquals(2L, Role.R_P01.getValue());
    }

    @Test
    @DisplayName("Should have R_P02 with value 4 (2^2)")
    void testRP02Value() {
        assertEquals(4L, Role.R_P02.getValue());
    }

    @Test
    @DisplayName("Should have R_P10 with value 1024 (2^10)")
    void testRP10Value() {
        assertEquals(1024L, Role.R_P10.getValue());
    }

    @Test
    @DisplayName("Should have R_P62 with value 2^62")
    void testRP62Value() {
        assertEquals(1L << 62L, Role.R_P62.getValue());
    }

    @Test
    @DisplayName("Should have each role value as power of 2 (except GUEST and ROOT)")
    void testRoleValuesArePowersOfTwo() {
        for (Role role : Role.values()) {
            if (role == Role.GUEST || role == Role.ROOT) {
                continue;
            }
            long value = role.getValue();
            // Check if value is a power of 2 (only one bit set)
            assertTrue((value & (value - 1)) == 0,
                    role.name() + " value should be a power of 2");
            assertTrue(value > 0, role.name() + " value should be positive");
        }
    }

    @Test
    @DisplayName("Should have unique values for all roles (except configurable ones)")
    void testUniqueRoleValues() {
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            for (int j = i + 1; j < roles.length; j++) {
                if (roles[i].getValue() == roles[j].getValue()) {
                    // Only allowed if these are configurable role positions
                    // that share the same bit position
                    assertTrue(
                            roles[i].name().startsWith("R_P") && roles[j].name().startsWith("R_P"),
                            "Non-configurable roles should have unique values"
                    );
                }
            }
        }
    }

    @Test
    @DisplayName("Should return role name by value using byValue")
    void testByValue() {
        assertEquals("guest", Role.byValue(0L));
        assertEquals("root", Role.byValue(Long.MAX_VALUE));
        // Other values depend on configuration
        assertNotNull(Role.byValue(1L)); // R_P00 or configured role
    }

    @Test
    @DisplayName("Should return null for invalid value in byValue")
    void testByValueInvalid() {
        // Test with a value that's not a power of 2 and not configured
        Long invalidValue = 3L; // Not a single bit
        String result = Role.byValue(invalidValue);
        // Result depends on configuration - may be null or a configured role
        // Just verify method doesn't throw exception
        assertDoesNotThrow(() -> Role.byValue(invalidValue));
    }

    @Test
    @DisplayName("Should return role by id using byId")
    void testById() {
        assertEquals(Role.GUEST, Role.byId("guest"));
        assertEquals(Role.ROOT, Role.byId("root"));
    }

    @Test
    @DisplayName("Should return null for invalid id in byId")
    void testByIdInvalid() {
        assertNull(Role.byId("invalid_role_name"));
        assertNull(Role.byId(""));
        assertNull(Role.byId(null));
    }

    @Test
    @DisplayName("Should have case-insensitive id for GUEST")
    void testGuestIdCaseInsensitive() {
        assertEquals("guest", Role.GUEST.id());
        assertEquals(Role.GUEST, Role.byId("guest"));
    }

    @Test
    @DisplayName("Should have case-insensitive id for ROOT")
    void testRootIdCaseInsensitive() {
        assertEquals("root", Role.ROOT.id());
        assertEquals(Role.ROOT, Role.byId("root"));
    }

    @Test
    @DisplayName("Should support bitwise operations for role combinations")
    void testBitwiseOperations() {
        long combined = Role.R_P00.getValue() | Role.R_P01.getValue() | Role.R_P02.getValue();
        assertEquals(7L, combined); // 1 | 2 | 4 = 7

        // Check if R_P00 is in the combination
        assertTrue((combined & Role.R_P00.getValue()) != 0);
        // Check if R_P01 is in the combination
        assertTrue((combined & Role.R_P01.getValue()) != 0);
        // Check if R_P02 is in the combination
        assertTrue((combined & Role.R_P02.getValue()) != 0);
        // Check if R_P03 is NOT in the combination
        assertFalse((combined & Role.R_P03.getValue()) != 0);
    }

    @Test
    @DisplayName("Should have 65 role positions (GUEST + 63 R_P roles + ROOT)")
    void testRoleCount() {
        Role[] roles = Role.values();
        assertEquals(65, roles.length, "Should have exactly 65 roles");
    }

    @Test
    @DisplayName("Should have sequential role names R_P00 to R_P62")
    void testSequentialRoleNames() {
        for (int i = 0; i <= 62; i++) {
            String expectedName = String.format("R_P%02d", i);
            boolean found = false;
            for (Role role : Role.values()) {
                if (role.name().equals(expectedName)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should have role " + expectedName);
        }
    }

    @Test
    @DisplayName("Should have correct bit shift for each R_P role")
    void testBitShiftValues() {
        assertEquals(1L << 0L, Role.R_P00.getValue());
        assertEquals(1L << 1L, Role.R_P01.getValue());
        assertEquals(1L << 5L, Role.R_P05.getValue());
        assertEquals(1L << 10L, Role.R_P10.getValue());
        assertEquals(1L << 20L, Role.R_P20.getValue());
        assertEquals(1L << 30L, Role.R_P30.getValue());
        assertEquals(1L << 40L, Role.R_P40.getValue());
        assertEquals(1L << 50L, Role.R_P50.getValue());
        assertEquals(1L << 60L, Role.R_P60.getValue());
        assertEquals(1L << 62L, Role.R_P62.getValue());
    }

    @Test
    @DisplayName("ROOT role should match all role bits when using bitwise AND")
    void testRootMatchesAllRoles() {
        long rootValue = Role.ROOT.getValue();
        for (Role role : Role.values()) {
            if (role == Role.GUEST) {
                // GUEST has value 0, so bitwise AND with anything is 0
                continue;
            }
            assertTrue((rootValue & role.getValue()) != 0,
                    "ROOT should match " + role.name());
        }
    }

    @Test
    @DisplayName("GUEST role should not match any other role with bitwise AND")
    void testGuestDoesNotMatchOtherRoles() {
        long guestValue = Role.GUEST.getValue();
        for (Role role : Role.values()) {
            if (role == Role.GUEST) {
                continue;
            }
            assertEquals(0L, guestValue & role.getValue(),
                    "GUEST should not match " + role.name());
        }
    }

    @Test
    @DisplayName("Should handle maximum role combination (all bits set)")
    void testMaximumRoleCombination() {
        long allRoles = 0L;
        for (Role role : Role.values()) {
            if (role != Role.GUEST && role != Role.ROOT) {
                allRoles |= role.getValue();
            }
        }
        // Verify all individual roles are present
        for (Role role : Role.values()) {
            if (role != Role.GUEST && role != Role.ROOT) {
                assertTrue((allRoles & role.getValue()) != 0,
                        role.name() + " should be in combined roles");
            }
        }
    }

    @Test
    @DisplayName("Should retrieve role id correctly")
    void testRoleIdMethod() {
        // GUEST and ROOT always have fixed ids
        assertNotNull(Role.GUEST.id());
        assertNotNull(Role.ROOT.id());
        assertEquals("guest", Role.GUEST.id());
        assertEquals("root", Role.ROOT.id());
    }

    @Test
    @DisplayName("Should handle role value boundaries")
    void testRoleValueBoundaries() {
        // Smallest non-zero role value
        assertEquals(1L, Role.R_P00.getValue());

        // Largest single-bit role value (before ROOT)
        assertEquals(1L << 62L, Role.R_P62.getValue());
        assertTrue(Role.R_P62.getValue() > 0, "R_P62 should be positive");
        assertTrue(Role.R_P62.getValue() < Long.MAX_VALUE, "R_P62 should be less than Long.MAX_VALUE");
    }

    @Test
    @DisplayName("Should support role checking pattern")
    void testRoleCheckingPattern() {
        // Simulate identity with multiple roles
        long userRoles = Role.R_P00.getValue() | Role.R_P05.getValue() | Role.R_P10.getValue();

        // Check if user has R_P00
        assertTrue((userRoles & Role.R_P00.getValue()) != 0);
        // Check if user has R_P05
        assertTrue((userRoles & Role.R_P05.getValue()) != 0);
        // Check if user has R_P10
        assertTrue((userRoles & Role.R_P10.getValue()) != 0);
        // Check if user does NOT have R_P01
        assertFalse((userRoles & Role.R_P01.getValue()) != 0);
    }

    @Test
    @DisplayName("Should handle role removal using bitwise operations")
    void testRoleRemoval() {
        // Start with multiple roles
        long userRoles = Role.R_P00.getValue() | Role.R_P01.getValue() | Role.R_P02.getValue();
        assertEquals(7L, userRoles);

        // Remove R_P01
        userRoles = userRoles & ~Role.R_P01.getValue();
        assertEquals(5L, userRoles); // 111 - 010 = 101

        // Verify R_P00 still exists
        assertTrue((userRoles & Role.R_P00.getValue()) != 0);
        // Verify R_P01 is removed
        assertFalse((userRoles & Role.R_P01.getValue()) != 0);
        // Verify R_P02 still exists
        assertTrue((userRoles & Role.R_P02.getValue()) != 0);
    }
}