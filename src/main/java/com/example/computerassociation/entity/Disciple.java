package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 弟子实体类，对应数据库表 disciples
 */
@Data
@TableName("disciples")
public class Disciple {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 users.id */
    private Long userId;

    /** 弟子姓名（默认取 username） */
    private String name;

    /** 学号（注册时未收集，默认空） */
    private String studentId;

    /** 宗门角色：outer_disciple / inner_disciple / elder ... */
    private String role;

    /** 所属峰：项目峰 / 算法峰 / 电路峰 / 管理台 / 无 */
    private String peak;

    /** 灵石数量 */
    private Long lingshi;

    /** 加入时间 */
    private LocalDateTime joinedAt;

    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}