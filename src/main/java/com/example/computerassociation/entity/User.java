// src/main/java/com/example/computerassociation/entity/User.java

/**
 * 用户实体类
 * 映射数据库表 users
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private String username;            /// 用户名，唯一

    private String password;            /// 加密后的密码

    private String avatar;              /// 头像路径

    private Integer role;               /// 角色编号（旧字段，现通过 user_roles 表管理）

    private Integer status;             /// 状态（1 正常 / 0 禁用）

    private LocalDateTime createTime;   /// 创建时间

    private LocalDateTime updateTime;   /// 更新时间

    private LocalDateTime lastLoginTime; /// 最后登录时间
}