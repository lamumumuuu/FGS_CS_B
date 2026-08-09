package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宗门公告实体类
 * 对应数据库表 announcements
 */
@Data
@TableName("announcements")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 发布者ID */
    private Long publisherId;

    /** 发布者名称（非数据库字段） */
    private String publisherName;

    /** 关联峰ID（全局公告为null） */
    private Long peakId;

    /** 公告类型：global-全协会, peak-本峰 */
    private String type;

    /** 状态：published-已发布, draft-草稿 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
