package com.codeman.platform.catalog.web;

import com.codeman.platform.catalog.dto.CatalogDtos.*;
import com.codeman.platform.catalog.service.CatalogService;
import com.codeman.platform.rbac.service.RequirePerm;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/products")
    @RequirePerm("catalog:view")
    public List<ProductView> products() {
        return service.products();
    }

    @GetMapping("/matrix")
    @RequirePerm("catalog:view")
    public MatrixView matrix() {
        return service.matrix();
    }
}
