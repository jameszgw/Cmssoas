package com.cmssoas.platform.rbac.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "permission")
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 48) private String code;
    @Column(nullable = false, length = 64) private String name;
    @Column(name = "parent_code", length = 48) private String parentCode;
    @Column(nullable = false, length = 16) private String type;
    @Column(nullable = false) private int sort;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getParentCode() { return parentCode; }
    public String getType() { return type; }
    public int getSort() { return sort; }
}
