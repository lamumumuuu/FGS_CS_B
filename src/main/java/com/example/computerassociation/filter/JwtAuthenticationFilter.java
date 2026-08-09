// src/main/java/com/example/computerassociation/filter/JwtAuthenticationFilter.java

/**
 * JWT 认证过滤器
 * 
 * 每个请求到达 Controller 前被调用一次（继承 OncePerRequestFilter）。
 * 从 Authorization 头中提取 Bearer Token，解析用户名，加载用户信息，
 * 并设置到 Spring Security 上下文和自定义 UserContext 中。
 * 请求处理完成后清除 ThreadLocal，防止内存泄漏。
 */

package com.example.computerassociation.filter;

import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.service.UserService;
import com.example.computerassociation.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;                /// JWT 工具（解析、校验）

    @Autowired
    private UserService userService;        /// 用户服务（查询用户信息）

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            String username = null;

            // 提取 Bearer Token
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);                        // 去掉 "Bearer " 前缀
                try {
                    username = jwtUtil.getUsernameFromToken(token);     // 从 JWT 中解析用户名
                } catch (Exception e) {
                    log.warn("JWT解析失败: {}", e.getMessage());
                }
            }

            // Token 有效且未在安全上下文中设置过认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userService.getByUsername(username);
                if (user != null && jwtUtil.validateToken(token, username)) {
                    // 设置 Spring Security 认证信息
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 设置自定义上下文（供 Controller 和切面使用）
                    UserContext.setUser(user);
                    UserContext.setIp(getClientIp(request));
                }
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器异常", e);
        }

        // 继续过滤链，请求处理完成后清理 ThreadLocal
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();                                        // 防止内存泄漏
        }
    }

    /** 获取客户端真实 IP（穿透多层代理） */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();                               // 以上都取不到则用 Servlet 容器的直接 IP
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();                               // 多级代理取第一个
        }
        return ip;
    }
}