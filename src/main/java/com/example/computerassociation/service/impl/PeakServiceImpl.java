// src/main/java/com/example/computerassociation/service/impl/PeakServiceImpl.java

/**
 * 峰服务实现类
 * 提供峰的基本查询功能：全部、按ID、按名称、按用户ID。
 */

package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.Peak;
import com.example.computerassociation.mapper.PeakMapper;
import com.example.computerassociation.service.PeakService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PeakServiceImpl implements PeakService {

    @Autowired
    private PeakMapper peakMapper;

    @Autowired
    private com.example.computerassociation.service.PermissionService permissionService;

    @Override
    public List<Peak> getAllPeaks() {
        // 获取当前用户并进行峰过滤
        // 全局访问条件：isGlobalRoleUser 返回 true，或拥有 member:view_all 权限
        // （双保险：即使角色判断因数据异常失效，权限检查仍能确保大长老等全局角色可查看所有峰）
        Long userId = UserContext.getUserId();
        boolean hasGlobalAccess = userId != null
                && (permissionService.isGlobalRoleUser(userId)
                    || permissionService.hasPermission(userId, "member:view_all"));
        if (userId != null && !hasGlobalAccess) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            QueryWrapper<Peak> queryWrapper = new QueryWrapper<>();
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                queryWrapper.in("id", userPeakIds);
                // 排除 _TREASURY_ 特殊峰（财务模块的持久化容器，不在宗门事务中显示）
                queryWrapper.ne("name", "_TREASURY_");
                return peakMapper.selectList(queryWrapper);
            } else {
                // 非全局用户且无峰关联，则不返回任何峰
                return java.util.Collections.emptyList();
            }
        }
        // 全局用户：排除 _TREASURY_ 特殊峰（财务模块的持久化容器）
        QueryWrapper<Peak> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("name", "_TREASURY_");
        return peakMapper.selectList(queryWrapper);
    }

    @Override
    public Peak getPeakById(Long id) {
        return peakMapper.selectById(id);
    }

    @Override
    public Peak getPeakByName(String name) {
        QueryWrapper<Peak> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        return peakMapper.selectOne(wrapper);
    }

    @Override
    public List<Peak> getPeaksByUserId(Long userId) {
        return peakMapper.selectByUserId(userId);
    }

    @Override
    public Peak addPeak(String name, String description) {
        Peak peak = new Peak();
        peak.setName(name);
        peak.setDescription(description);
        peakMapper.insert(peak);
        return peak;
    }

    @Override
    public boolean deletePeak(Long id) {
        Peak peak = peakMapper.selectById(id);
        if (peak == null) {
            return false;
        }
        
        if (hasMembers(id)) {
            return false;
        }
        
        peakMapper.deleteById(id);
        return true;
    }

    @Override
    public boolean hasMembers(Long peakId) {
        return peakMapper.countMembersByPeakId(peakId) > 0;
    }

    /**
     * 统计峰下弟子数量
     * 通过 peakMapper.countMembersByPeakId 查询（按峰名关联 disciples.peak 字段）
     */
    @Override
    public int getMemberCount(Long peakId) {
        Integer count = peakMapper.countMembersByPeakId(peakId);
        return count != null ? count : 0;
    }

    /**
     * 更新峰信息
     * 权限检查：
     * 1. 用户需拥有 peak:edit_any（修改任意峰信息）或 peak:edit_own（修改本峰信息）权限
     * 2. 若只有 peak:edit_own 权限，只能修改自己所属峰的信息
     */
    @Override
    public Peak updatePeak(Long id, String name, String description) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 检查 peak:edit_any 权限（可修改任意峰）
        if (permissionService.hasPermission(userId, "peak:edit_any")) {
            return doUpdatePeak(id, name, description);
        }

        // 检查 peak:edit_own 权限（只能修改本峰）
        if (permissionService.hasPermission(userId, "peak:edit_own")) {
            // 获取当前用户所属的峰
            List<Peak> userPeaks = getPeaksByUserId(userId);
            boolean isOwnPeak = userPeaks.stream().anyMatch(p -> p.getId().equals(id));
            if (!isOwnPeak) {
                throw new RuntimeException("您没有修改其他峰信息的权限");
            }
            return doUpdatePeak(id, name, description);
        }

        throw new RuntimeException("您没有修改峰信息的权限");
    }

    /**
     * 执行峰信息更新
     */
    private Peak doUpdatePeak(Long id, String name, String description) {
        Peak peak = peakMapper.selectById(id);
        if (peak == null) {
            throw new RuntimeException("峰不存在");
        }

        if (name != null) {
            peak.setName(name);
        }
        if (description != null) {
            peak.setDescription(description);
        }
        peakMapper.updateById(peak);
        return peak;
    }
}