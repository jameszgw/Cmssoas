package com.codeman.platform.customer.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 统一客户主数据。各业务实体(License/账单/合同/订阅/支付)按 name 聚合成客户360。 */
@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32) private String code;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "tenant_code", length = 32) private String tenantCode;
    @Column(length = 64) private String contact;
    @Column(length = 128) private String email;
    @Column(length = 32) private String phone;
    @Column(length = 64) private String industry;
    @Column(nullable = false, length = 16) private String status = "ACTIVE";
    @Column(length = 512) private String note;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getContact() { return contact; }
    public void setContact(String v) { this.contact = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getIndustry() { return industry; }
    public void setIndustry(String v) { this.industry = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
