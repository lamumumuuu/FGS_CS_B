// src/main/java/com/example/computerassociation/mapper/PeakMapper.java

/**
 * 峰 Mapper 接口
 * 包含根据用户 ID 查询所属峰及峰 ID 列表的自定义 SQL。
 */

package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.Peak;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PeakMapper extends BaseMapper<Peak> {

    /** 查询用户所属的峰（通过 user_roles 关联） */
    @Select("SELECT p.* FROM peaks p " +
            "INNER JOIN user_roles ur ON p.id = ur.peak_id " +
            "WHERE ur.user_id = #{userId} AND ur.peak_id IS NOT NULL")
    List<Peak> selectByUserId(@Param("userId") Long userId);

    /** 查询用户所属的峰 ID 列表 */
    @Select("SELECT ur.peak_id FROM user_roles ur " +
            "WHERE ur.user_id = #{userId} AND ur.peak_id IS NOT NULL")
    List<Long> selectPeakIdsByUserId(@Param("userId") Long userId);

    /** 统计峰下弟子数量 */
    @Select("SELECT COUNT(*) FROM disciples d WHERE d.peak = (SELECT name FROM peaks WHERE id = #{peakId})")
    Integer countMembersByPeakId(@Param("peakId") Long peakId);
}