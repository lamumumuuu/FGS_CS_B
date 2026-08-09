// src/main/java/com/example/computerassociation/service/impl/RoleServiceImpl.java

/**
 * 角色管理服务实现类
 * 提供角色查询、分配、移除、更新，以及角色层级校验。
 * 关键方法 updateUserRolesWithCheck 确保操作者只能修改角色层级低于自己的人。
 */

package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.Role;
import com.example.computerassociation.entity.UserRole;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.RoleMapper;
import com.example.computerassociation.mapper.UserRoleMapper;
import com.example.computerassociation.service.AuditLogService;
import com.example.computerassociation.service.RoleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private ObjectMapper objectMapper;                  /// 用于序列化审计数据

    /* ------------------------------------------------------------------ */
    /*  基础查询                                                         */
    /* ------------------------------------------------------------------ */
    @Override
    public List<Role> getAllRoles() {
        return roleMapper.selectList(null);
    }

    @Override
    public Role getRoleById(Long id) {
        if (id == null) return null;
        return roleMapper.selectById(id);
    }

    @Override
    public Role getRoleByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        // 兼容英文标识（name）和中文显示名（display_name）双重查询
        // 解决历史数据中 disciples.role 存储中文角色名导致同步失败的问题
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name).or().eq("display_name", name);
        return roleMapper.selectOne(wrapper);
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        return roleMapper.selectByUserId(userId);
    }

    /* ------------------------------------------------------------------ */
    /*  角色分配与移除                                                    */
    /* ------------------------------------------------------------------ */
    @Override
    @Transactional
    public void assignRoleToUser(Long userId, Long roleId, Long peakId) {
        if (userId == null || roleId == null) {
            throw BusinessException.of("用户ID和角色ID不能为空");
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null) throw BusinessException.of("角色不存在");

        // 需要峰级角色时必须指定峰
        if (isPeakLevelRole(role.getName()) && peakId == null) {
            throw BusinessException.of("峰级角色必须指定峰ID");
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setPeakId(peakId);
        userRole.setCreateTime(LocalDateTime.now());
        userRoleMapper.insert(userRole);

        auditLogService.logSuccess("分配角色", "member", "user_role", userId,
                null, "roleId: " + roleId + ", peakId: " + peakId);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            throw BusinessException.of("用户ID和角色ID不能为空");
        }
        QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("role_id", roleId);
        userRoleMapper.delete(wrapper);

        auditLogService.logSuccess("移除角色", "member", "user_role", userId,
                "roleId: " + roleId, null);
    }

    /* ------------------------------------------------------------------ */
    /*  角色更新（含层级校验）                                            */
    /* ------------------------------------------------------------------ */
    @Override
    @Transactional
    public void updateUserRoles(Long userId, List<Long> roleIds, Long peakId) {
        if (userId == null) throw BusinessException.of("用户ID不能为空");

        // 保存旧角色数据用于审计
        QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserRole> oldRoles = userRoleMapper.selectList(wrapper);
        String beforeData = null;
        try {
            beforeData = objectMapper.writeValueAsString(oldRoles.stream()
                    .map(ur -> "roleId: " + ur.getRoleId())
                    .collect(Collectors.toList()));
        } catch (JsonProcessingException e) {
            log.warn("序列化旧角色数据失败", e);
        }

        // 删除旧角色
        userRoleMapper.delete(wrapper);

        // 插入新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                if (roleId == null) continue;
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setPeakId(peakId);
                userRole.setCreateTime(LocalDateTime.now());
                userRoleMapper.insert(userRole);
            }
        }

        String afterData = "roleIds: " + roleIds + ", peakId: " + peakId;
        auditLogService.logSuccess("更新用户角色", "member", "user", userId, beforeData, afterData);
    }

    /**
     * 带权限层级校验的角色更新
     * 规则：不能修改自己；不能分配高于或等于自己等级的角色；不能修改等级高于或等于自己的用户。
     */
    @Override
    @Transactional
    public void updateUserRolesWithCheck(Long operatorId, Long targetUserId, List<Long> roleIds, Long peakId) {
        if (operatorId == null) throw BusinessException.of(401, "用户未登录");
        if (targetUserId == null) throw BusinessException.of("目标用户ID不能为空");

        if (operatorId.equals(targetUserId)) throw BusinessException.of("不能修改自己的角色");

        if (roleIds == null || roleIds.isEmpty()) throw BusinessException.of("角色列表不能为空");

        // 校验每个目标角色是否存在，以及峰级角色是否指定了峰
        for (Long roleId : roleIds) {
            if (roleId == null) throw BusinessException.of("角色ID不能为null");
            Role role = roleMapper.selectById(roleId);
            if (role == null) throw BusinessException.of("角色不存在: " + roleId);
            if (isPeakLevelRole(role.getName()) && peakId == null) {
                throw BusinessException.of("峰级角色[" + role.getDisplayName() + "]必须指定峰ID");
            }
        }

        // 操作者必须有权限分配这些角色（操作者最高层级必须小于目标角色的最低层级）
        if (!canModifyRoles(operatorId, roleIds)) {
            throw BusinessException.of(403, "权限不足，不能分配高于或等于自己等级的角色");
        }

        // 被修改用户当前最高层级不能 ≤ 操作者最高层级（即不能修改同级或上级）
        int targetCurrentHighestLevel = getHighestRoleLevel(targetUserId);
        int operatorHighestLevel = getHighestRoleLevel(operatorId);
        if (targetCurrentHighestLevel <= operatorHighestLevel) {
            throw BusinessException.of(403, "权限不足，不能修改等级高于或等于自己的用户");
        }

        updateUserRoles(targetUserId, roleIds, peakId);
    }

    /* ------------------------------------------------------------------ */
    /*  角色层级比较辅助方法                                              */
    /* ------------------------------------------------------------------ */

    /** 检查操作者是否有权限分配这些角色（操作者的最高层级 < 每个目标角色的层级） */
    @Override
    public boolean canModifyRoles(Long operatorId, List<Long> targetRoleIds) {
        if (operatorId == null || targetRoleIds == null || targetRoleIds.isEmpty()) return false;

        int operatorLevel = getHighestRoleLevel(operatorId);
        if (operatorLevel == Integer.MAX_VALUE) return false;   /// 没有角色则无权修改

        for (Long roleId : targetRoleIds) {
            if (roleId == null) continue;
            Role role = roleMapper.selectById(roleId);
            if (role == null || role.getLevel() == null) continue;
            if (role.getLevel() <= operatorLevel) return false; /// 目标角色层级必须严格大于操作者最高层级
        }
        return true;
    }

    /** 获取用户最高角色层级（数值最小者权限最高） */
    @Override
    public int getHighestRoleLevel(Long userId) {
        if (userId == null) return Integer.MAX_VALUE;
        List<Role> roles = getRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) return Integer.MAX_VALUE;
        return roles.stream()
                .map(Role::getLevel)
                .filter(level -> level != null)
                .min(Comparator.naturalOrder())
                .orElse(Integer.MAX_VALUE);
    }

    /** 获取用户最低角色层级（数值最大者权限最低） */
    @Override
    public int getLowestRoleLevel(Long userId) {
        if (userId == null) return Integer.MIN_VALUE;
        List<Role> roles = getRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) return Integer.MIN_VALUE;
        return roles.stream()
                .map(Role::getLevel)
                .filter(level -> level != null)
                .max(Comparator.naturalOrder())
                .orElse(Integer.MIN_VALUE);
    }

    /** 判断是否为峰级角色（长老、内门弟子需要绑定峰） */
    private boolean isPeakLevelRole(String roleName) {
        if (roleName == null) return false;
        return "elder".equals(roleName) || "inner_disciple".equals(roleName);
    }
}