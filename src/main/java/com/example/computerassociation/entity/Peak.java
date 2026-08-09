// src/main/java/com/example/computerassociation/entity/Peak.java

/**
 * 峰实体类
 * 映射数据库表 peaks
 *
 * 扩展字段：
 * - totalLingshi: 峰累计灵石（历史总和，用于展示）
 * - availableLingshi: 峰可支配灵石（可用于分配/调拨的余额）
 *   注：宗门"总可支配灵石"通过特殊配置行（id=0 或 name="_SYSTEM_"）
 *   或在 FinanceController 中独立维护，与峰级灵石分账本管理
 */

package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("peaks")
public class Peak {

    @TableId(type = IdType.AUTO)
    private Long id;                    /// 主键 ID

    private String name;                /// 峰名称，唯一

    private String description;         /// 峰描述

    /**
     * 峰累计灵石（历史累计值，不参与分配计算）
     */
    private Long totalLingshi;

    /**
     * 峰可支配灵石（当前可用余额，参与分配/调拨计算）
     */
    private Long availableLingshi;

    private LocalDateTime createTime;   /// 创建时间

    private LocalDateTime updateTime;   /// 更新时间

    /**
     * 峰下弟子数量（非持久化字段，仅用于 API 响应）
     * 由 SectController.getAllPeaks 动态填充
     */
    @TableField(exist = false)
    private Integer memberCount;
}