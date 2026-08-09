package com.example.computerassociation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建活动DTO
 */
@Data
public class CreateEventDTO {

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

    /** 关联峰ID */
    private Long peakId;

    /** 活动类型：global-全协会, peak-本峰 */
    private String type;

    /** 最大参与人数 */
    private Integer maxParticipants;
}
