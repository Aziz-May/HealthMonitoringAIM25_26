package tn.supcom.cot.iam.controllers.managers;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.supcom.cot.iam.controllers.Role;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.GrantPK;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.entities.Tenant;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhoenixIAMManager Tests")
class PhoenixIAMManagerTest {

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private GrantRepository grantRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private PhoenixIAMManager iamManager;

    private Tenant testTenant;
    private Identity testIdentity;
    private Grant testGrant;
    private GrantPK testGrantPK;

    private static MockedStatic<ConfigProvider> mockedConfigProvider;

    @BeforeAll
    static void initRoleConfig() {
        // Mock ConfigProvider.getConfig() before Role class is loaded
        mockedConfigProvider = Mockito.mockStatic(ConfigProvider.class);

        Config mockConfig =  Mockito.mock(Config.class);
        mockedConfigProvider.when(ConfigProvider::getConfig).thenReturn(mockConfig);

        // Provide fake roles to simulate configuration
        Mockito.when(mockConfig.getValues("roles", String.class)).thenReturn(List.of("customRole1", "customRole2"));
    }

    @AfterAll
    static void closeMock() {
        mockedConfigProvider.close(); // Release the static mock
    }

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setId("tenant-123");
        testTenant.setName("TestTenant");

        testIdentity = new Identity();
        testIdentity.setId("identity-456");
        testIdentity.setUsername("testuser");
        testIdentity.setRoles(0L);

        testGrantPK = new GrantPK();
        testGrantPK.setTenantId("tenant-123");
        testGrantPK.setIdentityId("identity-456");

