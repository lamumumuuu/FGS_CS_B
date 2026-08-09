// src/main/java/com/example/computerassociation/config/OpenApiConfig.java

/**
 * Swagger/OpenAPI 配置类
 * 
 * 配置生成接口文档的基本信息、服务器地址以及 JWT 认证方式。
 * 文档默认访问路径：/swagger-ui.html
 * OpenAPI JSON 路径：/v3/api-docs
 */

package com.example.computerassociation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";  /// 安全方案名称

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())                                            /// API 文档基本信息
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("开发环境")   /// 显示在文档中的服务器地址
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))  /// 全局应用 Bearer Token 认证
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,          /// 定义安全方案
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));            /// 指定 token 格式为 JWT
    }

    /** 构建 API 文档的基本信息 */
    private Info apiInfo() {
        return new Info()
                .title("计算机协会管理系统 API")
                .version("1.0.0")
                .description("提供用户注册、登录、密码重置等功能")
                .contact(new Contact()
                        .name("开发团队")
                        .email("dev@example.com"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }
}