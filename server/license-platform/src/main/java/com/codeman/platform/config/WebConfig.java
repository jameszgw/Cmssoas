package com.codeman.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        // 公开通道对浏览器端开放跨域(无凭据):
        //  - /sdk/**:在线授权(激活/心跳/反激活)——CmPrint 等浏览器客户端从集成方域名直调,
        //    鉴权靠 licenseId+服务端校验(吊销/席位/nonce),非 Cookie;
        //  - /pub/**:模板库(galleryKey)/CRL/公钥 等本就按密钥或公开分发的端点。
        registry.addMapping("/sdk/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
        registry.addMapping("/pub/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
