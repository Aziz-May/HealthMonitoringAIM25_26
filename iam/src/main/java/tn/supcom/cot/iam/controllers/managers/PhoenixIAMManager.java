package tn.supcom.cot.iam.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.supcom.cot.iam.controllers.Role;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.entities.Tenant;
import tn.supcom.cot.iam.services.EmailService; // Import service email

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Inject
    private EmailService emailService; // Injection

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
        identity.setRoles(1); // Default role (USER/FAMILY)
        identity.setProvidedScopes("resource.read resource.write");

        // --- Activation Logic ---
        identity.setAccountActivated(false);
        String code = generateActivationCode();
        identity.setActivationCode(code);
        identity.setActivationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        Identity savedIdentity = identityRepository.save(identity);

        // Send Email
        emailService.sendActivationEmail(email, code);

        return savedIdentity;
    }

    // --- Activate Account Method ---
    public void activateAccount(String username, String code) {
        Identity identity = identityRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (identity.isAccountActivated()) {
            return; // Already active, do nothing
        }

        if (identity.getActivationCode() == null || !identity.getActivationCode().equals(code)) {
            throw new IllegalArgumentException("Invalid activation code");
        }

        if (LocalDateTime.now().isAfter(identity.getActivationCodeExpiresAt())) {
            throw new IllegalArgumentException("Activation code has expired");
        }

        // Activate
        identity.setAccountActivated(true);
        identity.setActivationCode(null); // Clear code
        identity.setActivationCodeExpiresAt(null);

        identityRepository.save(identity);
    }

    public Grant saveGrant(String tenantName, String username, String approvedScopes) {
        Tenant tenant = findTenantByName(tenantName);
        Optional<Identity> identityOpt = identityRepository.findByUsername(username);
        if (identityOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        Identity identity = identityOpt.get();
        return saveGrantForIdentity(tenant.getId(), identity.getId(), approvedScopes);
    }

    public Grant saveGrantForIdentity(String tenantId, String identityId, String approvedScopes) {
        Grant grant = new Grant();
        grant.setId(Grant.createId(tenantId, identityId));
        grant.setTenantId(tenantId);
        grant.setIdentityId(identityId);
        grant.setApprovedScopes(approvedScopes);
        grant.setIssuanceDateTime(java.time.LocalDateTime.now());
        return grantRepository.save(grant);
    }

    private String generateActivationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6 digits
        return String.valueOf(code);
    }
}