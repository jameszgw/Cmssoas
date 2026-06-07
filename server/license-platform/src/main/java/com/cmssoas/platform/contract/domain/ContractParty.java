package com.cmssoas.platform.contract.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 合同签署方。每方独立签署，全部 SIGNED 后合同生效。 */
@Entity
@Table(name = "contract_party")
public class ContractParty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contract_id", nullable = false) private Long contractId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "party_role", length = 32) private String partyRole;
    @Column(length = 128) private String email;
    @Column(length = 32) private String phone;
    @Column(name = "sign_status", nullable = false, length = 16) private String signStatus = "PENDING";
    @Column(name = "sign_hash", length = 80) private String signHash;
    @Column(name = "signed_at") private LocalDateTime signedAt;

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long v) { this.contractId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getPartyRole() { return partyRole; }
    public void setPartyRole(String v) { this.partyRole = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getSignStatus() { return signStatus; }
    public void setSignStatus(String v) { this.signStatus = v; }
    public String getSignHash() { return signHash; }
    public void setSignHash(String v) { this.signHash = v; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime v) { this.signedAt = v; }
}
