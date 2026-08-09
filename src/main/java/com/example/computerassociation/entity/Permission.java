// src/main/java/com/example/computerassociation/entity/Permission.java

/**
 * 权限实体类
 * 映射数据库表 permissions
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permissions")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private String name;                /// 权限标识（如 quest:view_all）

    private String displayName;         /// 显示名称

    private String module;              /// 所属模块

    private String description;         /// 描述

    private LocalDateTime createTime;   /// 创建时间

    private LocalDateTime updateTime;   /// 更新时间
}