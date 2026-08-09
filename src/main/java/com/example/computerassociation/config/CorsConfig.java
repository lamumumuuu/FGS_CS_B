// src/main/java/com/example/computerassociation/config/CorsConfig.java

/**
 * 跨域配置类
 * 
 * 允许前端从不同源（如 localhost:3000）访问后端 API。
 * 当前使用通配符 "*" 允许所有来源，开发阶段方便，生产环境应改为具体域名。
 */

package com.example.computerassociation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                       /// 所有接口路径都允许跨域
                .allowedOriginPatterns("*")              /// 允许任何来源（使用模式匹配，避免与 allowCredentials 冲突）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  /// 允许的 HTTP 方法
                .allowedHeaders("*")                     /// 允许携带任何请求头
                .allowCredentials(true)                  /// 允许发送 Cookie 等凭证
                .maxAge(3600);                           /// 预检请求缓存 3600 秒（1 小时）
    }
}