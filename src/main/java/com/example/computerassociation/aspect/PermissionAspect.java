// src/main/java/com/example/computerassociation/aspect/PermissionAspect.java

/**
 * 权限校验 AOP 切面
 * 
 * 拦截带有 @RequiresPermission 或 @RequiresRole 注解的方法，
 * 在方法执行前进行权限或角色验证。未登录抛出 401，权限/角色不足抛出 403。
 */

package com.example.computerassociation.aspect;

import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.annotation.RequiresRole;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private PermissionService permissionService;        // 用于查询用户权限和角色

    /* ------------------------------------------------------------------ */
    /*  权限校验：拦截 @RequiresPermission                                */
    /* ------------------------------------------------------------------ */
    @Around("@annotation(com.example.computerassociation.annotation.RequiresPermission) || @within(com.example.computerassociation.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 优先取方法上的注解，若无则取类上的注解
        RequiresPermission methodAnnotation = method.getAnnotation(RequiresPermission.class);
        RequiresPermission classAnnotation = joinPoint.getTarget().getClass().getAnnotation(RequiresPermission.class);
        RequiresPermission effectiveAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        if (effectiveAnnotation != null) {
            String permission = effectiveAnnotation.value();

            // 权限值为空时跳过校验
            if (permission == null || permission.trim().isEmpty()) {
                log.warn("权限注解值为空，跳过校验: method={}", method.getName());
                return joinPoint.proceed();
            }

            Long userId = UserContext.getUserId();             // 从上下文获取当前登录用户 ID
            if (userId == null) {
                throw BusinessException.of(401, "用户未登录");
            }

            boolean hasPermission = permissionService.hasPermission(userId, permission);
            if (!hasPermission) {
                log.warn("权限不足: userId={}, permission={}, method={}", userId, permission, method.getName());
                throw BusinessException.of(403, "权限不足，无法执行此操作");
            }
        }

        return joinPoint.proceed();                            // 校验通过，执行原方法
    }

    /* ------------------------------------------------------------------ */
    /*  角色校验：拦截 @RequiresRole                                      */
    /* ------------------------------------------------------------------ */
    @Around("@annotation(com.example.computerassociation.annotation.RequiresRole) || @within(com.example.computerassociation.annotation.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresRole methodAnnotation = method.getAnnotation(RequiresRole.class);
        RequiresRole classAnnotation = joinPoint.getTarget().getClass().getAnnotation(RequiresRole.class);
        RequiresRole effectiveAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        if (effectiveAnnotation != null) {
            String[] requiredRoles = effectiveAnnotation.value();
            RequiresRole.Logical logical = effectiveAnnotation.logical();  // ANY 或 ALL

            if (requiredRoles == null || requiredRoles.length == 0) {
                log.warn("角色注解值为空，跳过校验: method={}", method.getName());
                return joinPoint.proceed();
            }

            Long userId = UserContext.getUserId();
            if (userId == null) {
                throw BusinessException.of(401, "用户未登录");
            }

            List<String> userRoles = permissionService.getUserRoleNames(userId);
            if (userRoles == null || userRoles.isEmpty()) {
                log.warn("用户无任何角色: userId={}, requiredRoles={}", userId, requiredRoles);
                throw BusinessException.of(403, "角色权限不足，无法执行此操作");
            }

            // 根据 logical 判断：ANY（任一满足）或 ALL（全部满足）
            boolean hasRole;
            if (logical == RequiresRole.Logical.ANY) {
                hasRole = Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
            } else {
                hasRole = userRoles.containsAll(Arrays.asList(requiredRoles));
            }

            if (!hasRole) {
                log.warn("角色不足: userId={}, requiredRoles={}, userRoles={}, logical={}",
                        userId, requiredRoles, userRoles, logical);
                throw BusinessException.of(403, "角色权限不足，无法执行此操作");
            }
        }

        return joinPoint.proceed();
    }
}