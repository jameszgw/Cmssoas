package com.codeman.platform.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "plan")
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 32) private String code;
    @Column(nullable = false, length = 64) private String name;
    @Column(name = "plan_key", nullable = false, length = 32) private String planKey;
    @Column(nullable = false) private int price;
    @Column(name = "version_range", nullable = false, length = 64) private String versionRange;
    @Column(nullable = false) private int seats;
    @Column(nullable = false, length = 512) private String modules;
    @Column(nullable = false, length = 2000) private String features;
    @Column(nullable = false, length = 16) private String status;
    @Column(nullable = false) private int sort;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getPlanKey() { return planKey; }
    public int getPrice() { return price; }
    public String getVersionRange() { return versionRange; }
    public int getSeats() { return seats; }
    public String getModules() { return modules; }
    public String getFeatures() { return features; }
    public String getStatus() { return status; }
    public int getSort() { return sort; }
}