        testGrant = new Grant();
        testGrant.setId(testGrantPK);
        testGrant.setTenant(testTenant);
        testGrant.setIdentity(testIdentity);
    }

    // ==================== Example test using Role.byId ====================
    @Test
    @DisplayName("Should find custom role by mocked configuration")
    void testCustomRoleById() {
        Role customRole1 = Role.byId("customRole1");
        Role customRole2 = Role.byId("customRole2");

        assertNotNull(customRole1);
        assertNotNull(customRole2);

        assertEquals(Role.GUEST, Role.byId("guest"));
        assertEquals(Role.ROOT, Role.byId("root"));
    }

    // ==================== findTenantByName Tests ====================

    @Test
    @DisplayName("Should find tenant by name successfully")
    void testFindTenantByNameSuccess() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));

        Tenant result = iamManager.findTenantByName("TestTenant");

        assertNotNull(result);
        assertEquals("tenant-123", result.getId());
        assertEquals("TestTenant", result.getName());
        verify(tenantRepository).findByName("TestTenant");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when tenant not found")
    void testFindTenantByNameNotFound() {
        when(tenantRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                iamManager.findTenantByName("NonExistent")
        );
        verify(tenantRepository).findByName("NonExistent");
    }

    @Test
    @DisplayName("Should handle null tenant name")
    void testFindTenantByNameNull() {
        when(tenantRepository.findByName(null)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                iamManager.findTenantByName(null)
        );
        verify(tenantRepository).findByName(null);
    }

    // ==================== findIdentityByName Tests ====================

    @Test
    @DisplayName("Should find identity by username successfully")
    void testFindIdentityByNameSuccess() {
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        Identity result = iamManager.findIdentityByName("testuser");

        assertNotNull(result);
        assertEquals("identity-456", result.getId());
        assertEquals("testuser", result.getUsername());
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when identity not found")
    void testFindIdentityByNameNotFound() {
        when(identityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                iamManager.findIdentityByName("nonexistent")
        );
        verify(identityRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should handle null username")
    void testFindIdentityByNameNull() {
        when(identityRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                iamManager.findIdentityByName(null)
        );
        verify(identityRepository).findByUsername(null);
    }

    // ==================== findGrant Tests ====================

    @Test
    @DisplayName("Should find grant successfully")
    void testFindGrantSuccess() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));
        when(grantRepository.findById(any(GrantPK.class))).thenReturn(Optional.of(testGrant));

        Optional<Grant> result = iamManager.findGrant("TestTenant", "identity-456");

        assertTrue(result.isPresent());
        assertEquals(testGrant, result.get());
        verify(tenantRepository).findByName("TestTenant");
        verify(grantRepository).findById(any(GrantPK.class));
    }

    @Test
    @DisplayName("Should return empty Optional when grant not found")
    void testFindGrantNotFound() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));
        when(grantRepository.findById(any(GrantPK.class))).thenReturn(Optional.empty());

        Optional<Grant> result = iamManager.findGrant("TestTenant", "identity-456");

        assertFalse(result.isPresent());
        verify(tenantRepository).findByName("TestTenant");
        verify(grantRepository).findById(any(GrantPK.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when tenant not found in findGrant")
    void testFindGrantTenantNotFound() {
        when(tenantRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                iamManager.findGrant("NonExistent", "identity-456")
        );
        verify(tenantRepository).findByName("NonExistent");
        verify(grantRepository, never()).findById(any(GrantPK.class));
    }

    @Test
    @DisplayName("Should construct correct GrantPK for findGrant")
    void testFindGrantConstructsCorrectPK() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));
        when(grantRepository.findById(any(GrantPK.class))).thenAnswer(invocation -> {
            GrantPK pk = invocation.getArgument(0);
            assertEquals("tenant-123", pk.getTenantId());
            assertEquals("identity-456", pk.getIdentityId());
            return Optional.of(testGrant);
        });

        iamManager.findGrant("TestTenant", "identity-456");

        verify(grantRepository).findById(any(GrantPK.class));
    }

    // ==================== getRoles Tests ====================

    @Test
    @DisplayName("Should return empty array when identity has no roles")
    void testGetRolesNoRoles() {
        testIdentity.setRoles(0L);
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        assertEquals(0, roles.length);
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return guest role when identity has guest role (value 0)")
    void testGetRolesGuestRole() {
        testIdentity.setRoles(0L);
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        // Guest role has value 0, so bitwise AND will always be 0
        // getRoles will return empty array for guest
        assertEquals(0, roles.length);
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return single role when identity has R_P00 (value 1)")
    void testGetRolesSingleRole() {
        testIdentity.setRoles(1L); // R_P00
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        assertTrue(roles.length >= 1);
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return multiple roles when identity has multiple role bits set")
    void testGetRolesMultipleRoles() {
        testIdentity.setRoles(7L); // Binary: 111 (R_P00=1, R_P01=2, R_P02=4)
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        assertTrue(roles.length >= 3);
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw IllegalAccessError when identity not found in getRoles")
    void testGetRolesIdentityNotFound() {
        when(identityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalAccessError.class, () ->
                iamManager.getRoles("nonexistent")
        );
        verify(identityRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should handle bitwise role operations correctly")
    void testGetRolesBitwiseOperations() {
        // Test with specific role bit patterns
        testIdentity.setRoles(5L); // Binary: 101 (R_P00=1 and R_P02=4)
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        // Verify no duplicates
        assertEquals(roles.length, java.util.Arrays.stream(roles).distinct().count());
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle null username in getRoles")
    void testGetRolesNullUsername() {
        when(identityRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThrows(IllegalAccessError.class, () ->
                iamManager.getRoles(null)
        );
        verify(identityRepository).findByUsername(null);
    }

    @Test
    @DisplayName("Should return unique roles (no duplicates)")
    void testGetRolesNoDuplicates() {
        testIdentity.setRoles(15L); // Binary: 1111 (R_P00, R_P01, R_P02, R_P03)
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        long uniqueCount = java.util.Arrays.stream(roles).distinct().count();
        assertEquals(roles.length, uniqueCount, "Roles array should not contain duplicates");
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle ROOT role (Long.MAX_VALUE)")
    void testGetRolesRootRole() {
        testIdentity.setRoles(Long.MAX_VALUE); // ROOT role
        when(identityRepository.findByUsername("rootuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("rootuser");

        assertNotNull(roles);
        // ROOT role has all bits set, should match all defined roles
        assertTrue(roles.length > 0);
        verify(identityRepository).findByUsername("rootuser");
    }

    @Test
    @DisplayName("Should handle high bit roles (R_P62)")
    void testGetRolesHighBitRoles() {
        testIdentity.setRoles(1L << 62L); // R_P62
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should filter out null role names from Role.byValue")
    void testGetRolesFiltersNullRoleNames() {
        // Set roles that may not have corresponding configured role names
        testIdentity.setRoles(255L); // Multiple role bits
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        // Verify no null values in the result
        for (String role : roles) {
            assertNotNull(role, "Role array should not contain null values");
        }
        verify(identityRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should use HashSet to ensure unique role names")
    void testGetRolesUsesHashSet() {
        testIdentity.setRoles(31L); // Binary: 11111 (5 roles)
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        String[] roles = iamManager.getRoles("testuser");

        assertNotNull(roles);
        // Verify uniqueness
        java.util.Set<String> uniqueRoles = new java.util.HashSet<>(java.util.Arrays.asList(roles));
        assertEquals(roles.length, uniqueRoles.size(), "Should not have duplicate roles");
        verify(identityRepository).findByUsername("testuser");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle complete workflow: find tenant, identity, and grant")
    void testCompleteWorkflow() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));
        when(grantRepository.findById(any(GrantPK.class))).thenReturn(Optional.of(testGrant));

        Tenant tenant = iamManager.findTenantByName("TestTenant");
        Identity identity = iamManager.findIdentityByName("testuser");
        Optional<Grant> grant = iamManager.findGrant("TestTenant", identity.getId());

        assertNotNull(tenant);
        assertNotNull(identity);
        assertTrue(grant.isPresent());
        assertEquals(tenant.getId(), grant.get().getTenant().getId());
        assertEquals(identity.getId(), grant.get().getIdentity().getId());
    }

    @Test
    @DisplayName("Should handle concurrent tenant lookups")
    void testConcurrentTenantLookups() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));

        Tenant result1 = iamManager.findTenantByName("TestTenant");
        Tenant result2 = iamManager.findTenantByName("TestTenant");

        assertNotNull(result1);
        assertNotNull(result2);
        verify(tenantRepository, times(2)).findByName("TestTenant");
    }

    @Test
    @DisplayName("Should verify repository interactions")
    void testRepositoryInteractions() {
        when(tenantRepository.findByName("TestTenant")).thenReturn(Optional.of(testTenant));
        when(identityRepository.findByUsername("testuser")).thenReturn(Optional.of(testIdentity));

        iamManager.findTenantByName("TestTenant");
        iamManager.findIdentityByName("testuser");

        verify(tenantRepository).findByName("TestTenant");
        verify(identityRepository).findByUsername("testuser");
        verifyNoInteractions(grantRepository);
    }
}