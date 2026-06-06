package com.cmssoas.platform.online.service;

import com.cmssoas.platform.common.ApiException;
import com.cmssoas.platform.config.AppProperties;
import com.cmssoas.platform.license.domain.License;
import com.cmssoas.platform.license.domain.LicenseStatus;
import com.cmssoas.platform.license.repo.LicenseRepository;
import com.cmssoas.platform.license.service.Ed25519KeyService;
import com.cmssoas.platform.online.domain.LicenseInstance;
import com.cmssoas.platform.online.dto.OnlineDtos.*;
import com.cmssoas.platform.online.repo.LicenseInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 在线授权服务：激活、心跳（nonce 防重放）、浮动席位、实时吊销、宽限期。
 * 响应均由 Ed25519 服务端签名，SDK 用内置公钥验签（防伪造 + 可信时间）。
 */
@Service
public class OnlineService {

    private static final Logger log = LoggerFactory.getLogger(OnlineService.class);
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final String ACTIVE = "ACTIVE", RELEASED = "RELEASED", EXPIRED = "EXPIRED";

    private final LicenseRepository licenseRepo;
    private final LicenseInstanceRepository instanceRepo;
    private final NonceStore nonceStore;
    private final Ed25519KeyService keyService;
    private final AppProperties props;

    public OnlineService(LicenseRepository licenseRepo, LicenseInstanceRepository instanceRepo,
                         NonceStore nonceStore, Ed25519KeyService keyService, AppProperties props) {
        this.licenseRepo = licenseRepo;
        this.instanceRepo = instanceRepo;
        this.nonceStore = nonceStore;
        this.keyService = keyService;
        this.props = props;
    }

    private int graceSec() { return props.getOnline().getGraceSec(); }
    private LocalDateTime graceCutoff() { return LocalDateTime.now().minusSeconds(graceSec()); }

    // ---------- 激活 ----------
    @Transactional
    public ActivateResponse activate(ActivateRequest r, String ip) {
        License l = licenseRepo.findByLicenseId(r.licenseId())
                .orElseThrow(() -> ApiException.notFound("License 不存在"));
        ensureUsable(l);

        int total = l.getConcurrency();
        LicenseInstance inst = instanceRepo.findByLicenseIdAndInstanceId(r.licenseId(), r.instanceId()).orElse(null);
        if (inst == null) {
            long used = instanceRepo.countByLicenseIdAndStatusAndLastHeartbeatAfter(r.licenseId(), ACTIVE, graceCutoff());
            if (used >= total) {
                throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                        "并发席位已满（" + used + "/" + total + "），无法激活新实例");
            }
            inst = new LicenseInstance();
            inst.setLicenseId(r.licenseId());
            inst.setInstanceId(r.instanceId());
            inst.setActivatedAt(LocalDateTime.now());
        }
        inst.setMachineCode(r.machineCode());
        inst.setIp(ip);
        inst.setStatus(ACTIVE);
        inst.setLastHeartbeat(LocalDateTime.now());
        instanceRepo.save(inst);

