package com.example.computerassociation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.computerassociation.dto.CreateAnnouncementDTO;
import com.example.computerassociation.entity.Announcement;

import java.util.List;

/**
 * 公告服务接口
 */
public interface AnnouncementService extends IService<Announcement> {

    /**
     * 获取已发布的公告列表（根据用户权限过滤）
     */
    List<Announcement> getPublishedAnnouncements(Long userId);

    /**
     * 根据ID获取公告详情
     */
    Announcement getAnnouncementById(Long id);

    /**
     * 创建公告
     */
    Announcement createAnnouncement(Long userId, CreateAnnouncementDTO dto);

    /**
     * 更新公告
     */
    Announcement updateAnnouncement(Long id, Long userId, CreateAnnouncementDTO dto);

    /**
     * 删除公告
     */
    void deleteAnnouncement(Long id, Long userId);

    /**
     * 获取我的公告列表
     */
    List<Announcement> getMyAnnouncements(Long userId);
}
