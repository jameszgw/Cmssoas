package com.cmssoas.platform.customer.web;

import com.cmssoas.platform.customer.domain.Customer;
import com.cmssoas.platform.customer.service.CustomerService;
import com.cmssoas.platform.rbac.service.RequirePerm;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 客户管理 + 客户360。 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("customer:view")
    public List<Customer> list() {
        return service.list();
    }

    @GetMapping("/{id}/overview")
    @RequirePerm("customer:view")
    public Map<String, Object> overview(@PathVariable Long id) {
        return service.overview(id);
    }

    @PostMapping
    @RequirePerm("customer:edit")
    public Customer create(@RequestBody Customer body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    @RequirePerm("customer:edit")
    public Customer update(@PathVariable Long id, @RequestBody Customer body) {
        return service.update(id, body);
    }

    @GetMapping("/export.csv")
    @RequirePerm("customer:view")
    public ResponseEntity<byte[]> exportCsv() {
        var rows = service.list().stream().map(c -> java.util.List.<Object>of(
                c.getCode(), c.getName(), c.getContact() == null ? "" : c.getContact(),
                c.getEmail() == null ? "" : c.getEmail(), c.getPhone() == null ? "" : c.getPhone(),
                c.getIndustry() == null ? "" : c.getIndustry(), c.getStatus(), c.getCreatedAt())).toList();
        return com.cmssoas.platform.common.CsvUtil.response("customers.csv",
                java.util.List.of("code", "name", "contact", "email", "phone", "industry", "status", "createdAt"),
                rows);
    }
}
