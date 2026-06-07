# ProGuard 参考配置（混淆层 L1）—— 与本示例的“加密+License绑定”叠加形成纵深防御。
# 用法（示例）：proguard @proguard.pro  （或 proguard-maven-plugin）
# 说明：仅混淆核心业务包，放过反射/序列化边界，避免运行期失败。

-injars  target/classes
-outjars target/obf
-libraryjars <java.home>/jmods

# 优化与混淆强度
-dontshrink
-optimizationpasses 3
-overloadaggressive
-repackageclasses 'o'
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*

# 保留入口（启动器需被 java -cp 调用）
-keep public class com.codeman.protect.ProtectedLauncher { public static void main(java.lang.String[]); }
-keep public class com.codeman.protect.BuildProtected   { public static void main(java.lang.String[]); }

# 受保护业务类通过反射 + 自定义类加载器加载，保留其名以便 defineClass/反射
-keep class com.codeman.protect.secret.SecretAlgorithm { *; }

# 字符串加密（需商用混淆器如 Allatori 才支持；ProGuard 无此功能）
# Allatori 可对 CryptoKeys 中的根密钥常量、类名字符串做加密，进一步抬高门槛。

# Spring Boot 项目额外 keep（按需）：
# -keep @org.springframework.stereotype.* class * { *; }
# -keepclassmembers class * { @org.springframework.beans.factory.annotation.Autowired *; }
