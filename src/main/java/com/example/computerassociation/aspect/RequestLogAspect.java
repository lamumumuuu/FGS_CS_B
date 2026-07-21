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

    private static final long SLOW_REQUEST_THRESHOLD = 1000;

    @Pointcut("execution(* com.example.computerassociation.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String ip = getClientIp(request);

        log.info("[{}] >>> {} {} from {}", traceId, method, uri, ip);

        try {
            Object result = point.proceed();
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
            throw e;
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

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
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
