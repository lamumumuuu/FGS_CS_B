package com.example.computerassociation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.computerassociation.dto.CreateEventDTO;
import com.example.computerassociation.entity.Event;

import java.util.List;

/**
 * 活动服务接口
 */
public interface EventService extends IService<Event> {

    /**
     * 获取活动列表（根据用户权限过滤）
     */
    List<Event> getActiveEvents(Long userId);

    /**
     * 获取活动详情
     */
    Event getEventById(Long id);

    /**
     * 创建活动
     */
    Event createEvent(Long userId, CreateEventDTO dto);

    /**
     * 更新活动
     */
    Event updateEvent(Long id, Long userId, CreateEventDTO dto);

    /**
     * 删除活动
     */
    void deleteEvent(Long id, Long userId);

    /**
     * 获取我的活动列表
     */
    List<Event> getMyEvents(Long userId);
}
