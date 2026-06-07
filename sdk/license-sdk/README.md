# CMSSOAS License SDK（客户端）

嵌入被保护 Spring Boot 应用的轻量库：**Ed25519 验签** + `.lic` 读取 + **功能/版本/有效期门禁**。
客户端只持有公钥，**无法伪造合法 License**；没有合法授权则核心功能被拒绝（fail-closed）。

## 核心 API
```java
LicenseVerifier verifier = new LicenseVerifier(publicKeyBase64); // 公钥内置
LicenseClaims claims = verifier.verify(licString);              // 验签失败抛 LicenseException

claims.isCurrentlyValid();          // 有效期 + 状态
claims.hasModule("RISK");           // 模块授权
claims.hasFeature("REPORT.EXPORT"); // 功能点授权
claims.quota("MAX_USERS");          // 配额
claims.appVersionAllowed("2.4.0");  // 按版本授权（区间判断）
```

## Spring 门禁示例（@RequiresFeature 思路）
```java
@Around("@annotation(req)")
public Object gate(ProceedingJoinPoint pjp, RequiresFeature req) throws Throwable {
    if (!CurrentLicense.get().hasFeature(req.value()))
        throw new LicenseDeniedException(req.value());
    return pjp.proceed();
}
```

## 演示
```bash
mvn -q -DskipTests package
PUB=$(curl -s localhost:8080/api/licenses/public-key | sed -E 's/.*"publicKeyBase64":"([^"]+)".*/\1/')
curl -s localhost:8080/api/licenses/LIC-2026-0001/download -o demo.lic

java -jar target/license-sdk-demo.jar info  "$PUB" demo.lic
java -jar target/license-sdk-demo.jar check "$PUB" demo.lic REPORT.EXPORT 2.4.0   # 放行/拒绝
```
篡改 `.lic` 任意字符后再 `check` → 验签失败 → 拒绝运行。

> 生产：公钥随包内置（或多公钥 + kid 轮换）；关键校验逻辑可下沉 Native（见方案 02）。
