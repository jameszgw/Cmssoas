# 代码保护演示（方案 02 落地）

可运行示例：**类字节码 AES 加密 + 解密密钥与 License 绑定**，并复用 `license-sdk` 做 Ed25519 验签。
核心结论：**没有合法 License、被篡改、或换一张 License，都无法解密运行受保护的业务代码（fail-closed）。**

## 一键演示
```bash
cd examples/protected-app
bash demo.sh
```
输出（节选）：
```
A. 合法 License            -> ✅ 验签通过 -> 🟢 SECRET-RESULT[42]=...
B. 另一张合法签名 License   -> ✅ 验签通过 -> ⛔ 解密失败（密钥与该 License 不匹配）
C. 被篡改 License          -> ⛔ 验签未通过
```

## 它演示了什么（对应方案 02）
| 层 | 本示例做法 |
|----|-----------|
| L2 字节码加密 | 构建期把 `SecretAlgorithm.class` 用 AES/CBC 加密为 `.class.enc`，并**删除明文 class**；运行期由自定义 `EncryptedClassLoader` 在内存解密后 `defineClass`。 |
| 与 License 绑定（创新点 §4） | 解密密钥 `= SHA-256(产品根密钥 \| licenseId \| watermark)`（`CryptoKeys`）。密钥从 License 派生 → 脱壳与破解 License 两个难题被耦合。 |
| 验签信任根 | `ProtectedLauncher` 用 `license-sdk` 的 `LicenseVerifier`（内置公钥）验签，伪造/篡改即被拦截。 |
| L1 混淆 | `proguard.pro` 参考配置（混淆核心包、保留反射边界）；字符串加密需 Allatori 等商用混淆器。 |
| L3 Native / L4 反调试 | 见方案 02：把 `CryptoKeys`/验签下沉 JNI/GraalVM、加反调试与完整性自校验，进一步抬高门槛。 |

## 关键文件
- `secret/SecretAlgorithm.java` —— 受保护的“核心 IP”
- `CryptoKeys.java` —— 解密密钥派生（KDF，绑定 License）+ AES
- `BuildProtected.java` —— 构建期：签发 License、加密业务类、移除明文
- `EncryptedClassLoader.java` / `ProtectedLauncher.java` —— 运行期：验签→派生密钥→解密→加载运行
- `proguard.pro` —— 混淆层参考配置

## L1 混淆已接入构建流水线（ProGuard）
```bash
mvn -Pharden -DskipTests package          # 产出 target/protected-app-1.0.0-obf.jar
jar tf target/protected-app-1.0.0-obf.jar # 观察类名
```
混淆前后对比（实测）：
```
混淆前： CryptoKeys.class  EncryptedClassLoader.class  BuildProtected.class  ProtectedLauncher.class  secret/SecretAlgorithm.class
混淆后： a.class           b.class                     BuildProtected.class  ProtectedLauncher.class  secret/SecretAlgorithm.class
```
即：辅助/密钥派生类被改名为 `a/b`（抬高逆向门槛），入口 main 与被类加载器按全名加载的 `SecretAlgorithm` 保留。CI 中 `harden` job 会自动产出并校验该混淆制品。

> 字符串加密（如对根密钥常量、类名字符串加密）需 Allatori/DashO 等商用混淆器。

## Spring Boot 胖包加密（Xjar，可选）
对 Spring Boot 应用，可用 Xjar 对 `BOOT-INF/classes` 整体加密，运行时以密码/密钥解密启动：
```xml
<!-- 第三方插件，需配置其仓库；示意 -->
<plugin>
  <groupId>com.github.core-lib</groupId>
  <artifactId>xjar-maven-plugin</artifactId>
  <version>4.0.2</version>
  <executions>
    <execution>
      <goals><goal>build</goal></goals>
      <configuration><password>${env.XJAR_PASSWORD}</password></configuration>
    </execution>
  </executions>
</plugin>
```
与本示例的「类级加密 + 解密密钥绑定 License」思路一致，可二选一或叠加；本仓库以可运行的自实现方案为准，Xjar 作为生产替代方案备选。

## 现实边界
纯软件保护无法 100% 阻止逆向；本示例展示如何**显著抬高门槛**并把代码与授权强绑定。
生产应：根密钥入 KMS/HSM、关键逻辑 Native 化、叠加商用混淆与反调试/反 dump、配合在线吊销与水印溯源（见方案 02/06）。
