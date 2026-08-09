// src/main/java/com/example/computerassociation/config/PasswordEncoderConfig.java

/**
 * 密码编码器配置
 * 
 * 单独提取 BCryptPasswordEncoder Bean，供 Spring Security 及其他模块注入使用。
 * 密码存储前会进行哈希加密，验证时自动比对。
 */

package com.example.computerassociation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}