#!/usr/bin/env bash
# 代码保护演示：类加密 + 解密密钥与 License 绑定
set -e
cd "$(dirname "$0")"

echo "### 1) 编译"
mvn -q compile
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"

echo "### 2) 构建保护：加密 SecretAlgorithm.class + 移除明文 + 签发 License"
java -cp "$CP" com.cmssoas.protect.BuildProtected target/classes target/protected

echo "### 3) 确认交付物中已无明文业务类"
if [ -f target/classes/com/cmssoas/protect/secret/SecretAlgorithm.class ]; then
  echo "  !! 明文仍存在(异常)"; else echo "  OK: SecretAlgorithm.class 明文已移除，仅存在加密体:"; fi
ls -l target/protected/SecretAlgorithm.class.enc

echo; echo "=== A. 合法 License（解密密钥匹配）==="
java -cp "$CP" com.cmssoas.protect.ProtectedLauncher target/protected license.lic || true

echo; echo "=== B. 另一张合法签名的 License（验签通过，但密钥不匹配）==="
java -cp "$CP" com.cmssoas.protect.ProtectedLauncher target/protected license-other.lic || true

echo; echo "=== C. 被篡改的 License（验签失败）==="
sed 's/^./Z/' target/protected/license.lic > target/protected/license-bad.lic
java -cp "$CP" com.cmssoas.protect.ProtectedLauncher target/protected license-bad.lic || true

echo; echo "### 完成"
