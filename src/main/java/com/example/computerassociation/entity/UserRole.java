// src/main/java/com/example/computerassociation/entity/UserRole.java

/**
 * 用户角色关联实体类
 * 映射数据库表 user_roles（多对多中间表）
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_roles")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private Long userId;                /// 用户 ID

    private Long roleId;                /// 角色 ID

    private Long peakId;                /// 所属峰 ID（NULL 表示全局角色）

    private LocalDateTime createTime;   /// 创建时间
}