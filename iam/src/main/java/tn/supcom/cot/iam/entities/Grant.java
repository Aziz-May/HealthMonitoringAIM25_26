package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.time.LocalDateTime;


@Entity
public class Grant implements RootEntity<String> {

    @Id
    private String id;

    @Column
    private long version=0L;

    @Column
    private String tenantId;

    @Column
    private String identityId;

    @Column
    private String approvedScopes;

    @Column
    private LocalDateTime issuanceDateTime;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        if (this.version != version) {
            throw new IllegalStateException();
        }

        ++this.version;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getApprovedScopes() {
        return approvedScopes;
    }

    public void setApprovedScopes(String approvedScopes) {
        this.approvedScopes = approvedScopes;
    }

    public LocalDateTime getIssuanceDateTime() {
        return issuanceDateTime;
    }

    public void setIssuanceDateTime(LocalDateTime issuanceDateTime) {
        this.issuanceDateTime = issuanceDateTime;
    }

}