        int used = (int) instanceRepo.countByLicenseIdAndStatusAndLastHeartbeatAfter(r.licenseId(), ACTIVE, graceCutoff());
        String serverTime = LocalDateTime.now().toString();
        String sig = sign(r.licenseId(), r.instanceId(), serverTime, "ACTIVATED");
        log.info("[online] 激活 {} / {} 席位 {}/{}", r.licenseId(), r.instanceId(), used, total);
        return new ActivateResponse(true, "激活成功", l.getLic(), serverTime, sig,
                props.getOnline().getHeartbeatSec(), graceSec(), used, total);
    }

    // ---------- 心跳 ----------
    @Transactional
    public HeartbeatResponse heartbeat(HeartbeatRequest r) {
        String nonceKey = r.licenseId() + ":" + r.instanceId() + ":" + r.nonce();
        if (!nonceStore.register(nonceKey)) {
            return new HeartbeatResponse(false, "REPLAY", "nonce 重复（疑似重放攻击）", now(), null, 0, 0);
        }
        License l = licenseRepo.findByLicenseId(r.licenseId()).orElse(null);
        if (l == null) return resp(false, "NOT_FOUND", "License 不存在", r);
        if (l.getStatus() == LicenseStatus.REVOKED) return resp(false, "REVOKED", "License 已被吊销，请停用", r);
        if (l.getNotAfter().isBefore(LocalDate.now())) return resp(false, "EXPIRED", "License 已过期", r);

        LicenseInstance inst = instanceRepo.findByLicenseIdAndInstanceId(r.licenseId(), r.instanceId()).orElse(null);
        if (inst == null || !ACTIVE.equals(inst.getStatus())) {
            return resp(false, "NOT_ACTIVATED", "实例未激活或已释放，请重新激活", r);
        }
        inst.setLastHeartbeat(LocalDateTime.now());
        instanceRepo.save(inst);
        return resp(true, "OK", "ok", r);
    }

    // ---------- 主动反激活（释放席位）----------
    @Transactional
    public void deactivate(DeactivateRequest r) {
        instanceRepo.findByLicenseIdAndInstanceId(r.licenseId(), r.instanceId()).ifPresent(i -> {
            i.setStatus(RELEASED);
            instanceRepo.save(i);
        });
    }

    // ---------- 监控 ----------
    public List<InstanceView> instances() {
        LocalDateTime now = LocalDateTime.now();
        return instanceRepo.findAllByOrderByLastHeartbeatDesc().stream()
                .map(i -> InstanceView.of(i, stateOf(i, now))).toList();
    }

    public List<SeatUsage> seatUsage() {
        List<SeatUsage> out = new ArrayList<>();
        for (License l : licenseRepo.findByStatus(LicenseStatus.ACTIVE)) {
            int used = (int) instanceRepo.countByLicenseIdAndStatusAndLastHeartbeatAfter(
                    l.getLicenseId(), ACTIVE, graceCutoff());
            if (used > 0 || l.getConcurrency() > 0) {
                out.add(new SeatUsage(l.getLicenseId(), l.getCustomer(), used, l.getConcurrency()));
            }
        }
        return out;
    }

    public OnlineStats stats() {
        LocalDateTime now = LocalDateTime.now();
        int online = 0, grace = 0;
        for (LicenseInstance i : instanceRepo.findByStatus(ACTIVE)) {
            String s = stateOf(i, now);
            if ("online".equals(s)) online++;
            else if ("grace".equals(s)) grace++;
        }
        return new OnlineStats(online, grace, online + grace);
    }

    // ---------- 定时回收过期席位 ----------
    @Scheduled(fixedDelayString = "30000")
    @Transactional
    public void reapStaleSeats() {
        LocalDateTime cutoff = graceCutoff();
        List<LicenseInstance> stale = instanceRepo.findByStatus(ACTIVE).stream()
                .filter(i -> i.getLastHeartbeat().isBefore(cutoff)).toList();
        for (LicenseInstance i : stale) {
            i.setStatus(EXPIRED);
            instanceRepo.save(i);
        }
        if (!stale.isEmpty()) log.info("[online] 回收超期席位 {} 个", stale.size());
    }

    // ---------- 内部 ----------
    private void ensureUsable(License l) {
        if (l.getStatus() == LicenseStatus.REVOKED) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "License 已被吊销");
        if (l.getNotAfter().isBefore(LocalDate.now())) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "License 已过期");
    }

    private String stateOf(LicenseInstance i, LocalDateTime now) {
        if (RELEASED.equals(i.getStatus())) return "released";
        if (EXPIRED.equals(i.getStatus())) return "offline";
        long secs = java.time.Duration.between(i.getLastHeartbeat(), now).getSeconds();
        if (secs <= props.getOnline().getHeartbeatSec() * 3L) return "online";
        if (secs <= graceSec()) return "grace";
        return "offline";
    }

    private HeartbeatResponse resp(boolean ok, String status, String msg, HeartbeatRequest r) {
        String serverTime = now();
        String sig = sign(r.licenseId(), r.instanceId(), serverTime, status);
        License l = licenseRepo.findByLicenseId(r.licenseId()).orElse(null);
        int total = l == null ? 0 : l.getConcurrency();
        int used = l == null ? 0 : (int) instanceRepo.countByLicenseIdAndStatusAndLastHeartbeatAfter(
                r.licenseId(), ACTIVE, graceCutoff());
        return new HeartbeatResponse(ok, status, msg, serverTime, sig, used, total);
    }

    private String sign(String licenseId, String instanceId, String serverTime, String status) {
        String payload = String.join("|", licenseId, instanceId, serverTime, status);
        return B64U.encodeToString(keyService.sign(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String now() { return LocalDateTime.now().toString(); }
}
