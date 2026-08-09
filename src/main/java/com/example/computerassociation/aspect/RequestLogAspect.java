// src/main/java/com/example/computerassociation/aspect/RequestLogAspect.java

/**
 * 请求日志 AOP 切面
 * 
 * 拦截所有 Controller 层方法，记录请求的方法、URI、客户端 IP 及执行耗时。
 * 超阈值（1秒）请求以 WARN 级别输出慢请求日志，异常时记录 ERROR 日志。
 * 每次请求分配一个 traceId，便于追踪整个请求链路。
 */

package com.example.computerassociation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    private static final long SLOW_REQUEST_THRESHOLD = 1000;  /// 慢请求阈值（毫秒）

    /** 定义切点：所有 controller 包下的公共方法 */
    @Pointcut("execution(* com.example.computerassociation.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /** 环绕通知：记录请求与响应日志 */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().substring(0, 8);   /// 生成简短追踪 ID

        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String ip = getClientIp(request);

        log.info("[{}] >>> {} {} from {}", traceId, method, uri, ip);    /// 请求进入日志

        try {
            Object result = point.proceed();                              /// 执行目标方法
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed > SLOW_REQUEST_THRESHOLD) {
                log.warn("[{}] <<< {} {} | {}ms [SLOW]", traceId, method, uri, elapsed);
            } else {
                log.info("[{}] <<< {} {} | {}ms", traceId, method, uri, elapsed);
            }

            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[{}] <<< {} {} | {}ms | ERROR: {}", traceId, method, uri, elapsed, e.getMessage());
            throw e;                                                      /// 重新抛出异常，由全局异常处理器处理
        }
    }

    /** 从 Spring 上下文中获取当前 HTTP 请求对象 */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /** 获取客户端真实 IP（穿透代理） */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();                                 /// 多级代理取第一个 IP
        }
        return ip;
    }
}