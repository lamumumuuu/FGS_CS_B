package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告数据访问层
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 获取所有已发布的公告
     */
    @Select("SELECT * FROM announcements WHERE status = 'published' ORDER BY created_at DESC")
    List<Announcement> selectPublishedAnnouncements();

    /**
     * 获取指定峰的已发布公告
     */
    @Select("SELECT * FROM announcements WHERE status = 'published' AND peak_id = #{peakId} ORDER BY created_at DESC")
    List<Announcement> selectPublishedByPeakId(@Param("peakId") Long peakId);

    /**
     * 获取全局公告（无峰限制）
     */
    @Select("SELECT * FROM announcements WHERE status = 'published' AND peak_id IS NULL ORDER BY created_at DESC")
    List<Announcement> selectGlobalPublished();
}
