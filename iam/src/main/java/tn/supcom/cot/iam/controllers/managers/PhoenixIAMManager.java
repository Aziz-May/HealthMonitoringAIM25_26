package tn.supcom.cot.iam.controllers.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tn.supcom.cot.iam.controllers.Role;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.GrantPK;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.entities.Tenant;

import java.util.HashSet;
import java.util.Optional;

@Singleton
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
        var pk = new GrantPK();
        pk.setIdentityId(identityId);
        pk.setTenantId(tenant.getId());
        return grantRepository.findById(pk);
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
}
