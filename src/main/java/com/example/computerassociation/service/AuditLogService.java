// src/main/java/com/example/computerassociation/service/AuditLogService.java

/**
 * 审计日志服务接口
 * 提供多种重载方法，方便在不同场景下记录操作审计信息。
 */

package com.example.computerassociation.service;

import com.example.computerassociation.entity.AuditLog;

public interface AuditLogService {

    /** 记录完整审计日志 */
    void log(String operation, String module, String targetType, Long targetId,
             String beforeData, String afterData, Integer status, String errorMessage);

    /** 记录操作成功的审计日志（状态=1） */
    void logSuccess(String operation, String module, String targetType, Long targetId,
                    String beforeData, String afterData);

    /** 记录操作失败的审计日志（状态=0，含错误信息） */
    void logFailure(String operation, String module, String targetType, Long targetId,
                    String errorMessage);

    /** 记录带操作人详细信息的成功审计日志 */
    void logSuccessWithOperator(Long operatorId, String operatorName, String operation,
                                String module, String targetType, Long targetId,
                                String beforeData, String afterData, String ipAddress);
}