// src/main/java/com/example/computerassociation/entity/AuditLog.java

/**
 * 审计日志实体类
 * 映射数据库表 audit_logs
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private Long operatorId;            /// 操作人 ID

    private String operatorName;        /// 操作人名称

    private String operation;           /// 操作描述

    private String module;              /// 操作模块

    private String targetType;          /// 操作目标类型

    private Long targetId;              /// 操作目标 ID

    private String beforeData;          /// 操作前数据（JSON）

    private String afterData;           /// 操作后数据（JSON）

    private String ipAddress;           /// 操作 IP

    private String userAgent;           /// 客户端信息

    private Integer status;             /// 操作结果（1 成功 / 0 失败）

    private String errorMessage;        /// 失败时的错误信息

    private LocalDateTime createTime;   /// 操作时间
}