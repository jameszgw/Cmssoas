package com.codeman.platform.harden.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 在线加固任务:上传源 jar → 异步按所选技术流水线加固 → 产出可下载产物。 */
@Entity
@Table(name = "harden_job")
public class HardenJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_no", nullable = false, length = 40) private String jobNo;
    @Column(name = "tenant_code", length = 32) private String tenantCode;
    @Column(name = "source_name", nullable = false, length = 200) private String sourceName;
    @Column(name = "source_size", nullable = false) private long sourceSize;
    @Column(nullable = false, length = 128) private String techniques;   // CSV: OBFUSCATE,ENCRYPT_BIND,FATJAR_ENCRYPT
    @Column(name = "bind_license", length = 40) private String bindLicense;
    @Column(nullable = false, length = 16) private String status;        // QUEUED/RUNNING/DONE/FAILED
    @Column(length = 1000) private String message;
    @Column(name = "out_size") private Long outSize;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "finished_at") private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public String getJobNo() { return jobNo; }
    public void setJobNo(String v) { this.jobNo = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String v) { this.sourceName = v; }
    public long getSourceSize() { return sourceSize; }
    public void setSourceSize(long v) { this.sourceSize = v; }
    public String getTechniques() { return techniques; }
    public void setTechniques(String v) { this.techniques = v; }
    public String getBindLicense() { return bindLicense; }
    public void setBindLicense(String v) { this.bindLicense = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public Long getOutSize() { return outSize; }
    public void setOutSize(Long v) { this.outSize = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime v) { this.finishedAt = v; }
}
