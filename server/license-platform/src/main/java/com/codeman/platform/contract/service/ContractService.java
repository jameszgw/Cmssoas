package com.codeman.platform.contract.service;

import com.codeman.platform.billing.service.InvoiceService;
import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.contract.domain.Contract;
import com.codeman.platform.contract.domain.ContractParty;
import com.codeman.platform.contract.domain.ContractTemplate;
import com.codeman.platform.contract.dto.ContractDtos.CreateReq;
import com.codeman.platform.contract.dto.ContractDtos.PartyReq;
import com.codeman.platform.contract.repo.ContractPartyRepository;
import com.codeman.platform.contract.repo.ContractRepository;
import com.codeman.platform.contract.repo.ContractTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

/**
 * 合同签约服务(自建电子签：哈希+时间戳存证留痕)。
 *
 * <p>流程：按模板填充生成合同(DRAFT) → 发起签署(SENT，内容快照 + SHA-256 防篡改) →
 * 各方逐一签署(每方一条签署存证哈希) → 全部签署后合同 SIGNED 并归档；金额>0 时联动账单出账。
 * 如需更强法律效力(CA 实名/可信时间戳/司法存证)，可在不改业务流程的前提下替换为第三方电子签实现。
 */
@Service
public class ContractService {

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ContractRepository repo;
    private final ContractPartyRepository partyRepo;
    private final ContractTemplateRepository tplRepo;
    private final InvoiceService invoices;
    private final AuditWriter audit;

    public ContractService(ContractRepository repo, ContractPartyRepository partyRepo,
                           ContractTemplateRepository tplRepo, InvoiceService invoices, AuditWriter audit) {
        this.repo = repo;
        this.partyRepo = partyRepo;
        this.tplRepo = tplRepo;
        this.invoices = invoices;
        this.audit = audit;
    }

    // ---- 模板 ----
    public List<ContractTemplate> templates() {
        return tplRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public ContractTemplate createTemplate(String name, String contentHtml, String variables) {
        ContractTemplate t = new ContractTemplate();
        t.setName(name);
        t.setContentHtml(contentHtml);
        t.setVariables(variables);
        t.setCreatedAt(LocalDateTime.now());
        return tplRepo.save(t);
    }

    // ---- 合同 ----
    public List<Contract> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Contract get(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("合同不存在"));
    }

    public List<ContractParty> parties(Long contractId) {
        return partyRepo.findByContractId(contractId);
    }

    @Transactional
    public Contract create(CreateReq r) {
        if (r.customer() == null || r.customer().isBlank()) throw ApiException.badRequest("客户名称必填");
        String content = r.contentHtml();
        if (r.templateId() != null) {
            ContractTemplate t = tplRepo.findById(r.templateId())
                    .orElseThrow(() -> ApiException.notFound("模板不存在"));
            content = render(t.getContentHtml(), r);
        }
        if (content == null || content.isBlank()) throw ApiException.badRequest("合同内容为空(请选模板或填写内容)");

        Contract c = new Contract();
        c.setTemplateId(r.templateId());
        c.setTenantCode(r.tenantCode());
        c.setCustomer(r.customer());
        c.setSubscriptionId(r.subscriptionId());
        c.setPlanCode(r.planCode());
        c.setTitle(r.title() == null || r.title().isBlank() ? (r.customer() + " 服务合同") : r.title());
        c.setContentHtml(content);
        c.setAmount(r.amount() == null ? 0 : r.amount());
        c.setStatus("DRAFT");
        c.setCreatedAt(LocalDateTime.now());
        repo.save(c);

        if (r.parties() != null) {
            for (PartyReq p : r.parties()) {
                if (p.name() == null || p.name().isBlank()) continue;
                ContractParty cp = new ContractParty();
                cp.setContractId(c.getId());
                cp.setName(p.name());
                cp.setPartyRole(p.partyRole());
                cp.setEmail(p.email());
                cp.setPhone(p.phone());
                cp.setSignStatus("PENDING");
                partyRepo.save(cp);
            }
        }
        audit.log(null, "CONTRACT_CREATE", "合同#" + c.getId() + " · " + c.getTitle());
        return c;
    }

