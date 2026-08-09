package com.example.computerassociation.dto;

import lombok.Data;

/**
 * 创建公告DTO
 */
@Data
public class CreateAnnouncementDTO {

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型：global-全协会, peak-本峰 */
    private String type;

    /** 关联峰ID（本峰公告需要） */
    private Long peakId;
}
