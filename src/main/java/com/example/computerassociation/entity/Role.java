// src/main/java/com/example/computerassociation/entity/Role.java

/**
 * 角色实体类
 * 映射数据库表 roles
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("roles")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private String name;                /// 角色标识（如 sect_master）

    private String displayName;         /// 显示名称（如 宗主）

    private String description;         /// 描述

    private Integer level;              /// 角色层级（0 最高）

    private Integer isSystem;           /// 是否系统内置（1 是）

    private LocalDateTime createTime;   /// 创建时间

    private LocalDateTime updateTime;   /// 更新时间
}