// src/main/java/com/example/computerassociation/service/impl/PermissionServiceImpl.java

/**
 * 权限查询服务实现类
 * 提供权限、角色、峰 ID 的查询，以及基于内存列表的 hasPermission / hasRole 判断。
 * 所有查询均对 null 参数做安全处理，返回空集合或 false。
 */

package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.entity.Permission;
import com.example.computerassociation.entity.Role;
import com.example.computerassociation.entity.RolePermission;
import com.example.computerassociation.entity.UserRole;
import com.example.computerassociation.mapper.PermissionMapper;
import com.example.computerassociation.mapper.PeakMapper;
import com.example.computerassociation.mapper.RoleMapper;
import com.example.computerassociation.mapper.RolePermissionMapper;
import com.example.computerassociation.mapper.UserRoleMapper;
import com.example.computerassociation.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PeakMapper peakMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            List<Permission> result = permissionMapper.selectByUserId(userId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getPermissionNamesByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            List<String> result = permissionMapper.selectPermissionNamesByUserId(userId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取用户权限名称失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            List<Role> result = roleMapper.selectByUserId(userId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUserRoleNames(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            List<String> result = roleMapper.selectRoleNamesByUserId(userId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取用户角色名称失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasPermission(Long userId, String permissionName) {
        if (userId == null || permissionName == null || permissionName.trim().isEmpty()) return false;
        List<String> permissions = getPermissionNamesByUserId(userId);
        return permissions.contains(permissionName);    /// 直接判断列表中是否包含
    }

    @Override
    public boolean hasRole(Long userId, String roleName) {
        if (userId == null || roleName == null || roleName.trim().isEmpty()) return false;
        List<String> roles = getUserRoleNames(userId);
        return roles.contains(roleName);
    }

    @Override
    public boolean hasAnyRole(Long userId, String... roleNames) {
        if (userId == null || roleNames == null || roleNames.length == 0) return false;
        List<String> userRoles = getUserRoleNames(userId);
        return Arrays.stream(roleNames).anyMatch(userRoles::contains);
    }

    @Override
    public List<Long> getUserPeakIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            List<Long> result = peakMapper.selectPeakIdsByUserId(userId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取用户峰ID失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isGlobalRoleUser(Long userId) {
        if (userId == null) return false;
        try {
            // 方式1：检查 user_roles 中是否存在 peak_id 为 NULL 的记录（原始逻辑）
            QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).isNull("peak_id");
            Long count = userRoleMapper.selectCount(wrapper);
            if (count != null && count > 0) {
                return true;
            }

            // 方式2：检查用户是否拥有全局角色（无论 peak_id 是否为 NULL）
            // 全局角色定义：sect_master（宗主）、grand_elder（大长老）、supreme_elder（太上长老）、honor_elder（荣誉长老）
            // 这些角色的权限范围是全局的，即使被分配到了具体峰，也应保留全局查看能力
            List<String> globalRoles = Arrays.asList("sect_master", "grand_elder", "supreme_elder", "honor_elder");
            List<String> userRoleNames = getUserRoleNames(userId);
            return userRoleNames.stream().anyMatch(globalRoles::contains);
        } catch (Exception e) {
            log.error("判断全局角色用户失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public List<Permission> getAllPermissions() {
        try {
            return permissionMapper.selectList(null);
        } catch (Exception e) {
            log.error("获取所有权限失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        if (roleId == null) return Collections.emptyList();
        try {
            return permissionMapper.selectByRoleId(roleId);
        } catch (Exception e) {
            log.error("获取角色权限失败: roleId={}", roleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getPermissionNamesByRoleId(Long roleId) {
        if (roleId == null) return Collections.emptyList();
        try {
            return permissionMapper.selectPermissionNamesByRoleId(roleId);
        } catch (Exception e) {
            log.error("获取角色权限名称失败: roleId={}", roleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null) return;

        QueryWrapper<RolePermission> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        rolePermissionMapper.delete(wrapper);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            permissionIds.forEach(pid -> {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    @Override
    public boolean canModifyRolePermissions(Long operatorId) {
        if (operatorId == null) return false;
        int level = getHighestRoleLevel(operatorId);
        return level <= 1;
    }

    private int getHighestRoleLevel(Long userId) {
        if (userId == null) return Integer.MAX_VALUE;
        try {
            QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            List<UserRole> userRoles = userRoleMapper.selectList(wrapper);

            int minLevel = Integer.MAX_VALUE;
            for (UserRole ur : userRoles) {
                Role role = roleMapper.selectById(ur.getRoleId());
                if (role != null && role.getLevel() != null && role.getLevel() < minLevel) {
                    minLevel = role.getLevel();
                }
            }
            return minLevel == Integer.MAX_VALUE ? 99 : minLevel;
        } catch (Exception e) {
            log.error("获取用户角色层级失败: userId={}", userId, e);
            return Integer.MAX_VALUE;
        }
    }
}