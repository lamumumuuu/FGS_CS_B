package com.example.computerassociation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tasks")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private String status;

    private Integer reward;

    private LocalDateTime deadline;

    private Long publisherId;

    private Long completerId;

    private String techRequirements;

    private Long peakId;

    private String rejectReason;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    /** 提交成果描述（勇者提交悬赏时填写并持久化） */
    private String submissionDescription;

    /** 提交成果附件链接（勇者提交悬赏时填写并持久化） */
    private String attachmentUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String publisherName;

    @TableField(exist = false)
    private String publisherAvatar;

    @TableField(exist = false)
    private String completerName;

    @TableField(exist = false)
    private String completerAvatar;
}
