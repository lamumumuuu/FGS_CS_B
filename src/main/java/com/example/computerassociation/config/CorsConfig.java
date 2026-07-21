package com.example.computerassociation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径进行跨域请求
        registry.addMapping("/**")
                // 允许的请求源模式，使用allowedOriginPatterns代替allowedOrigins以支持通配符
                .allowedOriginPatterns("*")
                // 允许的请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 是否允许携带凭证（如Cookie）
                .allowCredentials(true)
                // 预检请求的有效期（秒）
                .maxAge(3600);
    }
}