    /** 发起签署：内容定稿快照 + SHA-256 存证，生成合同编号。 */
    @Transactional
    public Contract send(Long id) {
        Contract c = get(id);
        if (!"DRAFT".equals(c.getStatus())) throw ApiException.badRequest("仅草稿可发起签署");
        if (partyRepo.findByContractId(id).isEmpty()) throw ApiException.badRequest("请先添加至少一个签署方");
        c.setContractNo("HT-" + LocalDateTime.now().format(NO) + "-" + id);
        c.setContentHash(sha256(c.getContentHtml()));
        c.setStatus("SENT");
        c.setSentAt(LocalDateTime.now());
        repo.save(c);
        audit.log(null, "CONTRACT_SEND", "合同 " + c.getContractNo() + " 已发起签署");
        return c;
    }

    /** 某签署方完成签署；全部签署后合同生效并(金额>0)联动出账。 */
    @Transactional
    public Contract sign(Long contractId, Long partyId) {
        Contract c = get(contractId);
        if (!("SENT".equals(c.getStatus()) || "SIGNING".equals(c.getStatus())))
            throw ApiException.badRequest("当前合同状态不可签署");
        ContractParty p = partyRepo.findById(partyId)
                .orElseThrow(() -> ApiException.notFound("签署方不存在"));
        if (!contractId.equals(p.getContractId())) throw ApiException.badRequest("签署方与合同不匹配");
        if ("SIGNED".equals(p.getSignStatus())) throw ApiException.badRequest("该签署方已签署");

        LocalDateTime now = LocalDateTime.now();
        p.setSignStatus("SIGNED");
        p.setSignedAt(now);
        p.setSignHash(sha256(c.getContentHash() + "|" + p.getName() + "|" + now));
        partyRepo.save(p);
        audit.log(null, "CONTRACT_SIGN", "合同 " + c.getContractNo() + " · " + p.getName() + " 已签署");

        List<ContractParty> all = partyRepo.findByContractId(contractId);
        boolean allSigned = all.stream().allMatch(x -> "SIGNED".equals(x.getSignStatus()));
        if (allSigned) {
            c.setStatus("SIGNED");
            c.setSignedAt(now);
            repo.save(c);
            audit.log(null, "CONTRACT_SIGNED", "合同 " + c.getContractNo() + " 全部签署完成，已存证归档");
            if (c.getAmount() > 0) {
                invoices.bill(c.getTenantCode() == null ? "-" : c.getTenantCode(), c.getCustomer(),
                        c.getSubscriptionId(), c.getPlanCode(), "CONTRACT", c.getAmount(),
                        "合同 " + c.getContractNo());
            }
        } else {
            c.setStatus("SIGNING");
            repo.save(c);
        }
        return c;
    }

    @Transactional
    public Contract voidContract(Long id) {
        Contract c = get(id);
        if ("SIGNED".equals(c.getStatus())) throw ApiException.badRequest("已签署合同不可作废");
        c.setStatus("VOID");
        repo.save(c);
        audit.log(null, "CONTRACT_VOID", "合同#" + id + " 已作废");
        return c;
    }

    // ---- 工具 ----
    private String render(String tpl, CreateReq r) {
        return tpl.replace("{{customer}}", nz(r.customer()))
                .replace("{{amount}}", r.amount() == null ? "0" : String.valueOf(r.amount()))
                .replace("{{plan}}", nz(r.planCode()))
                .replace("{{tenant}}", nz(r.tenantCode()))
                .replace("{{date}}", LocalDate.now().toString());
    }

    private String nz(String s) { return s == null ? "" : s; }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
