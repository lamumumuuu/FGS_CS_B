// src/main/java/com/example/computerassociation/service/impl/AuditLogServiceImpl.java

/**
 * 审计日志服务实现类
 * 从 UserContext 自动获取操作人信息，从 RequestContext 获取 User-Agent，写入 audit_logs 表。
 * 提供成功/失败日志的快捷记录方法。
 */

package com.example.computerassociation.service.impl;

import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.AuditLog;
import com.example.computerassociation.mapper.AuditLogMapper;
import com.example.computerassociation.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogMapper auditLogMapper;              /// 审计日志 Mapper

    /* ------------------------------------------------------------------ */
    /*  通用日志记录：自动填充操作人、IP、User-Agent                      */
    /* ------------------------------------------------------------------ */
    @Override
    public void log(String operation, String module, String targetType, Long targetId,
                    String beforeData, String afterData, Integer status, String errorMessage) {
        try {
            // 从当前请求上下文获取操作人
            Long operatorId = UserContext.getUserId();
            String operatorName = UserContext.getUsername();
            String ipAddress = UserContext.getIp();
            String userAgent = null;

            // 尝试获取 User-Agent
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    userAgent = request.getHeader("User-Agent");
                }
            } catch (Exception e) {
                log.debug("获取User-Agent失败", e);
            }

            doInsertLog(operatorId, operatorName, operation, module, targetType, targetId,
                    beforeData, afterData, status, errorMessage, ipAddress, userAgent);
        } catch (Exception e) {
            log.error("审计日志记录失败", e);          // 审计失败不影响主流程
        }
    }

    /* ------------------------------------------------------------------ */
    /*  快捷方法                                                         */
    /* ------------------------------------------------------------------ */
    @Override
    public void logSuccess(String operation, String module, String targetType, Long targetId,
                           String beforeData, String afterData) {
        log(operation, module, targetType, targetId, beforeData, afterData, 1, null);
    }

    @Override
    public void logFailure(String operation, String module, String targetType, Long targetId,
                           String errorMessage) {
        log(operation, module, targetType, targetId, null, null, 0, errorMessage);
    }

    @Override
    public void logSuccessWithOperator(Long operatorId, String operatorName, String operation,
                                       String module, String targetType, Long targetId,
                                       String beforeData, String afterData, String ipAddress) {
        try {
            doInsertLog(operatorId, operatorName, operation, module, targetType, targetId,
                    beforeData, afterData, 1, null, ipAddress, null);
        } catch (Exception e) {
            log.error("审计日志记录失败", e);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  实际插入数据库                                                    */
    /* ------------------------------------------------------------------ */
    private void doInsertLog(Long operatorId, String operatorName, String operation,
                             String module, String targetType, Long targetId,
                             String beforeData, String afterData, Integer status,
                             String errorMessage, String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(operatorId);
        auditLog.setOperatorName(operatorName);
        auditLog.setOperation(operation);
        auditLog.setModule(module);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setStatus(status);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setCreateTime(LocalDateTime.now());

        auditLogMapper.insert(auditLog);
    }
}