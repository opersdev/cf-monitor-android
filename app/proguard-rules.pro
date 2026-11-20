# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# 保留数据模型（反射不依赖，但避免混淆字段名影响调试）
-keep class com.cfsm.monitor.data.** { *; }
