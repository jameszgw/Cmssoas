package com.cmssoas.platform.catalog.web;

import com.cmssoas.platform.catalog.dto.CatalogDtos.*;
import com.cmssoas.platform.catalog.service.CatalogService;
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
    public List<ProductView> products() {
        return service.products();
    }

    @GetMapping("/matrix")
    public MatrixView matrix() {
        return service.matrix();
    }
}
