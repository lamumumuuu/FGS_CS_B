package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动数据访问层
 */
@Mapper
public interface EventMapper extends BaseMapper<Event> {

    /**
     * 获取所有进行中和已规划的活动
     */
    @Select("SELECT * FROM events WHERE status IN ('planned', 'ongoing') ORDER BY start_time ASC")
    List<Event> getActiveEvents();

    /**
     * 获取指定峰的活动
     */
    @Select("SELECT * FROM events WHERE peak_id = #{peakId} ORDER BY created_at DESC")
    List<Event> getEventsByPeakId(@Param("peakId") Long peakId);

    /**
     * 获取全局活动（无峰限制）
     */
    @Select("SELECT * FROM events WHERE peak_id IS NULL ORDER BY created_at DESC")
    List<Event> getGlobalEvents();

    /**
     * 统计活动已参加人数
     */
    @Select("SELECT COUNT(*) FROM event_participants WHERE event_id = #{eventId}")
    int countParticipants(@Param("eventId") Long eventId);

    /**
     * 统计用户是否已加入指定活动
     */
    @Select("SELECT COUNT(*) FROM event_participants WHERE event_id = #{eventId} AND user_id = #{userId}")
    int countUserParticipation(@Param("eventId") Long eventId, @Param("userId") Long userId);

    /**
     * 用户加入活动（插入参与记录）
     */
    @Insert("INSERT INTO event_participants (event_id, user_id, joined_at) VALUES (#{eventId}, #{userId}, NOW())")
    int insertParticipant(@Param("eventId") Long eventId, @Param("userId") Long userId);
}
