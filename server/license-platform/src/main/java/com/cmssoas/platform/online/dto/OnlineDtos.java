package com.cmssoas.platform.online.dto;

import com.cmssoas.platform.online.domain.LicenseInstance;
import jakarta.validation.constraints.NotBlank;

/** 在线授权（SDK 通道 + 监控）DTO 集合。 */
public final class OnlineDtos {
    private OnlineDtos() {}

    // ---- SDK 通道 ----
    public record ActivateRequest(
            @NotBlank String licenseId,
            @NotBlank String instanceId,
            String machineCode
    ) {}

    public record ActivateResponse(
            boolean ok, String message,
            String authSnapshot,   // 服务端已签名的 .lic
            String serverTime,
            String signature,      // 对响应的 Ed25519 签名
            int heartbeatSec, int graceSec,
            int seatsUsed, int seatsTotal
    ) {}

    public record HeartbeatRequest(
            @NotBlank String licenseId,
            @NotBlank String instanceId,
            @NotBlank String nonce,
            Long ts
    ) {}

    public record HeartbeatResponse(
            boolean ok, String status, String message,
            String serverTime, String signature,
            int seatsUsed, int seatsTotal
    ) {}

    public record DeactivateRequest(
            @NotBlank String licenseId,
            @NotBlank String instanceId
    ) {}

    // ---- 监控 ----
    public record InstanceView(
            String licenseId, String instanceId, String machineCode, String ip,
            String state,          // online / grace / offline / released
            String activatedAt, String lastHeartbeat
    ) {
        public static InstanceView of(LicenseInstance i, String state) {
            return new InstanceView(i.getLicenseId(), i.getInstanceId(), i.getMachineCode(), i.getIp(),
                    state, i.getActivatedAt().toString(), i.getLastHeartbeat().toString());
        }
    }

    public record SeatUsage(String licenseId, String customer, int used, int total) {}

    public record OnlineStats(int onlineInstances, int graceInstances, int totalSeatsUsed) {}
}
