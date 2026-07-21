package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 使用Lombok注解减少样板代码
 * 使用MyBatis-Plus注解映射数据库表
 */
@Data
@TableName("users") // 映射到数据库中的users表
public class User {
    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String username;      // 用户名
    private String email;         // 邮箱
    private String password;      // 加密后的密码
    private String avatar;        // 头像URL
    private Integer role;         // 角色：0-普通用户，1-干事,2-管理员
    private Integer status;       // 用户状态：0-禁用，1-启用
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;  // 更新时间
    private LocalDateTime lastLoginTime; // 最后登录时间
}
