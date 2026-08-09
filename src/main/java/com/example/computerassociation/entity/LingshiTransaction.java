package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 灵石流水实体类
 * 对应数据库表 lingshi_transactions
 *
 * 扩展字段：
 * - peakId/peakName: 峰关联信息，用于峰级财务分类统计
 */
@Data
@TableName("lingshi_transactions")
public class LingshiTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 弟子ID */
    private Long discipleId;

    /** 弟子姓名 */
    private String discipleName;

    /** 变更类型：reward-打赏, task_reward-任务奖励, adjust_in-灵石增加, adjust_out-灵石扣除,
     *           allocate_in-灵石分配（总→峰）, allocate_out-峰间调拨, peak_transfer-峰间调拨 */
    private String type;

    /** 变更金额（正数增加，负数减少） */
    private Long amount;

    /** 变更后余额 */
    private Long balance;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 备注 */
    private String remark;

    /** 关联峰ID（峰级分类统计用，可为空） */
    private Long peakId;

    /** 关联峰名称（冗余存储，便于查询展示） */
    private String peakName;

    private LocalDateTime createdAt;
}
