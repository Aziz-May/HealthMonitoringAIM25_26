package tn.supcom.cot.iam.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tn.supcom.cot.iam.controllers.Role;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.entities.Tenant;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

@ApplicationScoped
public class PhoenixIAMManager {

    @Inject
    private IdentityRepository identityRepository;

    @Inject
    private GrantRepository grantRepository;

    @Inject
    private TenantRepository tenantRepository;

    public Tenant findTenantByName(String name) {
        return tenantRepository.findByName(name).orElseThrow(IllegalArgumentException::new);
    }

    public Identity findIdentityByName(String name) {
        return identityRepository.findByUsername(name).orElseThrow(IllegalArgumentException::new);
    }

    public Optional<Grant> findGrant(String tenantName, String identityId) {
        Tenant tenant = findTenantByName(tenantName);
        if (tenant == null) {
            throw new IllegalArgumentException("Invalid Client Id!");
        }
        return grantRepository.findByTenantIdAndIdentityId(tenant.getId(), identityId);
    }

    public String[] getRoles(String username) {
        var identity = identityRepository.findByUsername(username).orElseThrow(IllegalAccessError::new);
        var roles = identity.getRoles();
        var ret = new HashSet<String>();
        for (var role: Role.values()) {
            if ((roles&role.getValue()) != 0L) {
                String value = Role.byValue(role.getValue());
                if (value == null) {
                    continue;
                }
                ret.add(value);
            }
        }
        return ret.toArray(new String[0]);
    }

    public Identity createIdentity(String username, String password, String email, String phoneNumber, LocalDate birthDate) {
        if (identityRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        Identity identity = new Identity();
        identity.setId(java.util.UUID.randomUUID().toString());
        identity.setUsername(username);
        identity.setPassword(tn.supcom.cot.iam.security.Argon2Utility.hash(password.toCharArray()));
        identity.setEmail(email);
        identity.setPhoneNumber(phoneNumber);
        identity.setBirthDate(birthDate);
        identity.setRoles(1); // Default role (USER)
        identity.setProvidedScopes("resource.read resource.write"); // Default scopes
        
        // Save and return the identity
        return identityRepository.save(identity);
    }

    public Grant saveGrant(String tenantName, String username, String approvedScopes) {
        Tenant tenant = findTenantByName(tenantName);
        
        // Try to find the identity, if not found it might be a timing issue
        Optional<Identity> identityOpt = identityRepository.findByUsername(username);
        if (identityOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        Identity identity = identityOpt.get();
        
        return saveGrantForIdentity(tenant.getId(), identity.getId(), approvedScopes);
    }
    
    public Grant saveGrantForIdentity(String tenantId, String identityId, String approvedScopes) {
        // Create the grant with composite string ID
        Grant grant = new Grant();
        grant.setId(Grant.createId(tenantId, identityId));
        grant.setTenantId(tenantId);
        grant.setIdentityId(identityId);
        grant.setApprovedScopes(approvedScopes);
        grant.setIssuanceDateTime(java.time.LocalDateTime.now());
        
        return grantRepository.save(grant);
    }
}
