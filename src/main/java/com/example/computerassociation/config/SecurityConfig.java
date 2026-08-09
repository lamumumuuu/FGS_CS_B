// src/main/java/com/example/computerassociation/config/SecurityConfig.java

/**
 * Spring Security 核心配置类
 * 
 * 定义整个应用的认证授权策略：关闭 CSRF、启用基于 JWT 的无状态会话、
 * 放行公开接口（登录/注册/Swagger/健康检查）、其余接口均需认证。
 * 通过注入 JwtAuthenticationFilter 在请求到达 Controller 前完成 Token 校验。
 */

package com.example.computerassociation.config;

import com.example.computerassociation.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity                                /// 启用 Spring Security 的 Web 安全支持
public class SecurityConfig {

    /* ------------------------------------------------------------------ */
    /*  从配置文件注入 CORS 参数                                          */
    /* ------------------------------------------------------------------ */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;                /// 允许的前端源地址

    @Value("${cors.allowed-methods}")
    private String allowedMethods;                /// 允许的 HTTP 方法

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;                /// 允许的请求头

    @Value("${cors.max-age}")
    private long maxAge;                          /// 预检请求缓存时间

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;  /// JWT 认证过滤器

    /* ------------------------------------------------------------------ */
    /*  安全过滤链：定义认证与授权规则                                    */
    /* ------------------------------------------------------------------ */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)                            // 前后端分离无需 CSRF 防护
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 应用自定义 CORS 配置
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态会话
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/user/register").permitAll()  // 注册公开
                        .requestMatchers(HttpMethod.POST, "/api/user/login").permitAll()     // 登录公开
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()              // 预检请求公开
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()                    // 健康检查公开
                        .anyRequest().authenticated()                                       // 其余全部需认证
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);  // 注册 JWT 过滤器

        return http.build();
    }

    /* ------------------------------------------------------------------ */
    /*  CORS 配置源：从环境变量读取白名单                                  */
    /* ------------------------------------------------------------------ */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));    // 允许的来源
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));            // 允许的方法
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));            // 允许的请求头
        configuration.setAllowCredentials(true);                                              // 允许携带凭证
        configuration.setMaxAge(maxAge);                                                      // 预检缓存时间

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);    // 对所有路径生效
        return source;
    }
}