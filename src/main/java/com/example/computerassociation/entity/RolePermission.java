// src/main/java/com/example/computerassociation/entity/RolePermission.java

/**
 * 角色权限关联实体类
 * 映射数据库表 role_permissions（多对多中间表）
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role_permissions")
public class RolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private Long roleId;                /// 角色 ID

    private Long permissionId;          /// 权限 ID

    private LocalDateTime createTime;   /// 创建时间
}