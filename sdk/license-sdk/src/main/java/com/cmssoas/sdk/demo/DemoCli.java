package com.cmssoas.sdk.demo;

import com.cmssoas.sdk.LicenseClaims;
import com.cmssoas.sdk.LicenseException;
import com.cmssoas.sdk.LicenseVerifier;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 演示客户端如何用 SDK 做功能门禁：
 *   info  <publicKeyB64> <lic 文件>                      —— 验签并打印授权内容
 *   check <publicKeyB64> <lic 文件> <功能点> [软件版本]    —— 判断某功能在当前授权下是否放行
 *
 * 关键点：没有合法 .lic（或被篡改/过期）时，受保护功能将被拒绝（fail-closed）。
 */
public class DemoCli {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("用法:");
            System.out.println("  info  <publicKeyB64> <licFile>");
            System.out.println("  check <publicKeyB64> <licFile> <feature> [appVersion]");
            System.exit(2);
        }
        String cmd = args[0];
        String pub = args[1];
        String lic = Files.readString(Path.of(args[2])).trim();

        LicenseVerifier verifier = new LicenseVerifier(pub);

        // —— 1) 验签（伪造/篡改将在此抛出）——
        LicenseClaims claims;
        try {
            claims = verifier.verify(lic);
        } catch (LicenseException e) {
            System.out.println("❌ 验签失败 -> 拒绝运行：" + e.getMessage());
            System.exit(1);
            return;
        }

        if ("info".equals(cmd)) {
            System.out.println("✅ 验签通过（签名合法）");
            System.out.println("  licenseId      : " + claims.licenseId() + "  v" + claims.licenseVersion());
            System.out.println("  customer       : " + claims.customer());
            System.out.println("  edition/mode   : " + claims.edition() + " / " + claims.mode());
            System.out.println("  status         : " + claims.status());
            System.out.println("  valid period   : " + claims.notBefore() + " ~ " + claims.notAfter()
                    + "  (当前有效=" + claims.isCurrentlyValid() + ")");
            System.out.println("  appVersionRange: " + claims.appVersionRange());
            System.out.println("  modules        : " + claims.modules());
            System.out.println("  features       : " + claims.features());
            System.out.println("  watermark      : " + claims.watermark());
            return;
        }

        if ("check".equals(cmd)) {
            String feature = args[3];
            String appVersion = args.length >= 5 ? args[4] : null;

            System.out.println("功能门禁判定 -> feature=" + feature
                    + (appVersion != null ? ", appVersion=" + appVersion : ""));
            if (!claims.isCurrentlyValid()) {
                deny("License 不在有效期或非 ACTIVE（status=" + claims.status() + "）");
            }
            if (appVersion != null && !claims.appVersionAllowed(appVersion)) {
                deny("软件版本 " + appVersion + " 不在授权范围 " + claims.appVersionRange());
            }
            if (!claims.hasFeature(feature)) {
                deny("功能未授权：" + feature);
            }
            System.out.println("✅ 放行：功能 [" + feature + "] 已授权且当前有效");
            return;
        }

        System.out.println("未知命令：" + cmd);
        System.exit(2);
    }

    private static void deny(String reason) {
        System.out.println("⛔ 拒绝：" + reason);
        System.exit(1);
    }
}
