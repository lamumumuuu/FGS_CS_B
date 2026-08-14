package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宗门活动实体类
 * 对应数据库表 events
 */
@Data
@TableName("events")
public class Event {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动名称 */
    private String name;

    /** 活动描述 */
    private String description;

    /** 活动地点 */
    private String location;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 组织方ID */
    private Long organizerId;

    /** 组织方名称（非数据库字段） */
    private String organizerName;

    /** 关联峰ID（全局活动为null） */
    private Long peakId;

    /** 活动类型：global-全协会, peak-本峰 */
    private String type;

    /** 状态：planned-已规划, ongoing-进行中, completed-已结束, cancelled-已取消 */
    private String status;

    /** 最大参与人数 */
    private Integer maxParticipants;

    /** 已参加人数（非数据库字段，查询时动态填充） */
    @TableField(exist = false)
    private Integer participantCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
