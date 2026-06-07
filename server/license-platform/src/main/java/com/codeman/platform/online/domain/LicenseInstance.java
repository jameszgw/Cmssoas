package com.codeman.platform.online.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 在线授权的激活实例（浮动席位的占用单元）。 */
@Entity
@Table(name = "license_instance")
public class LicenseInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_id", nullable = false, length = 40)
    private String licenseId;

    @Column(name = "instance_id", nullable = false, length = 64)
    private String instanceId;

    @Column(name = "machine_code", length = 80)
    private String machineCode;

    @Column(length = 64)
    private String ip;

    /** ACTIVE / RELEASED / EXPIRED */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;

    @Column(name = "last_heartbeat", nullable = false)
    private LocalDateTime lastHeartbeat;

    public Long getId() { return id; }
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String v) { this.licenseId = v; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String v) { this.instanceId = v; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String v) { this.machineCode = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime v) { this.activatedAt = v; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime v) { this.lastHeartbeat = v; }
}
