package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.CreateEventDTO;
import com.example.computerassociation.entity.Event;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.EventMapper;
import com.example.computerassociation.service.EventService;
import com.example.computerassociation.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 活动服务实现类
 * 实现活动的CRUD操作及权限控制
 */
@Slf4j
@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements EventService {

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private PermissionService permissionService;

    @Override
    public List<Event> getActiveEvents(Long userId) {
        List<Event> result = new ArrayList<>();

        // 1. 获取全局活动
        List<Event> globalEvents = eventMapper.getGlobalEvents();
        result.addAll(globalEvents);

        // 2. 获取用户所在峰的活动
        if (userId != null && !permissionService.isGlobalRoleUser(userId)) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                for (Long peakId : userPeakIds) {
                    List<Event> peakEvents = eventMapper.getEventsByPeakId(peakId);
                    result.addAll(peakEvents);
                }
            }
        } else {
            // 全局用户可以查看所有峰的活动
            QueryWrapper<Event> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("peak_id");
            List<Event> allPeakEvents = eventMapper.selectList(queryWrapper);
            result.addAll(allPeakEvents);
        }

        // 3. 按开始时间排序
        result.sort((a, b) -> {
            if (a.getStartTime() == null && b.getStartTime() == null) return 0;
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return a.getStartTime().compareTo(b.getStartTime());
        });

        return result;
    }

    @Override
    public Event getEventById(Long id) {
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.of("活动不存在");
        }

        // 权限校验
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)) {
            // 如果是全局活动，所有人可见
            if (event.getPeakId() != null) {
                // 峰级活动，只有本峰成员和组织者可见
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                boolean hasAccess = false;
                
                if (userPeakIds != null && userPeakIds.contains(event.getPeakId())) {
                    hasAccess = true;
                }
                
                if (event.getOrganizerId() != null && event.getOrganizerId().equals(userId)) {
                    hasAccess = true;
                }
                
                if (!hasAccess) {
                    throw BusinessException.of(403, "无权查看该活动");
                }
            }
        }

        return event;
    }

    @Override
    @Transactional
    public Event createEvent(Long userId, CreateEventDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw BusinessException.of("活动名称不能为空");
        }

        // 权限检查
        boolean hasPermission = permissionService.hasPermission(userId, "affair:create_event");
        if (!hasPermission) {
            throw BusinessException.of(403, "无权创建活动");
        }

        Event event = new Event();
        event.setName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setOrganizerId(userId);
        event.setOrganizerName("用户" + userId);
        event.setPeakId(dto.getPeakId());
        event.setType(dto.getType() != null ? dto.getType() : "peak");
        event.setStatus("planned");
        event.setMaxParticipants(dto.getMaxParticipants());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        eventMapper.insert(event);
        
        log.info("创建活动成功: id={}, userId={}, name={}", event.getId(), userId, event.getName());
        
        return event;
    }

    @Override
    @Transactional
    public Event updateEvent(Long id, Long userId, CreateEventDTO dto) {
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.of("活动不存在");
        }

        // 权限检查：组织者本人或具备管理权限
        boolean isOrganizer = event.getOrganizerId() != null && event.getOrganizerId().equals(userId);
        boolean hasManagePermission = permissionService.hasPermission(userId, "affair:manage_all_events");
        boolean isGlobalRole = permissionService.isGlobalRoleUser(userId);
        
        if (!isOrganizer && !hasManagePermission && !isGlobalRole) {
            // 检查本峰管理权限
            boolean hasManageOwn = permissionService.hasPermission(userId, "affair:manage_own_peak");
            if (hasManageOwn && event.getPeakId() != null) {
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                if (userPeakIds == null || !userPeakIds.contains(event.getPeakId())) {
                    throw BusinessException.of(403, "无权管理该活动");
                }
            } else {
                throw BusinessException.of(403, "无权管理该活动");
            }
        }

        if (dto.getName() != null) {
            event.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation());
        }
        if (dto.getStartTime() != null) {
            event.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            event.setEndTime(dto.getEndTime());
        }
        if (dto.getMaxParticipants() != null) {
            event.setMaxParticipants(dto.getMaxParticipants());
        }

        event.setUpdatedAt(LocalDateTime.now());
        eventMapper.updateById(event);

        log.info("更新活动成功: id={}, userId={}", id, userId);

        return event;
    }

    @Override
    @Transactional
    public void deleteEvent(Long id, Long userId) {
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.of("活动不存在");
        }

        // 权限检查
        boolean isOrganizer = event.getOrganizerId() != null && event.getOrganizerId().equals(userId);
        boolean hasManagePermission = permissionService.hasPermission(userId, "affair:manage_all_events");
        boolean isGlobalRole = permissionService.isGlobalRoleUser(userId);
        
        if (!isOrganizer && !hasManagePermission && !isGlobalRole) {
            boolean hasManageOwn = permissionService.hasPermission(userId, "affair:manage_own_peak");
            if (hasManageOwn && event.getPeakId() != null) {
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                if (userPeakIds == null || !userPeakIds.contains(event.getPeakId())) {
                    throw BusinessException.of(403, "无权删除该活动");
                }
            } else {
                throw BusinessException.of(403, "无权删除该活动");
            }
        }

        eventMapper.deleteById(id);
        log.info("删除活动成功: id={}, userId={}", id, userId);
    }

    @Override
    public List<Event> getMyEvents(Long userId) {
        QueryWrapper<Event> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organizer_id", userId);
        queryWrapper.orderByDesc("created_at");
        return eventMapper.selectList(queryWrapper);
    }
}
