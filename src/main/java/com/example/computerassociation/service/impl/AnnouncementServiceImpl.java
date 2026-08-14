package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.CreateAnnouncementDTO;
import com.example.computerassociation.entity.Announcement;
import com.example.computerassociation.entity.Peak;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.AnnouncementMapper;
import com.example.computerassociation.service.AnnouncementService;
import com.example.computerassociation.service.PermissionService;
import com.example.computerassociation.service.PeakService;
import com.example.computerassociation.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 公告服务实现类
 * 实现公告的CRUD操作及权限控制
 */
@Slf4j
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PeakService peakService;

    @Autowired
    private UserService userService;

    /**
     * 根据发布者ID解析真实用户名（发布者名称统一使用用户名而非"用户"+id）
     */
    private String resolvePublisherName(Long publisherId) {
        if (publisherId == null) {
            return "未知";
        }
        try {
            com.example.computerassociation.entity.User user = userService.getById(publisherId);
            if (user != null && user.getUsername() != null) {
                return user.getUsername();
            }
        } catch (Exception e) {
            log.warn("解析发布者用户名失败: publisherId={}", publisherId, e);
        }
        return "用户" + publisherId;
    }

    @Override
    public List<Announcement> getPublishedAnnouncements(Long userId) {
        List<Announcement> result = new ArrayList<>();

        // 1. 获取全局公告（无峰限制，所有人可见）
        List<Announcement> globalAnnouncements = announcementMapper.selectGlobalPublished();
        result.addAll(globalAnnouncements);

        // 2. 获取用户所在峰的公告
        if (userId != null && !permissionService.isGlobalRoleUser(userId)) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                for (Long peakId : userPeakIds) {
                    List<Announcement> peakAnnouncements = announcementMapper.selectPublishedByPeakId(peakId);
                    result.addAll(peakAnnouncements);
                }
            }
        } else {
            // 全局用户可以查看所有峰的公告
            List<Announcement> allAnnouncements = announcementMapper.selectPublishedAnnouncements();
            // 过滤掉已添加的全局公告，避免重复
            for (Announcement ann : allAnnouncements) {
                if (ann.getPeakId() != null) {
                    result.add(ann);
                }
            }
        }

        // 3. 按创建时间排序
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 4. 回填发布者真实用户名（兼容历史"用户"+id数据）
        for (Announcement ann : result) {
            ann.setPublisherName(resolvePublisherName(ann.getPublisherId()));
        }

        return result;
    }

    @Override
    public Announcement getAnnouncementById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.of("公告不存在");
        }

        // 权限校验：只有发布者或全局用户才能查看非本峰公告
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)) {
            // 如果是全局公告，所有人可见
            if (announcement.getPeakId() != null) {
                // 峰级公告，只有本峰成员和发布者可见
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                boolean hasAccess = false;
                
                // 检查是否在本峰
                if (userPeakIds != null && userPeakIds.contains(announcement.getPeakId())) {
                    hasAccess = true;
                }
                
                // 发布者本人
                if (announcement.getPublisherId() != null && announcement.getPublisherId().equals(userId)) {
                    hasAccess = true;
                }
                
                if (!hasAccess) {
                    throw BusinessException.of(403, "无权查看该公告");
                }
            }
        }

        // 回填发布者真实用户名
        announcement.setPublisherName(resolvePublisherName(announcement.getPublisherId()));

        return announcement;
    }

    @Override
    @Transactional
    public Announcement createAnnouncement(Long userId, CreateAnnouncementDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw BusinessException.of("公告标题不能为空");
        }

        // 权限检查
        boolean isGlobal = "global".equals(dto.getType());
        if (isGlobal) {
            if (!permissionService.hasPermission(userId, "affair:announce_global")) {
                throw BusinessException.of(403, "无权发布全局公告");
            }
        } else {
            // 本峰公告
            if (!permissionService.hasPermission(userId, "affair:announce_peak")) {
                throw BusinessException.of(403, "无权发布本峰公告");
            }
            // 获取用户默认峰
            if (dto.getPeakId() == null) {
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                if (userPeakIds != null && !userPeakIds.isEmpty()) {
                    dto.setPeakId(userPeakIds.get(0));
                }
            }
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setPublisherId(userId);
        announcement.setPublisherName(resolvePublisherName(userId));
        announcement.setPeakId(dto.getPeakId());
        announcement.setType(dto.getType() != null ? dto.getType() : "peak");
        announcement.setStatus("published");
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());

        announcementMapper.insert(announcement);
        
        log.info("创建公告成功: id={}, userId={}, type={}", announcement.getId(), userId, announcement.getType());
        
        return announcement;
    }

    @Override
    @Transactional
    public Announcement updateAnnouncement(Long id, Long userId, CreateAnnouncementDTO dto) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.of("公告不存在");
        }

        // 只有发布者或全局角色可以编辑
        boolean isPublisher = announcement.getPublisherId() != null && announcement.getPublisherId().equals(userId);
        boolean isGlobalRole = permissionService.isGlobalRoleUser(userId);
        
        if (!isPublisher && !isGlobalRole) {
            throw BusinessException.of(403, "无权编辑该公告");
        }

        if (dto.getTitle() != null) {
            announcement.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            announcement.setContent(dto.getContent());
        }

        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        log.info("更新公告成功: id={}, userId={}", id, userId);

        return announcement;
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id, Long userId) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.of("公告不存在");
        }

        // 只有发布者或全局角色可以删除
        boolean isPublisher = announcement.getPublisherId() != null && announcement.getPublisherId().equals(userId);
        boolean isGlobalRole = permissionService.isGlobalRoleUser(userId);
        
        if (!isPublisher && !isGlobalRole) {
            throw BusinessException.of(403, "无权删除该公告");
        }

        announcementMapper.deleteById(id);
        log.info("删除公告成功: id={}, userId={}", id, userId);
    }

    @Override
    public List<Announcement> getMyAnnouncements(Long userId) {
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("publisher_id", userId);
        queryWrapper.orderByDesc("created_at");
        List<Announcement> announcements = announcementMapper.selectList(queryWrapper);
        for (Announcement ann : announcements) {
            ann.setPublisherName(resolvePublisherName(ann.getPublisherId()));
        }
        return announcements;
    }
}
