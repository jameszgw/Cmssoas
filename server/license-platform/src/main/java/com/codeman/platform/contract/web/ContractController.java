package com.codeman.platform.contract.web;

import com.codeman.platform.contract.domain.Contract;
import com.codeman.platform.contract.domain.ContractParty;
import com.codeman.platform.contract.domain.ContractTemplate;
import com.codeman.platform.contract.dto.ContractDtos.CreateReq;
import com.codeman.platform.contract.dto.ContractDtos.TemplateReq;
import com.codeman.platform.contract.service.ContractService;
import com.codeman.platform.rbac.service.RequirePerm;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 合同签约接口：模板、合同、发起签署、各方签署、作废、导出。 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    // ---- 模板 ----
    @GetMapping("/templates")
    @RequirePerm("contract:view")
    public List<ContractTemplate> templates() {
        return service.templates();
    }

    @PostMapping("/templates")
    @RequirePerm("contract:edit")
    public ContractTemplate createTemplate(@RequestBody TemplateReq r) {
        return service.createTemplate(r.name(), r.contentHtml(), r.variables());
    }

    // ---- 合同 ----
    @GetMapping
    @RequirePerm("contract:view")
    public List<Contract> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @RequirePerm("contract:view")
    public Map<String, Object> detail(@PathVariable Long id) {
        return Map.of("contract", service.get(id), "parties", service.parties(id));
    }

    @PostMapping
    @RequirePerm("contract:edit")
    public Contract create(@RequestBody CreateReq r) {
        return service.create(r);
    }

    @PostMapping("/{id}/send")
    @RequirePerm("contract:edit")
    public Contract send(@PathVariable Long id) {
        return service.send(id);
    }

    @PostMapping("/{id}/sign/{partyId}")
    @RequirePerm("contract:sign")
    public Contract sign(@PathVariable Long id, @PathVariable Long partyId) {
        return service.sign(id, partyId);
    }

    @PostMapping("/{id}/void")
    @RequirePerm("contract:edit")
    public Contract voidContract(@PathVariable Long id) {
        return service.voidContract(id);
    }

    @GetMapping("/export.csv")
    @RequirePerm("contract:view")
    public ResponseEntity<byte[]> exportCsv() {
        var rows = service.list().stream().map(c -> java.util.List.<Object>of(
                c.getId(), c.getContractNo() == null ? "" : c.getContractNo(), c.getCustomer(),
                c.getTitle(), c.getAmount(), c.getStatus(),
                c.getContentHash() == null ? "" : c.getContentHash(), c.getCreatedAt())).toList();
        return com.codeman.platform.common.CsvUtil.response("contracts.csv",
                java.util.List.of("id", "contractNo", "customer", "title", "amount", "status", "contentHash", "createdAt"),
                rows);
    }
}
