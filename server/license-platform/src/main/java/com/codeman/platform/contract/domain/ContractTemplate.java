package com.codeman.platform.contract.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 合同模板。content_html 可含占位符 {{customer}} {{amount}} {{plan}} {{tenant}} {{date}}，生成合同时填充。 */
@Entity
@Table(name = "contract_template")
public class ContractTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "content_html", nullable = false, columnDefinition = "text") private String contentHtml;
    @Column(length = 256) private String variables;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String v) { this.contentHtml = v; }
    public String getVariables() { return variables; }
    public void setVariables(String v) { this.variables = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
