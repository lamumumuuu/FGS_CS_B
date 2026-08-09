// src/main/java/com/example/computerassociation/service/PeakService.java

/**
 * 峰（组织架构）服务接口
 * 提供峰的基本查询和管理功能。
 */

package com.example.computerassociation.service;

import com.example.computerassociation.entity.Peak;

import java.util.List;

public interface PeakService {

    /** 获取所有峰 */
    List<Peak> getAllPeaks();

    /** 按 ID 获取单个峰 */
    Peak getPeakById(Long id);

    /** 按名称获取峰 */
    Peak getPeakByName(String name);

    /** 获取用户所属的峰列表（通过 user_roles 关联） */
    List<Peak> getPeaksByUserId(Long userId);

    /** 添加新峰 */
    Peak addPeak(String name, String description);

    /** 删除峰（需确保该峰下无弟子） */
    boolean deletePeak(Long id);

    /** 检查峰下是否有弟子 */
    boolean hasMembers(Long peakId);

    /** 统计峰下弟子数量 */
    int getMemberCount(Long peakId);

    /**
     * 更新峰信息
     * @param id 峰ID
     * @param name 新名称（可为null）
     * @param description 新描述（可为null）
     * @return 更新后的峰
     */
    Peak updatePeak(Long id, String name, String description);
}