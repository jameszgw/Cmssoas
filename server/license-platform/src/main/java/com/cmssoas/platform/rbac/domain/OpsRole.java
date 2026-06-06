package com.cmssoas.platform.rbac.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ops_role")
public class OpsRole {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 32) private String code;
    @Column(nullable = false, length = 64) private String name;
    @Column(length = 128) private String description;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
